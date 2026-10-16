package com.ruoyi.wash.selfwash.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.selfwash.dto.SelfwashOrderVO;
import com.ruoyi.wash.selfwash.service.WashSelfwashOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端自助洗车订单。契约：openapi.yaml /api/v1/selfwash/orders**。
 *
 * <p>与无感 /api/v1/orders 完全分离；储值抵扣复用 wash-pay 钱包。
 */
@RestController
public class WxSelfwashOrderController {

    @Autowired
    private WashSelfwashOrderService service;

    @PostMapping("/api/v1/selfwash/orders")
    public ApiResult<String> create(@RequestBody(required = false) CreateRequest req) {
        if (req == null || req.workstationId() == null || req.serviceId() == null) {
            throw new ApiException(ErrorCode.A0001, "参数不完整");
        }
        return ApiResult.ok(service.create(MemberContext.require(), req.workstationId(), req.serviceId()));
    }

    @GetMapping("/api/v1/selfwash/orders")
    public ApiResult<List<SelfwashOrderVO>> list(@RequestParam(required = false) String tab) {
        return ApiResult.ok(service.list(MemberContext.require(), tab));
    }

    @GetMapping("/api/v1/selfwash/orders/{orderNo}")
    public ApiResult<SelfwashOrderVO> detail(@PathVariable String orderNo) {
        return ApiResult.ok(service.detail(MemberContext.require(), orderNo));
    }

    @PostMapping("/api/v1/selfwash/orders/{orderNo}/pay-balance")
    public ApiResult<Void> payBalance(@PathVariable String orderNo) {
        service.payWithBalance(MemberContext.require(), orderNo);
        return ApiResult.ok();
    }

    @PostMapping("/api/v1/selfwash/orders/{orderNo}/mock-pay")
    public ApiResult<Void> payMock(@PathVariable String orderNo) {
        service.payMock(MemberContext.require(), orderNo);
        return ApiResult.ok();
    }

    @PostMapping("/api/v1/selfwash/orders/{orderNo}/cancel")
    public ApiResult<Void> cancel(@PathVariable String orderNo, @RequestBody(required = false) CancelRequest req) {
        service.cancel(MemberContext.require(), orderNo, req == null ? null : req.reason());
        return ApiResult.ok();
    }

    public record CreateRequest(Long workstationId, Long serviceId) {
    }

    public record CancelRequest(String reason) {
    }
}
