package com.ruoyi.wash.pay.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.pay.dto.PrepayVO;
import com.ruoyi.wash.pay.service.WashPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 支付接口。契约：openapi.yaml /api/v1/payments/{orderNo}/prepay 与 /api/v1/payments/wechat/notify。
 *
 * <p>notify 是微信服务器回调，无登录态（已在 MemberWebConfig 中排除拦截）。
 */
@RestController
public class WxPaymentController {

    @Autowired
    private WashPayService payService;

    @PostMapping("/api/v1/payments/{orderNo}/prepay")
    public ApiResult<PrepayVO> prepay(@PathVariable String orderNo) {
        return ApiResult.ok(payService.prepay(orderNo, MemberContext.require()));
    }

    /**
     * 支付结果回调。真实环境由微信服务器调用（需验签 + 解密，接渠道时补）；
     * 本地 provider=mock 时可直接 POST {"orderNo":"SE..."} 模拟。
     */
    @PostMapping("/api/v1/payments/wechat/notify")
    public Map<String, String> notify(@RequestBody(required = false) Map<String, String> body) {
        String orderNo = body == null ? null : body.get("orderNo");
        if (orderNo == null || orderNo.isBlank()) {
            return Map.of("code", "FAIL", "message", "缺少订单号");
        }
        boolean advanced = payService.handlePaidNotify(orderNo);
        return Map.of("code", "SUCCESS", "message", advanced ? "已处理" : "重复通知已忽略");
    }
}
