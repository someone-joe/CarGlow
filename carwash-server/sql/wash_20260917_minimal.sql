-- =============================================================
-- 编码防复发：本文件为 UTF-8，导入前必须让客户端知晓。
-- mysql 客户端默认 latin1（MySQL 的 latin1 实为 cp1252 超集），
-- 不显式声明会把 UTF-8 字节按单字节解释、再以 utf8mb4 存库，造成双重编码乱码。
-- =============================================================
set names utf8mb4;

-- =============================================================
-- 洗车业务建表（最小链路第一批：会员 + 订单主表）
-- 规范（技术方案 4.6 / CODEBUDDY 红线）：
--   1. 所有业务表带 site_id / community_id（wash_member 为全局用户故不带）
--   2. 审计字段 create_by/create_time/update_by/update_time + del_flag 逻辑删除
--   3. 金额单位一律分（bigint），时间一律毫秒时间戳（bigint）
-- =============================================================

-- ----------------------------
-- C 端会员表
-- ----------------------------
drop table if exists wash_member;
create table wash_member (
    member_id      bigint(20)    not null auto_increment  comment '会员ID',
    openid         varchar(64)   not null                 comment '微信openid（小程序维度）',
    unionid        varchar(64)   default null             comment '微信unionid',
    nickname       varchar(64)   default null             comment '昵称',
    phone          varchar(20)   default null             comment '手机号（P8合规：V1.0暂存明文，后续改造为加密存储）',
    del_flag       char(1)       default '0'              comment '删除标志（0存在 1删除）',
    create_by      varchar(64)   default ''               comment '创建者',
    create_time    datetime      default null             comment '创建时间',
    update_by      varchar(64)   default ''               comment '更新者',
    update_time    datetime      default null             comment '更新时间',
    primary key (member_id),
    unique key uk_wash_member_openid (openid)
) engine=innodb auto_increment=1 default charset=utf8mb4 comment='C端会员表';

-- ----------------------------
-- 订单主表（最小列集，扩展只加不改）
-- ----------------------------
drop table if exists wash_order;
create table wash_order (
    order_id       bigint(20)    not null auto_increment  comment '订单ID',
    order_no       varchar(32)   not null                 comment '订单号（对外展示/回调定位）',
    member_id      bigint(20)    not null                 comment '会员ID',
    site_id        bigint(20)    not null                 comment '站点ID',
    community_id   bigint(20)    not null                 comment '小区ID',
    status         varchar(20)   not null                 comment '订单状态（取值见订单状态机规则表，15态）',
    service_name   varchar(64)   not null                 comment '服务项名称（下单时快照）',
    appoint_time   bigint(20)    not null                 comment '预约时间（毫秒时间戳）',
    pay_amount     bigint(20)    default 0                comment '实付金额（分）',
    plate_no       varchar(16)   default null             comment '车牌号',
    cancel_reason  varchar(255)  default null             comment '取消原因',
    del_flag       char(1)       default '0'              comment '删除标志（0存在 1删除）',
    create_by      varchar(64)   default ''               comment '创建者',
    create_time    datetime      default null             comment '创建时间',
    update_by      varchar(64)   default ''               comment '更新者',
    update_time    datetime      default null             comment '更新时间',
    primary key (order_id),
    unique key uk_wash_order_no (order_no),
    key idx_wash_order_member (member_id, status),
    key idx_wash_order_site (site_id, create_time)
) engine=innodb auto_increment=1 default charset=utf8mb4 comment='订单主表';
