package com.ruoyi.wash.worker.dto;

import java.util.List;

/**
 * 中央站工位队列看板 —— 与 openapi.yaml 的 StationQueueVO 逐字段一致。
 *
 * <p>工位（bay）表尚未接入，bayNo 先留空、sopStep 无来源留空；
 * 当前 bays 用「清洗中」的订单填充，waiting 用「待质检」的订单填充。
 */
public class StationQueueVO {

    private Long stationId;
    private String stationName;
    private Integer washingCount;
    private Integer qcCount;
    private List<Bay> bays;
    private List<Waiting> waiting;

    public Long getStationId() {
        return stationId;
    }

    public void setStationId(Long stationId) {
        this.stationId = stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public Integer getWashingCount() {
        return washingCount;
    }

    public void setWashingCount(Integer washingCount) {
        this.washingCount = washingCount;
    }

    public Integer getQcCount() {
        return qcCount;
    }

    public void setQcCount(Integer qcCount) {
        this.qcCount = qcCount;
    }

    public List<Bay> getBays() {
        return bays;
    }

    public void setBays(List<Bay> bays) {
        this.bays = bays;
    }

    public List<Waiting> getWaiting() {
        return waiting;
    }

    public void setWaiting(List<Waiting> waiting) {
        this.waiting = waiting;
    }

    /** 工位（bay）项 */
    public static class Bay {

        private String bayNo;
        private String orderNo;
        private String status;
        private Long startedAt;
        private Integer sopStep;

        public String getBayNo() {
            return bayNo;
        }

        public void setBayNo(String bayNo) {
            this.bayNo = bayNo;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(Long startedAt) {
            this.startedAt = startedAt;
        }

        public Integer getSopStep() {
            return sopStep;
        }

        public void setSopStep(Integer sopStep) {
            this.sopStep = sopStep;
        }
    }

    /** 非「在洗」的订单：待入场 / 待质检 / 待还车，status 决定前端给哪个动作 */
    public static class Waiting {

        private String orderNo;
        private String serviceName;
        private String status;
        private Long queuedAt;

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
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

        public Long getQueuedAt() {
            return queuedAt;
        }

        public void setQueuedAt(Long queuedAt) {
            this.queuedAt = queuedAt;
        }
    }
}
