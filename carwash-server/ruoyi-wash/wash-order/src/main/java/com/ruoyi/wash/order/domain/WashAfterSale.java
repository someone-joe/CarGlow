package com.ruoyi.wash.order.domain;

/** 售后单。表 wash_after_sale。金额一律分。 */
public class WashAfterSale {

    private Long afterSaleId;
    /** 售后单号（唯一，幂等键，对外即 ticketNo） */
    private String afterSaleNo;
    private String orderNo;
    private Long memberId;
    /** REWASH 重洗 / REFUND 退款 / CLAIM 理赔 */
    private String type;
    private String reason;
    /** INIT 待处理 / PROCESSING 处理中 / DONE 已完成 / REJECTED 已驳回 */
    private String status;
    private Long amount;

    public Long getAfterSaleId() {
        return afterSaleId;
    }

    public void setAfterSaleId(Long afterSaleId) {
        this.afterSaleId = afterSaleId;
    }

    public String getAfterSaleNo() {
        return afterSaleNo;
    }

    public void setAfterSaleNo(String afterSaleNo) {
        this.afterSaleNo = afterSaleNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }
}
