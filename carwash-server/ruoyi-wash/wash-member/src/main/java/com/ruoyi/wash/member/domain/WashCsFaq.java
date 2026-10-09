package com.ruoyi.wash.member.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/** 客服常见问题（您可能会遇到的问题）。后台「客服配置」维护，C 端客服 tab 展示。 */
public class WashCsFaq extends BaseEntity {

    private Long faqId;
    private String question;
    private String answer;
    private Integer sortOrder;

    public Long getFaqId() {
        return faqId;
    }

    public void setFaqId(Long faqId) {
        this.faqId = faqId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
