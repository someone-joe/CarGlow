package com.ruoyi.wash.promo.dto;

/**
 * 批量发放受众筛选。三者至少其一：
 * all=true 走全量；否则按 communityId / timeFrom~timeTo 过滤（小区与时间可叠加）。
 *
 * <p>小区口径：会员表无 community_id，关联取「常用停车位置 ∪ 车辆常停小区」，
 * 按 member_id 去重，不会因交集重复发。
 */
public record CouponGrantAudience(Long communityId, Long timeFrom, Long timeTo, Boolean all) {
}
