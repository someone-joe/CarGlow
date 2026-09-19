package com.ruoyi.wash.promo.dto;

/** 可投保的保险产品。 */
public record InsuranceProductVO(Long productId, String name, Long priceAmount, String coverageDesc) {
}
