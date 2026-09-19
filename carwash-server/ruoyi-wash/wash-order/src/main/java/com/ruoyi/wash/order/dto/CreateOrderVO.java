package com.ruoyi.wash.order.dto;

/** 下单返回 —— 与 openapi.yaml 的 CreateOrderVO 逐字段一致。 */
public class CreateOrderVO {

    private String orderNo;
    private String status;
    /** 应付金额，单位：分（已扣除优惠券抵扣） */
    private Long payAmount;
    /** 优惠券抵扣金额，单位：分；未用券时为 0 */
    private Long couponDiscount;
    /** 支付截止时间，毫秒时间戳 */
    private Long payExpireAt;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(Long payAmount) {
        this.payAmount = payAmount;
    }

    public Long getCouponDiscount() {
        return couponDiscount;
    }

    public void setCouponDiscount(Long couponDiscount) {
        this.couponDiscount = couponDiscount;
    }

    public Long getPayExpireAt() {
        return payExpireAt;
    }

    public void setPayExpireAt(Long payExpireAt) {
        this.payExpireAt = payExpireAt;
    }
}
