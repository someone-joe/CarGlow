-- =============================================================
-- 格口（钥匙柜的格子）与开箱留痕
-- 补齐 PRD 要求的"下单即预占格口、取消/取回即释放"，以及开箱操作留痕
-- =============================================================

drop table if exists wash_slot;
create table wash_slot (
    slot_id      bigint(20)  not null auto_increment comment '格口ID',
    cabinet_id   bigint(20)  not null                comment '所属机柜ID',
    slot_no      varchar(16) not null                comment '格口号（柜内编号）',
    status       varchar(16) not null default 'FREE' comment 'FREE空闲 / RESERVED已预占 / OCCUPIED占用中 / DISABLED停用',
    order_no     varchar(32) default null            comment '当前绑定的订单号（预占/占用时）',
    member_id    bigint(20)  default null            comment '当前绑定的会员ID',
    del_flag     char(1)     default '0'             comment '删除标志',
    create_time  datetime    default null            comment '创建时间',
    update_time  datetime    default null            comment '更新时间',
    primary key (slot_id),
    unique key uk_wash_slot_no (cabinet_id, slot_no),
    key idx_wash_slot_order (order_no)
) engine=innodb auto_increment=401 default charset=utf8mb4 comment='钥匙柜格口表';

drop table if exists wash_slot_open_log;
create table wash_slot_open_log (
    id           bigint(20)  not null auto_increment comment '主键',
    cabinet_id   bigint(20)  not null                comment '机柜ID',
    slot_id      bigint(20)  default null            comment '格口ID',
    slot_no      varchar(16) default null            comment '格口号',
    order_no     varchar(32) not null                comment '订单号',
    member_id    bigint(20)  default null            comment '会员ID',
    action       varchar(16) not null                comment 'OPEN_CODE取码 / OPEN开箱 / DEPOSIT存入 / TAKE取回 / RELEASE释放',
    open_code    varchar(16) default null            comment '开箱码（脱敏存储，仅留前2位+*）',
    create_time  datetime    not null default current_timestamp comment '创建时间',
    primary key (id),
    key idx_wash_slot_open_order (order_no, create_time)
) engine=innodb default charset=utf8mb4 comment='开箱操作留痕（只增不删）';

-- 本地开发种子：给 301 号柜配 3 个格口
insert into wash_slot (slot_id, cabinet_id, slot_no, status, create_time) values
(401, 301, 'A01', 'FREE', sysdate()),
(402, 301, 'A02', 'FREE', sysdate()),
(403, 301, 'A03', 'FREE', sysdate());
