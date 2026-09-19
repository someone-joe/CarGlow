package com.ruoyi.wash.goods.domain;

/** 服务分类。表 wash_service_category。 */
public class WashServiceCategory {

    private Long categoryId;
    private String categoryName;
    private Integer sortNum;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getSortNum() {
        return sortNum;
    }

    public void setSortNum(Integer sortNum) {
        this.sortNum = sortNum;
    }
}
