package com.ruoyi.wash.order.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.order.dto.PayResultVO;
import com.ruoyi.wash.order.state.WashOrderStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付（开发期）。契约：openapi.yaml POST /api/v1/payments/{orderNo}/mock-pay（x-dev-only）。
 *
 * <p>真实微信支付接入后，支付回调同样调用 WashOrderStateService.paySuccess，
 * 本控制器即可下线，状态机与日志逻辑零改动。
 */
@RestController
public class WxPayController {

    @Autowired
    private WashOrderStateService stateService;

    /** 生产必须为 false：配置在 application.yml，禁止在代码里写死 true */
    @Value("${wash.pay.mock-enabled:false}")
    private boolean mockEnabled;

    @PostMapping("/api/v1/payments/{orderNo}/mock-pay")
    public ApiResult<PayResultVO> mockPay(@PathVariable String orderNo) {
        if (!mockEnabled) {
            throw new ApiException(ErrorCode.A0003, "模拟支付未开启");
        }
        OrderStatus status = stateService.paySuccess(orderNo, MemberContext.require());

        PayResultVO vo = new PayResultVO();
        vo.setOrderNo(orderNo);
        vo.setStatus(status.name());
        return ApiResult.ok(vo);
    }
}
