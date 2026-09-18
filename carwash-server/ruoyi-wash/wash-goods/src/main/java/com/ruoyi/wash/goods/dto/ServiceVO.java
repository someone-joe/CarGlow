package com.ruoyi.wash.goods.dto;

/**
 * 服务项（C 端）。契约：openapi.yaml /api/v1/services 的 ServiceVO。
 *
 * <p>金额一律分。displayPrice 由后端算好（契约要求），当前没有会员价/活动价体系，
 * 直接取 price_amount；将来接了权益再按「会员价 > 活动价 > 原价」的优先级在这里覆盖。
 *
 * <p>categoryId / subtitle / originPrice / salePrice / memberPrice 目前为 null：
 * 服务分类表与营销价体系未开工，表上也无对应列，不臆造数据。
 */
public class ServiceVO {

    private Long serviceId;
    private Long categoryId;
    private String name;
    private String subtitle;
    private Long originPrice;
    private Long salePrice;
    private Long memberPrice;
    private Long displayPrice;
    private Integer workMinutes;
    private Long pickupFee;

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

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

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public Long getOriginPrice() {
        return originPrice;
    }

    public void setOriginPrice(Long originPrice) {
        this.originPrice = originPrice;
    }

    public Long getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(Long salePrice) {
        this.salePrice = salePrice;
    }

    public Long getMemberPrice() {
        return memberPrice;
    }

    public void setMemberPrice(Long memberPrice) {
        this.memberPrice = memberPrice;
    }

    public Long getDisplayPrice() {
        return displayPrice;
    }

    public void setDisplayPrice(Long displayPrice) {
        this.displayPrice = displayPrice;
    }

    public Integer getWorkMinutes() {
        return workMinutes;
    }

    public void setWorkMinutes(Integer workMinutes) {
        this.workMinutes = workMinutes;
    }

    public Long getPickupFee() {
        return pickupFee;
    }

    public void setPickupFee(Long pickupFee) {
        this.pickupFee = pickupFee;
    }
}
