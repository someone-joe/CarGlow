package com.ruoyi.wash.order.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.order.dto.CapacityVO;
import com.ruoyi.wash.order.service.WashCapacityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** C 端产能。契约：openapi.yaml GET /api/v1/capacity（date 默认今天）。 */
@RestController
public class WxCapacityController {

    @Autowired
    private WashCapacityService capacityService;

    @GetMapping("/api/v1/capacity")
    public ApiResult<CapacityVO> capacity(@RequestParam(required = false) String date) {
        return ApiResult.ok(capacityService.capacity(date));
    }
}
