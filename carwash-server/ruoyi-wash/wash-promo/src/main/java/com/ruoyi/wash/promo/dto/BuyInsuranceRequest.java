package com.ruoyi.wash.promo.dto;

/** 投保请求。orderNo 可空（不强制绑定订单）。 */
public record BuyInsuranceRequest(Long productId, Long vehicleId, String orderNo) {
}
