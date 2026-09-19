package com.ruoyi.wash.promo.dto;

/** 用户持有的优惠券。 */
public record CouponUserVO(Long couponUserId, Long templateId, String name,
                           Long thresholdAmount, Long discountAmount, String status,
                           Long expireTime, String orderNo) {
}
