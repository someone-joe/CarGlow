package com.ruoyi.wash.order.dto;

/**
 * 后台看板状态分布项。
 *
 * <p>status 为枚举名、label 为枚举自带中文名，前端不得自己维护一份状态名单（唯一真源是 OrderStatus）。
 */
public class StatusCountVO {

    private String status;
    private String label;
    private long count;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
