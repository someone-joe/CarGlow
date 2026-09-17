package com.ruoyi.wash.order.dto;

import java.util.List;

/**
 * C 端时间轴节点 —— 与 openapi.yaml 的 TimelineNode 逐字段一致。
 *
 * <p>8 个节点的取值与含义见《订单状态机规则表.md》第三节，
 * 时间轴**直接读流转日志**生成，不另写一套进度（规则表第七条）。
 */
public class TimelineNodeVO {

    /** 见契约 enum：ORDERED / KEY_IN / PICKED / ARRIVED / WASHING / QC_DONE / RETURNED / WAIT_TAKE_KEY */
    private String node;
    private String title;
    /** 毫秒时间戳，未到达时为空 */
    private Long time;
    private Boolean reached;
    private Boolean current;
    private List<MediaVO> evidences;

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Boolean getReached() {
        return reached;
    }

    public void setReached(Boolean reached) {
        this.reached = reached;
    }

    public Boolean getCurrent() {
        return current;
    }

    public void setCurrent(Boolean current) {
        this.current = current;
    }

    public List<MediaVO> getEvidences() {
        return evidences;
    }

    public void setEvidences(List<MediaVO> evidences) {
        this.evidences = evidences;
    }
}
