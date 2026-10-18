package com.ruoyi.wash.member.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 微信手机号授权：把前端 `<button open-type="getPhoneNumber">` 拿到的 code 换成手机号。
 *
 * <p>与 {@link WashAuthService#resolveOpenid} 同一套降级策略：未配置 wash.wx.secret（本地开发）时
 * 直接返回 null，由调用方跳过绑定——**绝不返回假手机号**，否则开发环境的脏数据会流进生产库。
 *
 * <p>真实接入需要：小程序已认证主体 + 已开通「获取手机号」能力，其余代码零改动。
 * 手机号属敏感信息，全程不打日志（只记 errcode / errmsg）。
 */
@Service
public class WxPhoneService {

    private static final Logger log = LoggerFactory.getLogger(WxPhoneService.class);

    private static final String TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
    private static final String PHONE_URL =
            "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=%s";

    @Value("${wash.wx.appid:}")
    private String appid;

    @Value("${wash.wx.secret:}")
    private String secret;

    /**
     * 用授权 code 换手机号。
     *
     * @return 手机号；用户未授权、本地开发模式或微信侧失败时返回 null（调用方据此跳过绑定，不报错）
     */
    public String getPhone(String phoneCode) {
        if (phoneCode == null || phoneCode.isBlank()) {
            return null;
        }
        if (secret == null || secret.isBlank()) {
            log.warn("[手机号授权] 未配置 wash.wx.secret，跳过手机号绑定（仅限本地开发）");
            return null;
        }
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                return null;
            }
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(String.format(PHONE_URL, accessToken)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"code\":\"" + phoneCode + "\"}"))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject jo = JSON.parseObject(response.body());
            if (jo.getIntValue("errcode") != 0) {
                // 只记错误码与微信的说明，绝不打手机号
                log.error("[手机号授权] 微信返回失败 errcode={} errmsg={}",
                        jo.getIntValue("errcode"), jo.getString("errmsg"));
                return null;
            }
            JSONObject phoneInfo = jo.getJSONObject("phone_info");
            if (phoneInfo == null) {
                return null;
            }
            String phone = phoneInfo.getString("phone_number");
            if (phone == null || phone.isBlank()) {
                return null;
            }
            // 脱敏后才允许进日志（保留前 3 后 4，够排查又不泄露）
            log.info("[手机号授权] 换号成功 phone={}****{}", phone.substring(0, 3), phone.substring(phone.length() - 4));
            return phone;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[手机号授权] 调用被中断");
            return null;
        } catch (Exception e) {
            log.error("[手机号授权] 调用异常", e);
            return null;
        }
    }

    /** 取小程序全局 access_token（换取手机号的前置步骤） */
    private String getAccessToken() {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create(String.format(TOKEN_URL, appid, secret)))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject jo = JSON.parseObject(response.body());
            String token = jo.getString("access_token");
            if (token == null || token.isBlank()) {
                log.error("[手机号授权] 获取 access_token 失败 errcode={} errmsg={}",
                        jo.getIntValue("errcode"), jo.getString("errmsg"));
                return null;
            }
            return token;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[手机号授权] access_token 调用被中断");
            return null;
        } catch (Exception e) {
            log.error("[手机号授权] access_token 调用异常", e);
            return null;
        }
    }
}
