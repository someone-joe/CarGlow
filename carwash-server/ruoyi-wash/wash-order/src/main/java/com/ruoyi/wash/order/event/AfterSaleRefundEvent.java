package com.ruoyi.wash.order.event;

/**
 * 售后工单「同意退款」事件 —— 售后走向退款的唯一入口。
 *
 * <p>与 {@link OrderCanceledEvent} 同构：wash-order 只发事件、不依赖 wash-pay，
 * 建退款单、调渠道、推进退款状态全在 wash-pay 监听侧。支付渠道怎么换，售后流程都不用动。
 *
 * <p>为什么不复用 OrderCanceledEvent：两者来源不同（取消 vs 运营审核同意），
 * 退款单的 reason 与溯源口径也不同。合并会让"这笔钱为什么退"在财务侧失去来源。
 *
 * @param afterSaleNo 工单号（退款单据此溯源到具体工单）
 * @param orderNo     订单号
 * @param memberId    会员ID
 * @param amount      应退金额（分）
 * @param reason      退款原因
 */
public record AfterSaleRefundEvent(String afterSaleNo, String orderNo, Long memberId, Long amount, String reason) {
}
