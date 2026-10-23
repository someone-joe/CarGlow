package com.ruoyi.wash.member.service;

import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.member.dto.AgreementVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 协议富文本。契约：openapi.yaml GET /api/v1/auth/agreements?type=SERVICE|PRIVACY|RECHARGE|INSURANCE。
 *
 * <p>取值优先级（与 WxConfigController 同范式）：后台「系统管理 → 参数设置」（key 见 KEY_PREFIX）
 * → 代码默认标题。协议全文属合规文案，代码不内置假文本：未配置时 content 为空串，
 * 运营填充后即时生效（sys_config 自带缓存）。配置 key 的种子数据见
 * sql/wash_20261024_agreement_config.sql（dev 占位内容，上线前必须替换为法务审核文本）。
 */
@Service
public class WashAgreementService {

    /** type（契约枚举，大写）→ {配置 key 中缀, 默认标题} */
    private static final Map<String, String[]> TYPES = new LinkedHashMap<>();

    static {
        TYPES.put("SERVICE", new String[]{"service", "服务条款与钥匙寄存协议"});
        TYPES.put("PRIVACY", new String[]{"privacy", "隐私政策"});
        TYPES.put("RECHARGE", new String[]{"recharge", "充值协议"});
        TYPES.put("INSURANCE", new String[]{"insurance", "保险与理赔说明"});
    }

    private static final String KEY_PREFIX = "wash.agreement.";

    @Autowired
    private ISysConfigService sysConfigService;

    /** 按契约枚举取协议；type 不合法按参数错误拒绝（A0001） */
    public AgreementVO get(String type) {
        String[] conf = TYPES.get(type == null ? "" : type.trim().toUpperCase(Locale.ROOT));
        if (conf == null) {
            throw new ApiException(ErrorCode.A0001, "协议类型不支持: " + type + "，可选 SERVICE/PRIVACY/RECHARGE/INSURANCE");
        }
        String title = resolve(KEY_PREFIX + conf[0] + ".title");
        if (title == null || title.isBlank()) {
            title = conf[1];
        }
        String content = resolve(KEY_PREFIX + conf[0] + ".content");
        return new AgreementVO(title, content == null ? "" : content);
    }

    /** 后台参数优先；未配置返回 null（标题有代码默认兜底，正文允许为空待运营填充） */
    private String resolve(String key) {
        return sysConfigService.selectConfigByKey(key);
    }
}
