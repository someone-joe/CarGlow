package com.ruoyi.wash.goods.dto;

/** 服务分类。契约：openapi.yaml /api/v1/service-categories 的 ServiceCategoryVO。 */
public class ServiceCategoryVO {

    private Long categoryId;
    private String name;
    private Integer sort;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
