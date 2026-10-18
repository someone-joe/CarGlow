package com.ruoyi.wash.selfwash.dto;

import java.util.List;

/**
 * 自助工位（C 端，实时）。
 *
 * <p>status：IDLE 空闲 / SERVING 服务中 / DISABLED 停用。
 * remainingSeconds：服务中剩余秒；queueCount：排队人数；
 * currentPlate：当前服务车牌；aheadPlates：前方排队车牌（按排队顺序）。
 */
public class WorkstationVO {
    private Long workstationId;
    private Long siteId;
    private String code;
    private String name;
    private String status;
    private String enabled;
    private Long remainingSeconds;
    private Integer queueCount;
    /** 预计等待分钟：服务中剩余 + 各排队单总时长（含加时），向上取整 */
    private Integer estimatedWaitMinutes;
    /** 服务中+排队中订单明细（服务中在前），排队进度弹窗用；车牌脱敏下发 */
    private List<WorkstationQueueItemVO> queueItems;

    public WorkstationVO() {
    }

    public WorkstationVO(Long workstationId, Long siteId, String code, String name, String status,
                         String enabled, Long remainingSeconds, Integer queueCount) {
        this.workstationId = workstationId;
        this.siteId = siteId;
        this.code = code;
        this.name = name;
        this.status = status;
        this.enabled = enabled;
        this.remainingSeconds = remainingSeconds;
        this.queueCount = queueCount;
    }

    public Long getWorkstationId() {
        return workstationId;
    }

    public void setWorkstationId(Long workstationId) {
        this.workstationId = workstationId;
    }

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public Integer getQueueCount() {
        return queueCount;
    }

    public void setQueueCount(Integer queueCount) {
        this.queueCount = queueCount;
    }

    public Integer getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public void setEstimatedWaitMinutes(Integer estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public List<WorkstationQueueItemVO> getQueueItems() {
        return queueItems;
    }

    public void setQueueItems(List<WorkstationQueueItemVO> queueItems) {
        this.queueItems = queueItems;
    }
}
