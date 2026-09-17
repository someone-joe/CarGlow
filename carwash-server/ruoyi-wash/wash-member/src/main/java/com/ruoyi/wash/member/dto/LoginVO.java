package com.ruoyi.wash.member.dto;

/**
 * 登录返回 —— 与 openapi.yaml 的 LoginVO 逐字段一致，禁止另立字段名。
 */
public class LoginVO {

    private String token;
    private Long expireAt;
    private Boolean isNewUser;
    private Boolean needAgreement;
    private Boolean phoneBound;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Long expireAt) {
        this.expireAt = expireAt;
    }

    public Boolean getIsNewUser() {
        return isNewUser;
    }

    public void setIsNewUser(Boolean isNewUser) {
        this.isNewUser = isNewUser;
    }

    public Boolean getNeedAgreement() {
        return needAgreement;
    }

    public void setNeedAgreement(Boolean needAgreement) {
        this.needAgreement = needAgreement;
    }

    public Boolean getPhoneBound() {
        return phoneBound;
    }

    public void setPhoneBound(Boolean phoneBound) {
        this.phoneBound = phoneBound;
    }
}
