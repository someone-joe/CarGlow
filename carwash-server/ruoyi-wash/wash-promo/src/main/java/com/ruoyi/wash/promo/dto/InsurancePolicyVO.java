package com.ruoyi.wash.promo.dto;

/** 客户保单。 */
public record InsurancePolicyVO(Long policyId, String policyNo, Long productId, String name,
                                Long vehicleId, String orderNo, String status,
                                Long startTime, Long endTime, Long paidAmount) {
}
