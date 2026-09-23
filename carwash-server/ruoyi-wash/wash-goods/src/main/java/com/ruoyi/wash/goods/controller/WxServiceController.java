package com.ruoyi.wash.goods.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.goods.dto.ServiceCategoryVO;
import com.ruoyi.wash.goods.dto.ServiceDetailVO;
import com.ruoyi.wash.goods.dto.ServiceVO;
import com.ruoyi.wash.goods.service.WashServiceQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端服务项。契约：openapi.yaml GET /api/v1/services。
 *
 * <p>服务分类（/api/v1/service-categories）与详情（/api/v1/services/{serviceId}）
 * 暂未实现：本项目当前只有一类洗车服务，无分类表，等商品模块正式开工再补。
 */
@RestController
public class WxServiceController {

    @Autowired
    private WashServiceQueryService serviceQuery;

    @GetMapping("/api/v1/services")
    public ApiResult<List<ServiceVO>> list() {
        return ApiResult.ok(serviceQuery.listEnabled());
    }

    /** 契约：openapi.yaml GET /api/v1/service-categories */
    @GetMapping("/api/v1/service-categories")
    public ApiResult<List<ServiceCategoryVO>> categories() {
        return ApiResult.ok(serviceQuery.listCategories());
    }

    /** 契约：openapi.yaml GET /api/v1/services/{serviceId}（bug092408：此前缺失，请求落到静态资源 404） */
    @GetMapping("/api/v1/services/{serviceId}")
    public ApiResult<ServiceDetailVO> detail(@PathVariable Long serviceId) {
        return ApiResult.ok(serviceQuery.detail(serviceId));
    }
}
