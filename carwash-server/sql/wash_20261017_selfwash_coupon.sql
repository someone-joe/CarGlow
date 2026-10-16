-- =============================================================
-- 自助洗车订单接入优惠券（T9 支付渠道补全：储值/微信/优惠券）
-- 与无感一致：下单即核销券并抵减实付；取消不退券（已知取舍，同无感）。
-- =============================================================

alter table wash_selfwash_order
  add column coupon_user_id bigint default null comment '使用的优惠券ID' after pay_channel,
  add column coupon_discount bigint not null default 0 comment '优惠券抵扣金额(分)' after coupon_user_id;
