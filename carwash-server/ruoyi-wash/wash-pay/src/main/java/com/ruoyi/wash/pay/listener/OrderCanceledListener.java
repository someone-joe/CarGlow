package com.ruoyi.wash.pay.listener;

import com.ruoyi.wash.order.event.OrderCanceledEvent;
import com.ruoyi.wash.pay.service.WashPayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听"订单已取消"→ 自动退款。
 *
 * <p>用事件而不是直接调用，是为了让订单域不依赖支付域：
 * 将来接真实渠道、加风控审核（大额人工确认），只改这里。
 *
 * <p>注意：默认同步执行，取消接口返回时退款单已创建；
 * 若渠道慢或不可用，退款单会留在待重试状态，由定时任务兜底。
 */
@Component
public class OrderCanceledListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCanceledListener.class);

    @Autowired
    private WashPayService payService;

    @EventListener
    public void onCanceled(OrderCanceledEvent event) {
        log.info("[订单取消] 开始自动退款 orderNo={} amount={}分", event.orderNo(), event.amount());
        try {
            payService.refundForCanceled(event);
        } catch (Exception e) {
            // 吞掉异常：取消本身已经成功，不能因为退款失败让取消接口报错；
            // 退款单已在库里，定时任务会重试
            log.error("[自动退款异常] orderNo={}，已留退款单待重试", event.orderNo(), e);
        }
    }
}
