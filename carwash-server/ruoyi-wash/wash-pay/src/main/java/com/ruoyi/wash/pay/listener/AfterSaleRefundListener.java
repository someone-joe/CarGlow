package com.ruoyi.wash.pay.listener;

import com.ruoyi.wash.order.event.AfterSaleRefundEvent;
import com.ruoyi.wash.pay.service.WashPayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听"售后工单同意退款"→ 建退款单并调渠道。
 *
 * <p>与 OrderCanceledListener 完全同构：wash-order 不依赖 wash-pay，只发事件过来，
 * 怎么退、退给谁、失败了怎么重试，全部由支付域决定。售后侧因此不用知道任何渠道细节。
 */
@Component
public class AfterSaleRefundListener {

    private static final Logger log = LoggerFactory.getLogger(AfterSaleRefundListener.class);

    @Autowired
    private WashPayService payService;

    @EventListener
    public void onAfterSaleRefund(AfterSaleRefundEvent event) {
        log.info("[售后退款] 开始退款 orderNo={} afterSaleNo={} amount={}分",
                event.orderNo(), event.afterSaleNo(), event.amount());
        try {
            payService.refundForAfterSale(event);
        } catch (Exception e) {
            // 吞掉异常：工单处置（同意）本身已经成功，不能因为渠道出问题让处置接口报错；
            // 退款单已落库，RefundRetryJob 会继续重试
            log.error("[售后退款] 首次退款异常，交由定时任务重试 orderNo={} afterSaleNo={}",
                    event.orderNo(), event.afterSaleNo(), e);
        }
    }
}
