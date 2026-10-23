package com.ruoyi.wash.member.dto;

/**
 * 协议富文本。契约：openapi.yaml GET /api/v1/auth/agreements。
 *
 * <p>content 为富文本 HTML；未配置时为空串（合规文案不内置假文本），
 * 由运营在后台「系统管理 → 参数设置」填充后即时生效。
 */
public class AgreementVO {

    /** 协议标题 */
    private String title;

    /** 协议正文（富文本 HTML） */
    private String content;

    public AgreementVO() {
    }

    public AgreementVO(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
