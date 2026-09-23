package com.ruoyi.wash.order.dto;

/** 今晚剩余产能。契约：openapi.yaml /api/v1/capacity 的 CapacityVO。 */
public class CapacityVO {

    private String date;
    /** 单日上限，0 表示当日不限量 */
    private Integer total;
    private Integer used;
    private Integer remaining;
    private String depositDeadline;
    private String promiseReturnTime;
    /** 营业时段起止（整点），下单页据此生成可选时段，站点可配置 */
    private String businessStart;
    private String businessEnd;
    private Boolean soldOut;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getUsed() {
        return used;
    }

    public void setUsed(Integer used) {
        this.used = used;
    }

    public Integer getRemaining() {
        return remaining;
    }

    public void setRemaining(Integer remaining) {
        this.remaining = remaining;
    }

    public String getDepositDeadline() {
        return depositDeadline;
    }

    public void setDepositDeadline(String depositDeadline) {
        this.depositDeadline = depositDeadline;
    }

    public String getPromiseReturnTime() {
        return promiseReturnTime;
    }

    public void setPromiseReturnTime(String promiseReturnTime) {
        this.promiseReturnTime = promiseReturnTime;
    }

    public String getBusinessStart() {
        return businessStart;
    }

    public void setBusinessStart(String businessStart) {
        this.businessStart = businessStart;
    }

    public String getBusinessEnd() {
        return businessEnd;
    }

    public void setBusinessEnd(String businessEnd) {
        this.businessEnd = businessEnd;
    }

    public Boolean getSoldOut() {
        return soldOut;
    }

    public void setSoldOut(Boolean soldOut) {
        this.soldOut = soldOut;
    }
}
