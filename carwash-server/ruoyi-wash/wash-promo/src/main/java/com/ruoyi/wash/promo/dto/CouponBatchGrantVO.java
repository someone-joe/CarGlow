package com.ruoyi.wash.promo.dto;

/** 批量发放结果汇总。 */
public record CouponBatchGrantVO(Long batchId, int totalCount, int successCount, int skippedCount, int failedCount) {
}
