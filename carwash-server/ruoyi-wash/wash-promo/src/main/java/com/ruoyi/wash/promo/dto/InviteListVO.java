package com.ruoyi.wash.promo.dto;

import java.util.List;

/** 我的邀请列表 + 统计（C 端）。 */
public class InviteListVO {
    private List<InviteVO> list;
    private Integer invitedCount;
    private Integer rewardedCount;

    public List<InviteVO> getList() {
        return list;
    }

    public void setList(List<InviteVO> list) {
        this.list = list;
    }

    public Integer getInvitedCount() {
        return invitedCount;
    }

    public void setInvitedCount(Integer invitedCount) {
        this.invitedCount = invitedCount;
    }

    public Integer getRewardedCount() {
        return rewardedCount;
    }

    public void setRewardedCount(Integer rewardedCount) {
        this.rewardedCount = rewardedCount;
    }
}
