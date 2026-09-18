package com.ruoyi.wash.order.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.order.dto.OrderDetailVO;
import com.ruoyi.wash.order.dto.OrderPageVO;
import com.ruoyi.wash.order.service.WashOrderDetailService;
import com.ruoyi.wash.order.service.WashOrderQueryService;
import com.ruoyi.wash.order.state.WashOrderStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端订单接口。契约：openapi.yaml /api/v1/orders（GET）。
 */
@RestController
public class WxOrderController {

    @Autowired
    private WashOrderQueryService queryService;

    @Autowired
    private WashOrderDetailService detailService;

    @Autowired
    private WashOrderStateService stateService;

    @GetMapping("/api/v1/orders")
    public ApiResult<OrderPageVO> list(@RequestParam(defaultValue = "ONGOING") String tab,
                                       @RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(queryService.page(MemberContext.require(), tab, pageNum, pageSize));
    }

    /** 取消订单。契约：openapi.yaml POST /api/v1/orders/{orderNo}/cancel（reason 必填） */
    @PostMapping("/api/v1/orders/{orderNo}/cancel")
    public ApiResult<Void> cancel(@PathVariable String orderNo, @RequestBody CancelRequest request) {
        stateService.cancel(orderNo, MemberContext.require(), request == null ? null : request.reason());
        return ApiResult.ok();
    }

    /**
     * 紧急取钥匙。契约：openapi.yaml POST /api/v1/orders/{orderNo}/emergency-take-key。
     *
     * <p>车未动 → 取消 + 已支付全额退款（并告警留痕）；车已动 → 状态机拒绝（B1002）。
     * 契约里提到的"预计完成时间"暂不返回：依赖承诺还车时间（站点/产能模块），响应体也没有 data 字段。
     */
    @PostMapping("/api/v1/orders/{orderNo}/emergency-take-key")
    public ApiResult<Void> emergencyTakeKey(@PathVariable String orderNo) {
        stateService.emergencyTakeKey(orderNo, MemberContext.require());
        return ApiResult.ok();
    }

    /** 与契约 request body 逐字段一致 */
    public record CancelRequest(String reason) {
    }

    /** 订单详情（含 8 节点时间轴）。契约：openapi.yaml GET /api/v1/orders/{orderNo} */
    @GetMapping("/api/v1/orders/{orderNo}")
    public ApiResult<OrderDetailVO> detail(@PathVariable String orderNo) {
        return ApiResult.ok(detailService.detail(orderNo, MemberContext.require()));
    }
}
