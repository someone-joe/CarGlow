package com.ruoyi.wash.common.statemachine;

/**
 * 状态流转的触发方（谁发起的）。
 *
 * <p>同时用于落库的 operator_type 与 source 两个字段。
 * PRD 附录 D 中这两个字段是分开的，但业务上「谁操作」与「从哪端来」始终同源
 * （客户只从 C 端来、定时任务没有具体人），拆成两套枚举必然出现不一致，故合并为一。
 *
 * <p>2026-09-19 架构调整：原「取送端」与「作业端」合并为单一「师傅端」工作人员角色
 * （PRD 第 3 章角色模型为 客户/工作人员/管理员 三级，取送与作业为子功能；前期 2 名合伙人自作业）。
 * 故取送人员与中央站技师统一为 {@code WORKER}（师傅）；动作类型仍由事件本身（取车/洗车/质检）区分，合并端不合并审计。
 */
public enum OrderOperatorType {

    /** 车主，来自 C 端小程序 */
    CUSTOMER("客户"),
    /** 师傅，来自师傅端小程序（取送 + 作业合并） */
    WORKER("师傅"),
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
