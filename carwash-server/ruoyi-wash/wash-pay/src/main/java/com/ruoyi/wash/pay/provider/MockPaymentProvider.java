package com.ruoyi.wash.pay.provider;

import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.pay.domain.WashRefundOrder;
import com.ruoyi.wash.pay.dto.PrepayVO;

import java.util.UUID;

/**
 * 本地开发渠道：不发真实请求。
 *
 * <p>prepay 返回一组 mock 参数（前端拿去调不起真实支付，因此本地走 /mock-pay 或 notify 模拟回调）；
 * refund 直接返回成功，用于跑通"取消 → 退款单 → 已退款"链路。
 */
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String code() {
        return "mock";
    }

    @Override
    public PrepayVO prepay(WashOrder order) {
        PrepayVO vo = new PrepayVO();
        vo.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
        vo.setNonceStr(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        vo.setPackageValue("prepay_id=mock_" + order.getOrderNo());
        vo.setSignType("RSA");
        vo.setPaySign("mock-sign");
        vo.setExpireAt(order.getPayExpireAt());
        return vo;
    }

    @Override
    public RefundResult refund(WashRefundOrder refund) {
        return RefundResult.ok("mock-refund-" + refund.getRefundNo());
    }
}
