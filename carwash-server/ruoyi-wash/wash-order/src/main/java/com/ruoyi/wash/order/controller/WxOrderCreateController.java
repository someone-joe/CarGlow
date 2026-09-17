package com.ruoyi.wash.order.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.order.dto.CreateOrderRequest;
import com.ruoyi.wash.order.dto.CreateOrderVO;
import com.ruoyi.wash.order.service.WashOrderCreateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 下单接口。契约：openapi.yaml POST /api/v1/orders。
 * 幂等键取请求头 Idempotency-Key（契约 required）。
 */
@RestController
public class WxOrderCreateController {

    @Autowired
    private WashOrderCreateService createService;

    @PostMapping("/api/v1/orders")
    public ApiResult<CreateOrderVO> create(@RequestBody CreateOrderRequest request,
                                           @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResult.ok(createService.create(MemberContext.require(), request, idempotencyKey));
    }
}
