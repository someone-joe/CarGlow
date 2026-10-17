package com.ruoyi.wash.promo.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.promo.dto.BannerVO;
import com.ruoyi.wash.promo.service.WashBannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端首页 Banner。契约：openapi.yaml GET /api/v1/banners。
 *
 * <p>只返回上架且在当前生效窗口内的轮播；无配置时返回空数组，
 * 前端按产品要求降级为「服务理念」文字图。
 */
@RestController
public class WxBannerController {

    @Autowired
    private WashBannerService bannerService;

    @GetMapping("/api/v1/banners")
    public ApiResult<List<BannerVO>> list() {
        return ApiResult.ok(bannerService.homeBanners());
    }
}
