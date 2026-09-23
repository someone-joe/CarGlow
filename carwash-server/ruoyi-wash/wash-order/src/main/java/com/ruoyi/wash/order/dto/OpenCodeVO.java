package com.ruoyi.wash.order.dto;

/** 开箱码 —— 与 openapi.yaml 的 OpenCodeVO 逐字段一致。 */
public class OpenCodeVO {

    private String code;
    private String qrcodeUrl;
    private String cabinetName;
    private String slotNo;
    /** 开箱码失效时间（毫秒时间戳），前端据此倒计时 */
    private Long expireAt;

    public Long getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Long expireAt) {
        this.expireAt = expireAt;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getQrcodeUrl() {
        return qrcodeUrl;
    }

    public void setQrcodeUrl(String qrcodeUrl) {
        this.qrcodeUrl = qrcodeUrl;
    }

    public String getCabinetName() {
        return cabinetName;
    }

    public void setCabinetName(String cabinetName) {
        this.cabinetName = cabinetName;
    }

    public String getSlotNo() {
        return slotNo;
    }

    public void setSlotNo(String slotNo) {
        this.slotNo = slotNo;
    }
}
