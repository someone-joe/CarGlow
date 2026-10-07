package com.ruoyi.wash.worker.dto;

/**
 * 后台师傅列表行 / 详情，同时兼作「新增 / 修改」的入参。
 *
 * <p>为什么不直接用 WashWorker 实体：实体只有登录需要的字段（没有 create_time / remark），
 * 而后台列表要展示创建时间、表单要能填备注，补字段进实体会把登录链路也牵扯进来。
 * 与 AdminOrderVO 一致的做法——由 Mapper 直接查成 VO。
 *
 * <p><b>安全</b>：序列化出参时 {@code password} 必须是 null。
 * 它在VO里存在只为了接收「新增 / 重置密码」的入参，Service 负责查完就清空。
 */
public class AdminWorkerVO {

    private Long workerId;
    private String workerNo;
    private String password;
    private String name;
    private String phone;
    private Long siteId;
    /** 站点名由服务端关联查出，前端不做 id→名称的映射 */
    private String siteName;
    private String status;
    /** 中文状态名，后端枚举给出 */
    private String statusLabel;
    private String remark;
    private String createTime;

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

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
