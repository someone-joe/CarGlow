-- =============================================================
-- 储值钱包（需求 T1/T2）
-- 钱包余额（充值本金 + 赠送）+ 充值单 + 流水 + 充值赠送规则。
-- 归属 wash-pay 模块域：充值是支付行为、余额抵扣是支付渠道，避免新增模块与 wash-pay 成环。
-- 本期不支持提现。扣款顺序：先赠后本。
-- =============================================================

create table if not exists wash_member_wallet (
  wallet_id      bigint    not null auto_increment comment '主键',
  member_id      bigint    not null comment '会员ID',
  balance        bigint    not null default 0 comment '充值本金余额（分）',
  gift_balance   bigint    not null default 0 comment '赠送余额（分）',
  total_recharge bigint    not null default 0 comment '累计充值本金（分，统计用）',
  version        int       not null default 0 comment '乐观锁版本',
  create_time    datetime  default null comment '创建时间',
  update_time    datetime  default null comment '更新时间',
  primary key (wallet_id),
  unique key uk_wallet_member (member_id)
) engine=innodb default charset=utf8mb4 comment='会员储值钱包';

create table if not exists wash_wallet_recharge (
  recharge_id       bigint       not null auto_increment comment '主键',
  recharge_no       varchar(32)  not null comment '充值单号',
  member_id         bigint       not null comment '会员ID',
  rule_id           bigint       default null comment '赠送规则ID，自定义金额为空',
  pay_amount        bigint       not null comment '实付金额（分）',
  gift_amount       bigint       not null default 0 comment '赠送金额（分）',
  channel           varchar(16)  not null default 'MOCK' comment '支付渠道（MOCK，微信后补）',
  status            varchar(16)  not null default 'INIT' comment 'INIT/SUCCESS/FAILED',
  provider_trade_no varchar(64)  default null comment '渠道流水号',
  pay_expire_at     bigint       default null comment '支付截止(ms)',
  create_time       datetime     default null comment '创建时间',
  update_time       datetime     default null comment '更新时间',
  primary key (recharge_id),
  unique key uk_recharge_no (recharge_no),
  key idx_recharge_member (member_id, create_time)
) engine=innodb default charset=utf8mb4 comment='储值充值单';

create table if not exists wash_wallet_flow (
  flow_id       bigint       not null auto_increment comment '主键',
  flow_no       varchar(32)  not null comment '流水号',
  member_id     bigint       not null comment '会员ID',
  biz_type      varchar(16)  not null comment 'RECHARGE/GIFT/PAY/REFUND',
  change_amount bigint       not null default 0 comment '本金变动（分，正入负出）',
  gift_change   bigint       not null default 0 comment '赠送变动（分，正入负出）',
  balance_after bigint       not null default 0 comment '变动后本金（分）',
  gift_after    bigint       not null default 0 comment '变动后赠送（分）',
  biz_no        varchar(64)  default null comment '关联单号（recharge_no/order_no）',
  remark        varchar(255) default null comment '备注',
  create_time   datetime     default null comment '创建时间',
  primary key (flow_id),
  unique key uk_flow_no (flow_no),
  key idx_flow_member (member_id, create_time)
) engine=innodb default charset=utf8mb4 comment='钱包流水';

create table if not exists wash_wallet_gift_rule (
  rule_id      bigint    not null auto_increment comment '主键',
  recharge_fen bigint    not null comment '充值门槛（分），如 20000=200元',
  gift_fen     bigint    not null comment '赠送（分），如 2000=20元',
  enabled      char(1)   not null default 'Y' comment '是否启用',
  sort_order   int       not null default 0 comment '排序，升序',
  create_time  datetime  default null comment '创建时间',
  update_time  datetime  default null comment '更新时间',
  primary key (rule_id)
) engine=innodb default charset=utf8mb4 comment='充值赠送规则';

-- 种子赠送规则：充200送20 / 充300送50 / 充500送100（幂等）
insert into wash_wallet_gift_rule (recharge_fen, gift_fen, enabled, sort_order, create_time)
select recharge_fen, gift_fen, enabled, sort_order, sysdate() from (
  select 20000 as recharge_fen, 2000 as gift_fen, 'Y' as enabled, 1 as sort_order union all
  select 30000, 5000, 'Y', 2 union all
  select 50000, 10000, 'Y', 3
) as seed
where (select count(*) from wash_wallet_gift_rule) = 0;

-- ----------------------------
-- 运营后台菜单：钱包配置（2100 系列，父目录 2000 洗车业务）
-- ----------------------------
delete from sys_menu where menu_id in (2100, 2101, 2102);
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2100, '钱包配置', 2000, 10, 'wallet-config', 'wash/wallet-config/index', 1, 0, 'C', '0', '0', 'wash:wallet:list', 'money', 'admin', sysdate(), '储值充值赠送规则配置 + 充值流水查询');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2101, '赠送规则保存', 2100, 1, '', null, 1, 0, 'F', '0', '0', 'wash:wallet:edit', '#', 'admin', sysdate(), '保存充值赠送规则');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2102, '充值流水查询', 2100, 2, '', null, 1, 0, 'F', '0', '0', 'wash:wallet:recharge', '#', 'admin', sysdate(), '查看充值流水');

insert ignore into sys_role_menu (role_id, menu_id) values (1, 2100), (1, 2101), (1, 2102);
