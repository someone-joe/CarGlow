package com.ruoyi.wash.notify.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 消息推送记录。表 wash_notify_record。
 *
 * <p>用途：幂等（bizNo 唯一）、月度限额统计、发送审计。
 */
public class WashNotifyRecord extends BaseEntity {

    private Long recordId;
    private Long memberId;
    private String openid;
    /** NotifyTemplate 名称 */
    private String templateKey;
    /** 实际使用的微信模板ID（mock 时可能为空） */
    private String templateId;
    /** 业务幂等号，同一 bizNo 只发一次 */
    private String bizNo;
    /** 推送内容摘要 */
    private String content;
    /** SENT 已发送 / SKIPPED 跳过 / FAILED 失败 */
    private String status;
    private String failReason;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

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

    public String getTemplateKey() {
        return templateKey;
    }

    public void setTemplateKey(String templateKey) {
        this.templateKey = templateKey;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }
}
