package com.ruoyi.wash.selfwash.dto;

/**
 * 自助工位排队明细项（C 端，排队进度弹窗用）。
 *
 * <p>status：SERVING 服务中 / QUEUING 排队中。
 * estimatedMinutes：服务中为剩余分钟，排队中为总时长（付费时长+加时），向上取整。
 */
public class WorkstationQueueItemVO {
    private String plateNo;
    private String serviceName;
    private String status;
    private Integer estimatedMinutes;

    public WorkstationQueueItemVO() {
    }

    public WorkstationQueueItemVO(String plateNo, String serviceName, String status, Integer estimatedMinutes) {
        this.plateNo = plateNo;
        this.serviceName = serviceName;
        this.status = status;
        this.estimatedMinutes = estimatedMinutes;
    }

    public String getPlateNo() {
        return plateNo;
    }

    public void setPlateNo(String plateNo) {
        this.plateNo = plateNo;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }
}
