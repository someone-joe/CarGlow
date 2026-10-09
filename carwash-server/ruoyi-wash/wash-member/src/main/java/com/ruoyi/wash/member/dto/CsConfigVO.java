package com.ruoyi.wash.member.dto;

import java.util.List;

/**
 * 客服配置（后台「客服配置」页读写载体）。
 *
 * <p>标量字段（企微码、双电话、夜间提示）落 sys_config（key 见 AdminCsConfigController.CONFIG_KEY_*），
 * faqs 落 wash_cs_faq 表。前后端共用此结构：GET 返回当前配置，POST 接收保存后的配置。
 */
public class CsConfigVO {

    private String wecomQrcodeUrl;
    private String platformPhone;
    private String stationPhone;
    private String nightTip;
    private List<CsFaq> faqs;

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

    public List<CsFaq> getFaqs() {
        return faqs;
    }

    public void setFaqs(List<CsFaq> faqs) {
        this.faqs = faqs;
    }
}
