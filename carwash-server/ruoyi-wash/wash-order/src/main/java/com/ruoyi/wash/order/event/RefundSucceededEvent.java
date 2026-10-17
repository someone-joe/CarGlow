package com.ruoyi.wash.order.event;

/**
 * 退款已到账事件 —— 由 wash-pay 在渠道真正退款成功后发出。
 *
 * <p>方向与前几个事件相反：这里是支付域告诉订单域"钱已经退了"，
 * 订单域据此把售后工单收尾为 DONE，运营不必人工回头改工单状态。
 *
 * <p>监听器必须容忍重复（渠道回调本身会重发）：置 DONE 是幂等操作。
 *
 * @param orderNo  订单号
 * @param refundNo 退款单号（对账用）
 * @param amount   退款金额（分）
 */
public record RefundSucceededEvent(String orderNo, String refundNo, Long amount) {
}
