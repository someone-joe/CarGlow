package com.ruoyi.wash.member.dto;

/**
 * 客服配置。契约：openapi.yaml GET /api/v1/config/customer-service。
 *
 * <p>取值全部来自配置（application.yml 的 wash.customer-service），
 * 前端不得硬编码电话号码或文案；未配置的字段返回空串，前端自行决定展示。
 */
public class CustomerServiceVO {

    private String wecomQrcodeUrl;
    private String platformPhone;
    private String stationPhone;
    private String nightTip;

    public String getWecomQrcodeUrl() {
        return wecomQrcodeUrl;
    }

    public void setWecomQrcodeUrl(String wecomQrcodeUrl) {
        this.wecomQrcodeUrl = wecomQrcodeUrl;
    }

    public String getPlatformPhone() {
        return platformPhone;
    }

    public void setPlatformPhone(String platformPhone) {
        this.platformPhone = platformPhone;
    }

    public String getStationPhone() {
        return stationPhone;
    }

    public void setStationPhone(String stationPhone) {
        this.stationPhone = stationPhone;
    }

    public String getNightTip() {
        return nightTip;
    }

    public void setNightTip(String nightTip) {
        this.nightTip = nightTip;
    }
}
