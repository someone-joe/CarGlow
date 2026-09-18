package com.ruoyi.wash.order.event;

/**
 * 订单已取消事件 —— 用于解耦"取消"与"退款"。
 *
 * <p>wash-order 不依赖 wash-pay：这里只发事件，由 wash-pay 监听并创建退款单。
 * 好处是支付渠道怎么变，订单域代码都不用动。
 *
 * @param orderNo  订单号
 * @param memberId 会员ID
 * @param amount   应退金额（分）
 * @param reason   取消原因（退款单沿用）
 */
public record OrderCanceledEvent(String orderNo, Long memberId, Long amount, String reason) {
}
