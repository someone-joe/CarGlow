-- =============================================================
-- 自助洗车订单（T9 Stage2：软件闭环）
-- 与无感 wash_order 完全独立。计时计费：SERVING 时 started_at + (service_minutes+added_seconds) 分钟 = expire_at。
-- 纯 C 端自我管理，不涉后台菜单（后台自助订单监控可后续按需加）。
-- =============================================================

create table if not exists wash_selfwash_order (
  order_id        bigint       not null auto_increment comment '主键',
  order_no        varchar(32)  not null comment '订单号',
  member_id       bigint       not null comment '会员ID',
  site_id         bigint       not null comment '站点ID',
  workstation_id  bigint       not null comment '工位ID',
  service_id      bigint       not null comment '服务ID',
  service_name    varchar(128) default null comment '服务名称快照',
  service_minutes int          not null default 0 comment '付费时长(分钟)',
  pay_amount      bigint       not null comment '实付金额(分)',
  pay_channel     varchar(16)  default null comment 'BALANCE/WECHAT_MOCK',
  status          varchar(16)  not null comment 'SelfWashStatus',
  queue_no        int          default null comment '排队序号(QUEUING)',
  started_at      bigint       default null comment '服务开始(ms)',
  expire_at       bigint       default null comment '服务截止(ms)',
  added_seconds   int          not null default 0 comment '累计加时(秒)',
  pay_expire_at   bigint       default null comment '支付截止(ms)',
  cancel_reason   varchar(255) default null comment '取消原因',
  del_flag        char(1)      not null default '0' comment '删除标记',
  create_time     datetime     default null comment '创建时间',
  update_time     datetime     default null comment '更新时间',
  primary key (order_id),
  unique key uk_sw_order_no (order_no),
  key idx_sw_member (member_id, status, order_id),
  key idx_sw_ws (workstation_id, status),
  key idx_sw_expire (status, expire_at)
) engine=innodb default charset=utf8mb4 comment='自助洗车订单';
