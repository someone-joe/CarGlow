package com.ruoyi.wash.pay.provider;

import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.pay.domain.WashRefundOrder;
import com.ruoyi.wash.pay.dto.PrepayVO;

/**
 * 支付渠道适配层 —— 业务代码只认这个接口，不认具体渠道。
 *
 * <p>切真实微信支付的步骤：
 * 1. 引入微信支付 SDK（如 com.github.wechatpay-apiv3:wechatpay-java）；
 * 2. 实现本接口的 WechatPayProvider（目前是占位实现，会明确报错而不是假装成功）；
 * 3. application.yml 把 wash.pay.provider 改成 wechat 并填商户号等配置。
 * 业务侧（下单、退款、状态机）无需改动。
 */
public interface PaymentProvider {

    /** 渠道标识：mock / wechat */
    String code();

    /** 下单预支付：返回小程序调起支付所需的参数 */
    PrepayVO prepay(WashOrder order);

    /**
     * 退款。
     *
     * @return 结果：success=true 表示渠道已受理/成功
     */
    RefundResult refund(WashRefundOrder refund);

    record RefundResult(boolean success, String tradeNo, String failReason) {

        public static RefundResult ok(String tradeNo) {
            return new RefundResult(true, tradeNo, null);
        }

        public static RefundResult fail(String reason) {
            return new RefundResult(false, null, reason);
        }
    }
}
