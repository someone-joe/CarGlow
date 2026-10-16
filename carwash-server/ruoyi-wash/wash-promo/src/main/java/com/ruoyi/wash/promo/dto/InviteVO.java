package com.ruoyi.wash.promo.dto;

import java.util.Date;

/** 我邀请的人（C 端）。 */
public class InviteVO {
    private Long inviteId;
    private Long inviteeMemberId;
    private String status;
    private String statusLabel;
    private String firstOrderNo;
    private Date createTime;

    public Long getInviteId() {
        return inviteId;
    }

    public void setInviteId(Long inviteId) {
        this.inviteId = inviteId;
    }

    public Long getInviteeMemberId() {
        return inviteeMemberId;
    }

    public void setInviteeMemberId(Long inviteeMemberId) {
        this.inviteeMemberId = inviteeMemberId;
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

    public String getFirstOrderNo() {
        return firstOrderNo;
    }

    public void setFirstOrderNo(String firstOrderNo) {
        this.firstOrderNo = firstOrderNo;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
