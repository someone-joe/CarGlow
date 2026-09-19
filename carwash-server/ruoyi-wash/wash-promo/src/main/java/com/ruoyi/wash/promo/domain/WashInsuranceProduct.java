package com.ruoyi.wash.promo.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/** 保险产品（保障方案）。表 wash_insurance_product。保费单位：分。 */
public class WashInsuranceProduct extends BaseEntity {

    private Long insuranceProductId;
    private String name;
    /** 保费（分） */
    private Long priceAmount;
    /** 保障说明 */
    private String coverageDesc;
    /** Y 上架 / N 下架 */
    private String status;

    public Long getInsuranceProductId() {
        return insuranceProductId;
    }

    public void setInsuranceProductId(Long insuranceProductId) {
        this.insuranceProductId = insuranceProductId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getPriceAmount() {
        return priceAmount;
    }

    public void setPriceAmount(Long priceAmount) {
        this.priceAmount = priceAmount;
    }

    public String getCoverageDesc() {
        return coverageDesc;
    }

    public void setCoverageDesc(String coverageDesc) {
        this.coverageDesc = coverageDesc;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
