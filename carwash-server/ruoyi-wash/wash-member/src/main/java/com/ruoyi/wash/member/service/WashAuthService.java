package com.ruoyi.wash.member.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.member.domain.WashMember;
import com.ruoyi.wash.member.dto.LoginVO;
import com.ruoyi.wash.member.mapper.WashMemberMapper;
import com.ruoyi.wash.member.security.MemberTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 微信登录：code2session 换 openid → 查/建会员 → 签发 token。
 *
 * <p>本地开发未配置 wash.wx.secret 时走模拟登录（固定 dev-openid），
 * 保证调试期间始终是同一个用户；配置 secret 后自动切到真实微信调用，代码零改动。
 */
@Service
public class WashAuthService {

    private static final Logger log = LoggerFactory.getLogger(WashAuthService.class);
    private static final String JSCODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    @Autowired
    private WashMemberMapper memberMapper;

    @Autowired
    private MemberTokenService tokenService;

    @Autowired
    private WxPhoneService phoneService;

    @Value("${wash.wx.appid:}")
    private String appid;

    @Value("${wash.wx.secret:}")
    private String secret;

    public LoginVO login(String code, String phoneCode) {
        String openid = resolveOpenid(code);

        WashMember member = memberMapper.selectByOpenid(openid);
        boolean isNewUser = false;
        if (member == null) {
            member = new WashMember();
            member.setOpenid(openid);
            memberMapper.insertWashMember(member);
            isNewUser = true;
        }

        // 手机号授权：用户点了「允许获取手机号」才会带 phoneCode 上来。
        // 只在手机号为空时绑定——不授权就保持原样，绝不把已有手机号清掉。
        bindPhoneIfNeeded(member, phoneCode);

        LoginVO vo = new LoginVO();
        vo.setToken(tokenService.createToken(member.getMemberId()));
        vo.setExpireAt(tokenService.getExpireAt());
        vo.setIsNewUser(isNewUser);
        // 新用户返回 true，由前端弹《用户服务协议》《隐私政策》（PRD 6.2.1）
        vo.setNeedAgreement(isNewUser);
        vo.setPhoneBound(member.getPhone() != null);
        return vo;
    }

    /**
     * 需要时绑定手机号。
     *
     * <p>三个"不"：用户没授权（phoneCode 空）不绑；已有手机号不覆盖（换号需走客服，避免误操作丢联系方式）；
     * 微信侧换号失败也不报错——手机号只是联系方式，不该阻断登录，前端可再次引导授权。
     */
    private void bindPhoneIfNeeded(WashMember member, String phoneCode) {
        if (member.getPhone() != null && !member.getPhone().isBlank()) {
            return;
        }
        String phone = phoneService.getPhone(phoneCode);
        if (phone == null) {
            return;
        }
        // SQL 自带「手机号为空才更新」的兜底：并发登录同时授权时，只有一个能写成功
        memberMapper.bindPhone(member.getMemberId(), phone);
        member.setPhone(phone);
    }

    private String resolveOpenid(String code) {        // 模拟登录：secret 为空 = 本地开发模式
        if (secret == null || secret.isBlank()) {
            log.warn("[微信登录] 未配置 wash.wx.secret，使用模拟 openid（仅限本地开发）");
            return "dev-openid";
        }

        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create(String.format(JSCODE2SESSION_URL, appid, secret, code)))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject jo = JSON.parseObject(response.body());
            if (jo.getIntValue("errcode") != 0 || jo.getString("openid") == null) {
                throw new ApiException(ErrorCode.C0001, "微信登录失败：" + jo.getString("errmsg"));
            }
            return jo.getString("openid");
        } catch (ApiException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ErrorCode.C0001, "微信登录调用被中断");
        } catch (Exception e) {
            log.error("[微信登录] code2session 调用异常", e);
            throw new ApiException(ErrorCode.C0001, "微信登录调用失败");
        }
    }
}
