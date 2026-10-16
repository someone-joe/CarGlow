package com.ruoyi.wash.selfwash.dto;

/** 自助工位（C 端）。status 为实时状态：IDLE 空闲 / SERVING 服务中 / DISABLED 停用。 */
public class WorkstationVO {
    private Long workstationId;
    private Long siteId;
    private String code;
    private String name;
    private String status;
    private String enabled;

    public WorkstationVO() {
    }

    public WorkstationVO(Long workstationId, Long siteId, String code, String name, String status, String enabled) {
        this.workstationId = workstationId;
        this.siteId = siteId;
        this.code = code;
        this.name = name;
        this.status = status;
        this.enabled = enabled;
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
}
