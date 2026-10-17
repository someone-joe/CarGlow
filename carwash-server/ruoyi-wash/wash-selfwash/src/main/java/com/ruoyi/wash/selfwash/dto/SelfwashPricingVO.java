package com.ruoyi.wash.selfwash.dto;

/**
 * 自助洗车计时计价参数（C 端 + 后台共用）。
 *
 * <p>金额单位分，时长单位分钟。默认值：20 元 / 30 分钟，超时 8 元 / 10 分钟。
 */
public record SelfwashPricingVO(Long baseAmount, Integer baseMinutes,
                                Long overtimeAmount, Integer overtimeMinutes) {
}
