package com.ruoyi.wash.invoice.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 发票申请。表 wash_invoice。按订单开票：金额以订单实付为准（后端权威，不收前端金额）。
 *
 * <p>titleType：PERSONAL 个人 / COMPANY 企业（企业必填税号）。status 见 {@link InvoiceStatus}。
 */
public class WashInvoice extends BaseEntity {

    private Long invoiceId;
    private Long memberId;
    private String orderNo;
    /** 开票金额（分），取自订单实付 */
    private Long amount;
    private String titleType;
    private String title;
    private String taxNo;
    private String email;
    private String status;
    /** 开票后回填的发票号 */
    private String invoiceNo;
    private String rejectReason;
    private String remark;

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getTitleType() {
        return titleType;
    }

    public void setTitleType(String titleType) {
        this.titleType = titleType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTaxNo() {
        return taxNo;
    }

    public void setTaxNo(String taxNo) {
        this.taxNo = taxNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
