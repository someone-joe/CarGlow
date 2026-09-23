package com.ruoyi.wash.network.domain;

/** 站点（中央站）。表 wash_site。产能上限与承诺时间都在站点上配置，禁止代码里写死。 */
public class WashSite {

    private Long siteId;
    private String siteName;
    /** 单日产能上限，0 表示不限量 */
    private Integer dailyLimit;
    private String depositDeadline;
    private String promiseReturnTime;
    /** 夜间服务营业时段起止（整点，如 19:00 / 23:00），下单页可选时段由此生成 */
    private String businessStart;
    private String businessEnd;
    private String serviceStatus;
    private String closedNotice;

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public String getDepositDeadline() {
        return depositDeadline;
    }

    public void setDepositDeadline(String depositDeadline) {
        this.depositDeadline = depositDeadline;
    }

    public String getPromiseReturnTime() {
        return promiseReturnTime;
    }

    public void setPromiseReturnTime(String promiseReturnTime) {
        this.promiseReturnTime = promiseReturnTime;
    }

    public String getBusinessStart() {
        return businessStart;
    }

    public void setBusinessStart(String businessStart) {
        this.businessStart = businessStart;
    }

    public String getBusinessEnd() {
        return businessEnd;
    }

    public void setBusinessEnd(String businessEnd) {
        this.businessEnd = businessEnd;
    }

    public String getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(String serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    public String getClosedNotice() {
        return closedNotice;
    }

    public void setClosedNotice(String closedNotice) {
        this.closedNotice = closedNotice;
    }
}
