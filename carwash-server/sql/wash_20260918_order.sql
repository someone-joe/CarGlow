-- =============================================================
-- 编码防复发：本文件为 UTF-8，导入前必须让客户端知晓。
-- mysql 客户端默认 latin1（MySQL 的 latin1 实为 cp1252 超集），
-- 不显式声明会把 UTF-8 字节按单字节解释、再以 utf8mb4 存库，造成双重编码乱码。
-- =============================================================
set names utf8mb4;

-- =============================================================
-- 下单链路第二批：订单扩展列 + 服务项 / 车辆 / 机柜 三张基础表
-- （服务项属 wash-goods、车辆属 wash-member、机柜属 wash-network，
--   表结构统一在此维护，模块只负责各自的 Mapper）
-- =============================================================

-- ----------------------------
-- 1. 订单表扩展列（下单需要留痕，扩展只加不改）
-- ----------------------------
alter table wash_order
    add column service_id    bigint(20)  default null comment '服务项ID（下单时快照到 service_name / pay_amount）' after order_no,
    add column vehicle_id    bigint(20)  default null comment '车辆ID' after service_id,
    add column cabinet_id    bigint(20)  default null comment '机柜ID（存钥匙的柜）' after vehicle_id,
    add column pay_expire_at bigint(20)  default null comment '支付截止时间（毫秒时间戳）' after pay_amount,
    add column remark        varchar(255) default null comment '用户备注（契约 CreateOrderRequest.remark）' after cancel_reason;

-- ----------------------------
-- 2. 服务项表（wash-goods）
-- ----------------------------
drop table if exists wash_service;
create table wash_service (
    service_id     bigint(20)   not null auto_increment comment '服务项ID',
    service_name   varchar(64)  not null                comment '服务项名称',
    price_amount   bigint(20)   not null default 0      comment '价格（分）',
    work_minutes   int(11)      not null default 0      comment '作业时长（分钟）',
    enabled        char(1)      not null default 'Y'    comment '是否上架（Y是 N否）',
    del_flag       char(1)      default '0'             comment '删除标志（0存在 1删除）',
    create_by      varchar(64)  default ''              comment '创建者',
    create_time    datetime     default null            comment '创建时间',
    update_by      varchar(64)  default ''              comment '更新者',
    update_time    datetime     default null            comment '更新时间',
    primary key (service_id)
) engine=innodb auto_increment=101 default charset=utf8mb4 comment='服务项表';

-- ----------------------------
-- 3. 车辆表（wash-member）
-- ----------------------------
drop table if exists wash_vehicle;
create table wash_vehicle (
    vehicle_id     bigint(20)   not null auto_increment comment '车辆ID',
    member_id      bigint(20)   not null                comment '会员ID',
    plate_no       varchar(16)  not null                comment '车牌号（支持新能源8位）',
    brand          varchar(64)  default null            comment '品牌型号',
    color          varchar(32)  default null            comment '颜色',
    is_new_energy  char(1)      default 'N'             comment '是否新能源（Y是 N否）',
    community_id   bigint(20)   default null            comment '常停小区ID',
    parking_no     varchar(64)  default null            comment '车位号，如 A区23号',
    del_flag       char(1)      default '0'             comment '删除标志（0存在 1删除）',
    create_by      varchar(64)  default ''              comment '创建者',
    create_time    datetime     default null            comment '创建时间',
    update_by      varchar(64)  default ''              comment '更新者',
    update_time    datetime     default null            comment '更新时间',
    primary key (vehicle_id),
    key idx_wash_vehicle_member (member_id)
) engine=innodb auto_increment=201 default charset=utf8mb4 comment='会员车辆表';

-- ----------------------------
-- 4. 机柜表（wash-network，最小列集；格口表待柜机模块开工再建）
-- ----------------------------
drop table if exists wash_cabinet;
create table wash_cabinet (
    cabinet_id     bigint(20)   not null auto_increment comment '机柜ID',
    site_id        bigint(20)   not null                comment '所属站点ID',
    community_id   bigint(20)   not null                comment '所在小区ID',
    cabinet_name   varchar(64)  not null                comment '机柜名称，如 3栋负一层柜',
    enabled        char(1)      not null default 'Y'    comment '是否启用（Y是 N否）',
    del_flag       char(1)      default '0'             comment '删除标志（0存在 1删除）',
    create_by      varchar(64)  default ''              comment '创建者',
    create_time    datetime     default null            comment '创建时间',
    update_by      varchar(64)  default ''              comment '更新者',
    update_time    datetime     default null            comment '更新时间',
    primary key (cabinet_id)
) engine=innodb auto_increment=301 default charset=utf8mb4 comment='钥匙柜表';

-- ----------------------------
-- 5. 本地开发种子数据（仅开发库使用，生产不要执行）
-- ----------------------------
insert into wash_service (service_id, service_name, price_amount, work_minutes, enabled, create_time)
values (101, '标准洗车（外观+内饰吸尘）', 3900, 45, 'Y', sysdate());

insert into wash_vehicle (vehicle_id, member_id, plate_no, brand, color, is_new_energy, community_id, parking_no, create_time)
values (201, 1, '粤A12345', '丰田 凯美瑞', '白色', 'N', 1, 'A区23号车位', sysdate());

insert into wash_cabinet (cabinet_id, site_id, community_id, cabinet_name, enabled, create_time)
values (301, 1, 1, '3栋负一层钥匙柜', 'Y', sysdate());
