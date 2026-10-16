-- =============================================================
-- 邀请好友/推荐有礼（需求 8.2）—— 数据在 wash-promo，邀请码落在 wash_member
-- 规则：被邀请人绑定后得新人券(NEWBIE)作首单优惠；被邀请人首次成单后给邀请人发 INVITE 奖励券(10元)
-- =============================================================

alter table wash_member
  add column invite_code varchar(16) default null comment '邀请码(唯一，首次访问惰性生成)' after phone;
alter table wash_member
  add unique key uk_member_invite_code (invite_code);

create table if not exists wash_invite (
  invite_id            bigint      not null auto_increment comment '主键',
  inviter_member_id    bigint      not null comment '邀请人会员ID',
  invitee_member_id    bigint      not null comment '被邀请人会员ID',
  first_order_no       varchar(32) default null comment '被邀请人首次成单订单号',
  reward_coupon_user_id bigint     default null comment '发给邀请人的奖励券ID',
  status               varchar(16) not null default 'BOUND' comment 'BOUND待首单/REWARDED已发奖',
  del_flag             char(1)     not null default '0' comment '删除标记',
  create_time          datetime    default null comment '创建时间',
  update_time          datetime    default null comment '更新时间',
  primary key (invite_id),
  unique key uk_invite_invitee (invitee_member_id),
  key idx_invite_inviter (inviter_member_id, status)
) engine=innodb default charset=utf8mb4 comment='邀请关系';

-- 邀请人奖励券：无门槛减 10 元（1000 分），有效期 1 年
insert into wash_coupon_template (name, type, threshold_amount, discount_amount, total, issued,
                                  per_limit, start_time, end_time, status, create_time)
select '邀请好友奖励券', 'INVITE', 0, 1000, 100000, 0, 9999,
       unix_timestamp(now()) * 1000, unix_timestamp(date_add(now(), interval 365 day)) * 1000, 'Y', now()
from dual
where not exists (select 1 from wash_coupon_template where type = 'INVITE');
