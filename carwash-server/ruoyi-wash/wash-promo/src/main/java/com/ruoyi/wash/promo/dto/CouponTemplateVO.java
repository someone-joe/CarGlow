package com.ruoyi.wash.promo.dto;

/** 优惠券模板（领券中心展示）。remain=剩余库存，claimed=当前会员已领数量。 */
public record CouponTemplateVO(Long templateId, String name, String type,
                               Long thresholdAmount, Long discountAmount, Integer perLimit,
                               Long endTime, Integer remain, Integer claimed) {
}
