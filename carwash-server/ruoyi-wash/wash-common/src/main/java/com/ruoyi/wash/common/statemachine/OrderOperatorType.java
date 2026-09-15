package com.ruoyi.wash.common.statemachine;

/**
 * 状态流转的触发方（谁发起的）。
 *
 * <p>同时用于落库的 operator_type 与 source 两个字段。
 * PRD 附录 D 中这两个字段是分开的，但业务上「谁操作」与「从哪端来」始终同源
 * （客户只从 C 端来、定时任务没有具体人），拆成两套枚举必然出现不一致，故合并为一。
 */
public enum OrderOperatorType {

    /** 车主，来自 C 端小程序 */
    CUSTOMER("客户"),
    /** 取送人员，来自取送端小程序 */
    PICKER("取送人员"),
    /** 中央站技师，来自作业端 */
    STATION("中央站技师"),
    /** 站长/客服/财务，来自运营后台 */
    ADMIN("后台管理员"),
    /** 定时任务 */
    JOB("定时任务"),
    /** 钥匙柜回调 */
    DEVICE("柜机回调");

    private final String label;

    OrderOperatorType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
