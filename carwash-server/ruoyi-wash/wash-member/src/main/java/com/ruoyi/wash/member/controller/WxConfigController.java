package com.ruoyi.wash.member.controller;

import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.member.dto.CustomerServiceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端通用配置。契约：openapi.yaml GET /api/v1/config/customer-service。
 *
 * <p>本类属 misc 域，项目暂无 misc 模块，暂挂 wash-member（同为 C 端接口层）；
 * 将来 misc 模块开工时整体挪走，接口路径不变，前端无感。
 *
 * <p>取值优先级：后台「系统管理 → 参数设置」（key 见 CONFIG_KEY_*，改完即时生效，sys_config 自带缓存）
 * → application.yml 兜底 → 代码默认。platformPhone 默认 10086：
 * 客服入口任何时候都必须能拨通，不能因为没配置就只剩一句文案（bug092203）。
 */
@RestController
public class WxConfigController {

    /** 平台电话最终兜底：yml 显式留空时 @Value 拿到的是空串而非默认值，必须在代码里再兜一次 */
    private static final String DEFAULT_PLATFORM_PHONE = "10086";

    /** 后台参数 key：系统管理 → 参数设置 → 新增同名参数即可覆盖默认值 */
    private static final String KEY_QRCODE = "wash.cs.wecom-qrcode-url";
    private static final String KEY_PLATFORM_PHONE = "wash.cs.platform-phone";
    private static final String KEY_STATION_PHONE = "wash.cs.station-phone";
    private static final String KEY_NIGHT_TIP = "wash.cs.night-tip";

    @Autowired
    private ISysConfigService sysConfigService;

    @Value("${wash.customer-service.wecom-qrcode-url:}")
    private String wecomQrcodeUrl;

    /** 平台客服电话：默认 10086，保证任何情况下都拨得出去 */
    @Value("${wash.customer-service.platform-phone:10086}")
    private String platformPhone;

    @Value("${wash.customer-service.station-phone:}")
    private String stationPhone;

    @Value("${wash.customer-service.night-tip:}")
    private String nightTip;

    @GetMapping("/api/v1/config/customer-service")
    public ApiResult<CustomerServiceVO> customerService() {
        CustomerServiceVO vo = new CustomerServiceVO();
        vo.setWecomQrcodeUrl(resolve(KEY_QRCODE, wecomQrcodeUrl));
        String platform = resolve(KEY_PLATFORM_PHONE, platformPhone);
        vo.setPlatformPhone(platform == null || platform.isBlank() ? DEFAULT_PLATFORM_PHONE : platform);
        vo.setStationPhone(resolve(KEY_STATION_PHONE, stationPhone));
        vo.setNightTip(resolve(KEY_NIGHT_TIP, nightTip));
        return ApiResult.ok(vo);
    }

    /** 后台参数优先；未配置（null/空串）回落 yml 值 */
    private String resolve(String key, String fallback) {
        String value = sysConfigService.selectConfigByKey(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
