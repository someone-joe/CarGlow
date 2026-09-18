package com.ruoyi.wash.order.dto;

import java.util.List;

/**
 * 后台首页看板统计。
 *
 * <p>全部是对 wash_order 的只读聚合，不新增表、不写状态。
 * 口径说明见 WashOrderStatsService，前端只做展示不做二次计算。
 */
public class DashboardStatsVO {

    /** 今日下单数（按 create_time 计） */
    private long todayOrderCount;

    /** 作业中：车已取走进入作业环节 */
    private long washingCount;

    /** 待存钥匙：等客户把钥匙放进柜子 */
    private long waitingKeyCount;

    /** 异常：退款中，需人工跟进 */
    private long abnormalCount;

    /** 各状态分布（按 OrderStatus 枚举顺序） */
    private List<StatusCountVO> statusBreakdown;

    public long getTodayOrderCount() {
        return todayOrderCount;
    }

    public void setTodayOrderCount(long todayOrderCount) {
        this.todayOrderCount = todayOrderCount;
    }

    public long getWashingCount() {
        return washingCount;
    }

    public void setWashingCount(long washingCount) {
        this.washingCount = washingCount;
    }

    public long getWaitingKeyCount() {
        return waitingKeyCount;
    }

    public void setWaitingKeyCount(long waitingKeyCount) {
        this.waitingKeyCount = waitingKeyCount;
    }

    public long getAbnormalCount() {
        return abnormalCount;
    }

    public void setAbnormalCount(long abnormalCount) {
        this.abnormalCount = abnormalCount;
    }

    public List<StatusCountVO> getStatusBreakdown() {
        return statusBreakdown;
    }

    public void setStatusBreakdown(List<StatusCountVO> statusBreakdown) {
        this.statusBreakdown = statusBreakdown;
    }
}
