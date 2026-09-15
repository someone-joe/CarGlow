package com.ruoyi.wash.common.statemachine;

import java.time.LocalDateTime;

/**
 * 订单状态流转日志实体，对应表 wash_order_status_log。
 *
 * <p>只增不删、不可修改：C 端 8 节点时间轴与后台追溯都读它，
 * 因此不提供 update 方法，后台也不开放删除入口。
 */
public class OrderStatusLog {

    private Long id;

    private Long orderId;

    /** 流转前状态 */
    private String fromStatus;

    /** 流转后状态 */
    private String toStatus;

    /** 触发事件，见 {@link OrderEvent} */
    private String event;

    /** 操作者类型，见 {@link OrderOperatorType} */
    private String operatorType;

    /** 操作者 ID：客户/员工/管理员；定时任务与柜机回调可为空 */
    private Long operatorId;

    /** 来源端，与 operatorType 同源 */
    private String source;

    /** 原因/备注：取消、质检不合格时必填 */
    private String reason;

    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(String operatorType) {
        this.operatorType = operatorType;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
