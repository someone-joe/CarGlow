package com.ruoyi.wash.selfwash.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 自助洗车工位（洗车站内的一个自助工位，如 A1/A2/A3）。表 wash_workstation。
 *
 * <p>「空闲/服务中/排队」为实时状态，由自助订单推导，不落库；本表只存配置与启用开关。
 */
public class WashWorkstation extends BaseEntity {

    private Long workstationId;
    private Long siteId;
    /** 工位编号，如 A1 */
    private String code;
    private String name;
    /** 是否启用 Y/N */
    private String enabled;
    private Integer sortOrder;

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

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
