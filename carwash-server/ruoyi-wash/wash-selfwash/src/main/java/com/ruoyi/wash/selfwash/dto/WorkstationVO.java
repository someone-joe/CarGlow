package com.ruoyi.wash.selfwash.dto;

/**
 * 自助工位（C 端，实时）。
 *
 * <p>status：IDLE 空闲 / SERVING 服务中 / DISABLED 停用。
 * remainingSeconds：服务中时的剩余秒；queueCount：该工位当前排队人数。
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
}
