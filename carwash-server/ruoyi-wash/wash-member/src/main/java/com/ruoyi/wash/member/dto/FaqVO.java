package com.ruoyi.wash.member.dto;

/**
 * 常见问题条目（C 端）。契约：openapi.yaml GET /api/v1/faqs → FaqVO。
 *
 * <p>数据来源 wash_cs_faq 表（后台「客服配置」页维护），与「客服配置」接口同一份数据，
 * 只是字段口径不同：本 VO 的 label 即表里的 question（FAQ 页展示为标签文案）。
 *
 * <p>ticketType（一键建单时带入的工单类型）当前库里没有对应列，返回 null——
 * 不臆造字段；待工单模块开工、表加列后再接。
 */
public class FaqVO {

    private Long faqId;
    private String label;
    private String answer;
    private String ticketType;

    public FaqVO() {
    }

    public FaqVO(Long faqId, String label, String answer, String ticketType) {
        this.faqId = faqId;
        this.label = label;
        this.answer = answer;
        this.ticketType = ticketType;
    }

    public Long getFaqId() {
        return faqId;
    }

    public void setFaqId(Long faqId) {
        this.faqId = faqId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getTicketType() {
        return ticketType;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }
}
