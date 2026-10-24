package com.ruoyi.wash.pay.provider;

import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.pay.domain.WashRefundOrder;
import com.ruoyi.wash.pay.dto.PrepayVO;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 本地开发渠道：不发真实请求。
 *
 * <p>prepay 返回一组 mock 参数（前端拿去调不起真实支付，因此本地走 /mock-pay 或 notify 模拟回调）；
 * refund 默认直接返回成功，用于跑通"取消 → 退款单 → 已退款"链路。
 *
 * <p>forceFail 仅用于接口自动化：构造"渠道失败 → 待重试"的场景（解锁 P07）。
 * 由 WashPayService.setMockForceFail 切换，正常业务路径恒为 false。
 */
public class MockPaymentProvider implements PaymentProvider {

    private final AtomicBoolean forceFail = new AtomicBoolean(false);

    public void setForceFail(boolean forceFail) {
        this.forceFail.set(forceFail);
    }

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
        if (forceFail.get()) {
            return RefundResult.fail("mock 强制失败（测试注入）");
        }
        return RefundResult.ok("mock-refund-" + refund.getRefundNo());
    }
}
