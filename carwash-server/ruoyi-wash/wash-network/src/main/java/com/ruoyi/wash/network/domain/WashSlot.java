package com.ruoyi.wash.network.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/** 钥匙柜格口。表 wash_slot。状态取值：FREE / RESERVED / OCCUPIED / DISABLED。 */
public class WashSlot extends BaseEntity {

    private Long slotId;
    private Long cabinetId;
    private String slotNo;
    private String status;
    private String orderNo;
    private Long memberId;

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public Long getCabinetId() {
        return cabinetId;
    }

    public void setCabinetId(Long cabinetId) {
        this.cabinetId = cabinetId;
    }

    public String getSlotNo() {
        return slotNo;
    }

    public void setSlotNo(String slotNo) {
        this.slotNo = slotNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
}
