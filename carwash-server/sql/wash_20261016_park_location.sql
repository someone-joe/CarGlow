-- =============================================================
-- 会员常用停车位置（需求 10.3 常用停车位置管理 / 3.2.4 填写停车位置）
-- 成员自有数据，归属 wash-member（与车辆同域）。无感下单时选一个已存位置，避免每次重填。
-- 纯 C 端自我管理，不涉及后台，故不注册 sys_menu。
-- =============================================================

create table if not exists wash_park_location (
  location_id    bigint       not null auto_increment comment '主键',
  member_id      bigint       not null comment '会员ID',
  community_id   bigint       not null comment '所在小区ID',
  community_name varchar(128) default null comment '所在小区名称（快照）',
  building       varchar(64)  default null comment '楼栋/单元号',
  parking_no     varchar(64)  default null comment '车位号，如 B2区A柱旁',
  detail         varchar(255) default null comment '具体位置补充',
  is_default     char(1)      not null default 'N' comment '默认位置 Y/N',
  del_flag       char(1)      not null default '0' comment '删除标记（0正常 2删除）',
  create_time    datetime     default null comment '创建时间',
  update_time    datetime     default null comment '更新时间',
  primary key (location_id),
  key idx_park_member (member_id, is_default, create_time)
) engine=innodb default charset=utf8mb4 comment='会员常用停车位置';
