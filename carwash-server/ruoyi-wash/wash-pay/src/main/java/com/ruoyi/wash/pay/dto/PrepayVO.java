package com.ruoyi.wash.pay.dto;

/** 支付参数 —— 与 openapi.yaml /api/v1/payments/{orderNo}/prepay 的 data 逐字段一致。 */
public class PrepayVO {

    private String timeStamp;
    private String nonceStr;
    /** 注意：字段名叫 package（Java 关键字），JSON 序列化时由 Jackson 输出为 package */
    private String packageValue;
    private String signType;
    private String paySign;
    private Long expireAt;

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getNonceStr() {
        return nonceStr;
    }

    public void setNonceStr(String nonceStr) {
        this.nonceStr = nonceStr;
    }

    public String getPackageValue() {
        return packageValue;
    }

    public void setPackageValue(String packageValue) {
        this.packageValue = packageValue;
    }

    public String getSignType() {
        return signType;
    }

    public void setSignType(String signType) {
        this.signType = signType;
    }

    public String getPaySign() {
        return paySign;
    }

    public void setPaySign(String paySign) {
        this.paySign = paySign;
    }

    public Long getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Long expireAt) {
        this.expireAt = expireAt;
    }
}
