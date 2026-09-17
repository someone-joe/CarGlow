package com.ruoyi.wash.order.dto;

/** 下单返回 —— 与 openapi.yaml 的 CreateOrderVO 逐字段一致。 */
public class CreateOrderVO {

    private String orderNo;
    private String status;
    /** 应付金额，单位：分 */
    private Long payAmount;
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

    public Long getPayExpireAt() {
        return payExpireAt;
    }

    public void setPayExpireAt(Long payExpireAt) {
        this.payExpireAt = payExpireAt;
    }
}
