package com.ruoyi.wash.member.dto;

/** 客服常见问题条目。问题必填；解答可空，空则 C 端弹通用占位。 */
public class CsFaq {

    private String question;
    private String answer;

    public CsFaq() {
    }

    public CsFaq(String question, String answer) {
        this.question = question;
        this.answer = answer;
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
}
