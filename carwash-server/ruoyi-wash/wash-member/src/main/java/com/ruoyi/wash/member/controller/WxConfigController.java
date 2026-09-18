package com.ruoyi.wash.member.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.member.dto.CustomerServiceVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端通用配置。契约：openapi.yaml GET /api/v1/config/customer-service。
 *
 * <p>本类属 misc 域，项目暂无 misc 模块，暂挂 wash-member（同为 C 端接口层）；
 * 将来 misc 模块开工时整体挪走，接口路径不变，前端无感。
 */
@RestController
public class WxConfigController {

    @Value("${wash.customer-service.wecom-qrcode-url:}")
    private String wecomQrcodeUrl;

    @Value("${wash.customer-service.platform-phone:}")
    private String platformPhone;

    @Value("${wash.customer-service.station-phone:}")
    private String stationPhone;

    @Value("${wash.customer-service.night-tip:}")
    private String nightTip;

    @GetMapping("/api/v1/config/customer-service")
    public ApiResult<CustomerServiceVO> customerService() {
        CustomerServiceVO vo = new CustomerServiceVO();
        vo.setWecomQrcodeUrl(wecomQrcodeUrl);
        vo.setPlatformPhone(platformPhone);
        vo.setStationPhone(stationPhone);
        vo.setNightTip(nightTip);
        return ApiResult.ok(vo);
    }
}
