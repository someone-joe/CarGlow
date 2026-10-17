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
    /** 处置人（后台登录账号）：退款是财务动作，事后必须能查到谁批的 */
    private String handlerBy;
    /** 处置说明：驳回必填 */
    private String handleRemark;
    /** 处置时间（毫秒时间戳） */
    private Long handleTime;
    /** 创建时间（毫秒时间戳）；查询用 UNIX_TIMESTAMP(create_time)*1000 换算，落库仍是 datetime */
    private Long createTime;

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

    public String getHandlerBy() {
        return handlerBy;
    }

    public void setHandlerBy(String handlerBy) {
        this.handlerBy = handlerBy;
    }

    public String getHandleRemark() {
        return handleRemark;
    }

    public void setHandleRemark(String handleRemark) {
        this.handleRemark = handleRemark;
    }

    public Long getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(Long handleTime) {
        this.handleTime = handleTime;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
}
