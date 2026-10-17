package com.ruoyi.wash.order.event;

/**
 * 钥匙已入柜事件（WAIT_KEY → KEY_IN）—— 派单的触发点。
 *
 * <p>wash-order 不依赖 wash-worker：这里只发事件，由 wash-worker 监听并执行自动派单
 * （挑「同小区、在线、资质通过」的师傅落 worker_id）。好处是派单规则怎么变，
 * 订单域代码都不用动；派单失败也只记日志，不影响钥匙入柜本身。
 *
 * @param orderNo     订单号
 * @param memberId    会员ID
 * @param communityId 小区ID（同小区合并派单的依据）
 * @param siteId      站点ID（派单的硬边界：师傅只看本站点）
 */
public record OrderKeyInEvent(String orderNo, Long memberId, Long communityId, Long siteId) {
}
