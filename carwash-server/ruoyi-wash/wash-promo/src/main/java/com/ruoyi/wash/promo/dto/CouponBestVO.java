package com.ruoyi.wash.promo.dto;

/** 给定金额下可用的最优券。discountAmount 为可减免金额（分）。 */
public record CouponBestVO(Long couponUserId, Long discountAmount) {
}
