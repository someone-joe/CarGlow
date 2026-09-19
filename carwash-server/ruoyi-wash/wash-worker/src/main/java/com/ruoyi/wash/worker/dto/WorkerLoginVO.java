package com.ruoyi.wash.worker.dto;

/**
 * 师傅工号登录返回 —— 与 openapi.yaml 的 WorkerLoginVO 逐字段一致，禁止另立字段名。
 */
public class WorkerLoginVO {

    private String token;
    private Long expireAt;
    private Long workerId;
    private String workerNo;
    private String name;
    private Long siteId;
    /** 站点名需查 wash_site（wash-network），M1 先置空，接入后填充 */
    private String siteName;
    private String status;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Long expireAt) {
        this.expireAt = expireAt;
    }

    public Long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
    }

    public String getWorkerNo() {
        return workerNo;
    }

    public void setWorkerNo(String workerNo) {
        this.workerNo = workerNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
