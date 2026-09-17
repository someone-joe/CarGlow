-- =============================================================
-- 支付与状态机：订单状态流转日志表
-- 来源：《订单状态机规则表.md》第四节（唯一真源），字段不得擅自增减
-- 导入：docker exec carglow-mysql sh -c "mysql -uroot -pcarglow_dev --default-character-set=utf8mb4 carwash < /tmp/xxx.sql"
-- =============================================================

drop table if exists wash_order_status_log;
create table wash_order_status_log (
    id            bigint(20)   not null auto_increment comment '主键',
    order_id      bigint(20)   not null                comment '订单ID',
    from_status   varchar(32)  default null            comment '流转前状态',
    to_status     varchar(32)  not null                comment '流转后状态',
    event         varchar(32)  not null                comment '触发事件（见 OrderEvent）',
    operator_type varchar(16)  not null                comment '操作者类型 CUSTOMER/PICKER/STATION/ADMIN/JOB/DEVICE',
    operator_id   bigint(20)   default null            comment '操作者ID（JOB、DEVICE 可为空）',
    source        varchar(16)  not null                comment '来源端，与 operator_type 同源，避免两套枚举漂移',
    reason        varchar(255) default null            comment '原因/备注（取消、质检不合格必填）',
    create_time   datetime     not null default current_timestamp comment '创建时间',
    primary key (id),
    key idx_wash_order_status_log_order (order_id, create_time)
) engine=innodb default charset=utf8mb4 comment='订单状态流转日志（只增不删）';
