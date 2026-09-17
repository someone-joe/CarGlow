package com.ruoyi.wash.order.dto;

/**
 * 订单按钮 —— 与 openapi.yaml 的 OrderAction 逐字段一致。
 * 按钮由后端按状态机规则返回，前端不得自行推断。
 */
public class OrderActionVO {

    private String action;
    private String label;
    private Boolean enabled;

    public OrderActionVO() {
    }

    public OrderActionVO(String action, String label, Boolean enabled) {
        this.action = action;
        this.label = label;
        this.enabled = enabled;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
