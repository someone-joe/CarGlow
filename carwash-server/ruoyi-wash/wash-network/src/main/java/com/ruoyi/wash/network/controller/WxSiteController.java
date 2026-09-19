package com.ruoyi.wash.network.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.network.dto.CommunityVO;
import com.ruoyi.wash.network.dto.SiteVO;
import com.ruoyi.wash.network.service.WashSiteQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端站点与小区。契约：openapi.yaml /api/v1/site/current、/api/v1/communities。
 *
 * <p>当前站点由配置指定（wash.default-site-id）：MVP 只有一个中央站，
 * 多站点后这里改成按定位/小区解析，接口不变。
 */
@RestController
public class WxSiteController {

    @Autowired
    private WashSiteQueryService siteQuery;

    @Value("${wash.default-site-id:1}")
    private Long defaultSiteId;

    @GetMapping("/api/v1/site/current")
    public ApiResult<SiteVO> current() {
        return ApiResult.ok(siteQuery.current(defaultSiteId));
    }

    @GetMapping("/api/v1/communities")
    public ApiResult<List<CommunityVO>> communities() {
        return ApiResult.ok(siteQuery.listCommunities());
    }
}
