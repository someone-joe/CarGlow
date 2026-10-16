package com.ruoyi.wash.promo.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 邀请关系。表 wash_invite。
 *
 * <p>status：BOUND 已绑定待首单 / REWARDED 已发奖（被邀请人首次成单后给邀请人发奖励券）。
 * 一个被邀请人只有一条记录（唯一约束），防止重复绑定与重复发奖。
 */
public class WashInvite extends BaseEntity {

    private Long inviteId;
    private Long inviterMemberId;
    private Long inviteeMemberId;
    /** 被邀请人首次成单的订单号 */
    private String firstOrderNo;
    /** 发给邀请人的奖励券 */
    private Long rewardCouponUserId;
    private String status;

    public Long getInviteId() {
        return inviteId;
    }

    public void setInviteId(Long inviteId) {
        this.inviteId = inviteId;
    }

    public Long getInviterMemberId() {
        return inviterMemberId;
    }

    public void setInviterMemberId(Long inviterMemberId) {
        this.inviterMemberId = inviterMemberId;
    }

    public Long getInviteeMemberId() {
        return inviteeMemberId;
    }

    public void setInviteeMemberId(Long inviteeMemberId) {
        this.inviteeMemberId = inviteeMemberId;
    }

    public String getFirstOrderNo() {
        return firstOrderNo;
    }

    public void setFirstOrderNo(String firstOrderNo) {
        this.firstOrderNo = firstOrderNo;
    }

    public Long getRewardCouponUserId() {
        return rewardCouponUserId;
    }

    public void setRewardCouponUserId(Long rewardCouponUserId) {
        this.rewardCouponUserId = rewardCouponUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
