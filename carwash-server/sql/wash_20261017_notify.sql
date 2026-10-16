-- =============================================================
-- 消息通知推送记录（需求 11 骨架）—— 新模块 wash-notify
-- 用途：幂等(biz_no 唯一) / 月度限额统计 / 发送审计。
-- =============================================================

create table if not exists wash_notify_record (
  record_id    bigint       not null auto_increment comment '主键',
  member_id    bigint       not null comment '接收会员ID',
  openid       varchar(64)  default null comment '微信 openid',
  template_key varchar(32)  not null comment '模板枚举名(NotifyTemplate)',
  template_id  varchar(64)  default null comment '微信模板ID(mock时可能为空)',
  biz_no       varchar(64)  default null comment '业务幂等号，同一业务只发一次',
  content      varchar(1000) default null comment '推送内容摘要',
  status       varchar(16)  not null comment 'SENT/SKIPPED/FAILED',
  fail_reason  varchar(255) default null comment '跳过/失败原因',
  del_flag     char(1)      not null default '0' comment '删除标记',
  create_time  datetime     default null comment '创建时间',
  primary key (record_id),
  key idx_nr_member (member_id, template_key, create_time),
  key idx_nr_biz (biz_no)
) engine=innodb default charset=utf8mb4 comment='消息推送记录';
