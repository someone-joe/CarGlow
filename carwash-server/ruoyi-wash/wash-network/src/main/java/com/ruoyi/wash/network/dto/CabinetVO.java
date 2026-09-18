package com.ruoyi.wash.network.dto;

/**
 * 机柜（C 端）。契约：openapi.yaml /api/v1/cabinets 的 CabinetVO。
 *
 * <p>distance 目前为 null：需要用户授权定位 + 机柜坐标，均未开工，契约允许为空。
 * online 目前等价于「机柜启用」，真正的在线状态要等柜机心跳上报。
 */
public class CabinetVO {

    private Long cabinetId;
    private String name;
    private Integer slotTotal;
    private Integer slotFree;
    private Boolean online;
    private Boolean full;
    private Integer distance;

    public Long getCabinetId() {
        return cabinetId;
    }

    public void setCabinetId(Long cabinetId) {
        this.cabinetId = cabinetId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSlotTotal() {
        return slotTotal;
    }

    public void setSlotTotal(Integer slotTotal) {
        this.slotTotal = slotTotal;
    }

    public Integer getSlotFree() {
        return slotFree;
    }

    public void setSlotFree(Integer slotFree) {
        this.slotFree = slotFree;
    }

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public Boolean getFull() {
        return full;
    }

    public void setFull(Boolean full) {
        this.full = full;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }
}
