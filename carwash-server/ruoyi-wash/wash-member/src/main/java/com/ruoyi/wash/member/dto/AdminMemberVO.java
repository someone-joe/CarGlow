package com.ruoyi.wash.member.dto;

/**
 * 后台会员列表行。
 *
 * <p>合规（技术方案 P8 个保法最小化）：后台只返回**脱敏后**的手机号与 openid，
 * 不返回明文。目前连后台都不暴露明文，最大限度降低泄露面。
 */
public class AdminMemberVO {

    private Long memberId;
    private String openidMasked;
    private String nickname;
    private String phoneMasked;
    private Integer vehicleCount;
    private Integer orderCount;
    private String createTime;

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getOpenidMasked() {
        return openidMasked;
    }

    public void setOpenidMasked(String openidMasked) {
        this.openidMasked = openidMasked;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhoneMasked() {
        return phoneMasked;
    }

    public void setPhoneMasked(String phoneMasked) {
        this.phoneMasked = phoneMasked;
    }

    public Integer getVehicleCount() {
        return vehicleCount;
    }

    public void setVehicleCount(Integer vehicleCount) {
        this.vehicleCount = vehicleCount;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
