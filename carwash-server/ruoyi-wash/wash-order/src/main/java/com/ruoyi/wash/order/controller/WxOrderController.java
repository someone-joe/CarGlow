package com.ruoyi.wash.order.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.order.dto.OrderPageVO;
import com.ruoyi.wash.order.service.WashOrderQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端订单接口。契约：openapi.yaml /api/v1/orders（GET）。
 */
@RestController
public class WxOrderController {

    @Autowired
    private WashOrderQueryService queryService;

    @GetMapping("/api/v1/orders")
    public ApiResult<OrderPageVO> list(@RequestParam(defaultValue = "ONGOING") String tab,
                                       @RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(queryService.page(MemberContext.require(), tab, pageNum, pageSize));
    }
}
