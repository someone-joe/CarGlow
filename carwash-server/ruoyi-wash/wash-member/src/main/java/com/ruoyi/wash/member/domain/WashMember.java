package com.ruoyi.wash.member.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * C 端会员。表 wash_member。
 * 手机号合规（P8）：V1.0 暂存明文，改造为加密存储时只动本域，不影响其他模块。
 */
public class WashMember extends BaseEntity {

    private Long memberId;
    private String openid;
    private String unionid;
    private String nickname;
    private String phone;

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getUnionid() {
        return unionid;
    }

    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
