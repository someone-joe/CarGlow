package com.ruoyi.wash.common.statemachine;

/**
 * 订单状态流转事件 —— 全项目唯一定义处（SSOT）。
 *
 * <p>定义来源：《订单状态机规则表.md》第二节，共 17 个事件。
 * 状态不允许被直接修改，只能通过「事件」驱动流转。
 */
public enum OrderEvent {

    /* ---- 正常主链路 ---- */
    PAY_SUCCESS("支付成功"),
    DEPOSIT_KEY("客户存放钥匙"),
    TAKE_KEY("工作人员取钥匙"),
    PICK_CAR_DONE("取车拍照完成"),
    ARRIVE_STATION("车辆入场打卡"),
    SOP_DONE("SOP 全部完成"),
    QC_FAIL("质检不合格，打回重洗"),
    QC_PASS("质检通过"),
    LEAVE_STATION("车辆驶离中央站"),
    RETURN_DONE("停放完成且钥匙归柜"),
    TAKE_KEY_BACK("客户取回钥匙"),
    REVIEW_SUBMIT("客户提交评价"),
    AUTO_FINISH("超时未评价，自动完成"),

    /* ---- 顺延（自环） ---- */
    POSTPONE("超时未存钥匙，顺延次日"),

    /* ---- 取消与退款 ---- */
    CANCEL("取消订单"),
    APPLY_REFUND("申请退款"),
    REFUND_SUCCESS("退款成功");

    private final String label;

    OrderEvent(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
