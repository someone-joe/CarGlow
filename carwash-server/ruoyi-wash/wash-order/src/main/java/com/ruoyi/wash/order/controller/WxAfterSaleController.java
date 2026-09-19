package com.ruoyi.wash.order.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.order.service.WashAfterSaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** C 端评价与售后。契约：openapi.yaml /reviews、/after-sales。 */
@RestController
public class WxAfterSaleController {

    @Autowired
    private WashAfterSaleService afterSaleService;

    /** 提交评价：契约 openapi.yaml POST /api/v1/orders/{orderNo}/reviews（rating 必填） */
    @PostMapping("/api/v1/orders/{orderNo}/reviews")
    public ApiResult<Void> review(@PathVariable String orderNo,
                                  @RequestBody(required = false) ReviewRequest request) {
        if (request == null || request.rating() == null) {
            throw new com.ruoyi.wash.common.api.ApiException(
                    com.ruoyi.wash.common.api.ErrorCode.A0001, "评分必填");
        }
        afterSaleService.submitReview(orderNo, MemberContext.require(), request.rating(),
                request.tags(), request.content());
        return ApiResult.ok();
    }

    /** 提交售后：契约 openapi.yaml POST /api/v1/after-sales（orderNo、type 必填），返回工单号 */
    @PostMapping("/api/v1/after-sales")
    public ApiResult<String> afterSale(@RequestBody(required = false) AfterSaleRequest request) {
        if (request == null || request.orderNo() == null) {
            throw new com.ruoyi.wash.common.api.ApiException(
                    com.ruoyi.wash.common.api.ErrorCode.A0001, "订单号必填");
        }
        String ticketNo = afterSaleService.createAfterSale(request.orderNo(), MemberContext.require(),
                request.type(), request.reason());
        return ApiResult.ok(ticketNo);
    }

    public record ReviewRequest(Integer rating, List<String> tags, String content) {
    }

    public record AfterSaleRequest(String orderNo, String type, String reason, List<String> images) {
    }
}
