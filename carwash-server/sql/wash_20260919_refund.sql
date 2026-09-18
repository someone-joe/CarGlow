-- =============================================================
-- 退款单表（取消订单后自动退款的载体，支持失败重试）
-- 说明：退款是否真的成功以本表 status 为准，订单状态 REFUNDING 只是"处理中"
-- =============================================================

drop table if exists wash_order_refund;
create table wash_order_refund (
    refund_id     bigint(20)   not null auto_increment comment '主键',
    refund_no     varchar(32)  not null                comment '退款单号（唯一，幂等键）',
    order_no      varchar(32)  not null                comment '订单号',
    member_id     bigint(20)   not null                comment '会员ID',
    amount        bigint(20)   not null                comment '退款金额（分）',
    reason        varchar(255) default null            comment '退款原因',
    status        varchar(16)  not null default 'INIT' comment 'INIT待退款 / SUCCESS已退 / FAILED失败待重试',
    provider      varchar(16)  not null                comment '支付渠道：mock / wechat',
    provider_trade_no varchar(64) default null         comment '渠道退款流水号',
    fail_reason   varchar(255) default null            comment '最近一次失败原因',
    retry_count   int(11)      not null default 0      comment '已重试次数',
    del_flag      char(1)      default '0'             comment '删除标志',
    create_time   datetime     not null default current_timestamp comment '创建时间',
    update_time   datetime     default null            comment '更新时间',
    primary key (refund_id),
    unique key uk_wash_refund_no (refund_no),
    key idx_wash_refund_order (order_no),
    key idx_wash_refund_status (status, create_time)
) engine=innodb default charset=utf8mb4 comment='订单退款单';
