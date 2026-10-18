package com.ruoyi.wash.order.dto;

/**
 * 售后工单出参（C 端「我的售后」）。契约：openapi.yaml AfterSaleVO。
 *
 * <p>为什么不直接把 WashAfterSale 实体返回：实体将来加内部字段会被顺带带出去。
 * 与 AdminWorkerVO 不返回 password 是同一个口径——出参一律用 VO 收口。
 * 金额一律分，时间一律毫秒时间戳。
 */
public class AfterSaleVO {

    private String afterSaleNo;
    private String orderNo;
    /** REWASH 重洗 / REFUND 退款 / CLAIM 理赔 */
    private String type;
    private String reason;
    /** INIT 待处理 / PROCESSING 处理中 / DONE 已完成 / REJECTED 已驳回 */
    private String status;
    /** 状态中文名，由后端枚举给出（前端只展示不翻译，避免两端各写一份） */
    private String statusLabel;
    private Long amount;
    private String handlerBy;
    private String handleRemark;
    private Long handleTime;
    private Long createTime;

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

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
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
