package com.ruoyi.wash.worker.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 师傅（工作人员）。表 wash_worker。
 *
 * <p>取送人员与中央站技师合并为单一师傅（见 OrderOperatorType.WORKER），
 * 动作差异由订单事件本身区分，不在这里拆角色。
 */
public class WashWorker extends BaseEntity {

    private Long workerId;
    private String workerNo;
    private String password;
    private String name;
    private String phone;
    private Long siteId;
    /** NORMAL 正常 / DISABLED 禁用 */
    private String status;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
