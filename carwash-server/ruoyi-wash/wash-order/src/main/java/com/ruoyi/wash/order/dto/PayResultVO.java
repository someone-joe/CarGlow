package com.ruoyi.wash.order.dto;

/** 支付结果 —— 与 openapi.yaml 的 PayResultVO 逐字段一致（x-dev-only）。 */
public class PayResultVO {

    private String orderNo;
    private String status;

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
}
