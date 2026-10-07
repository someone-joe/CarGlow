-- 开箱留痕补「角色 / 人员 / 来源端 / 结果」（PRD 6.2：每次开箱记录订单号、角色、人员、时间、格号、结果、来源端）
-- 只增列、不删不改：旧数据 role/user_id/source/result 为 null（客户侧开箱），不受影响
alter table wash_slot_open_log
    add column role    varchar(16) default null comment '开箱角色：CUSTOMER客户 / WORKER工作人员 / ADMIN管理员 / SYSTEM系统' after open_code,
    add column user_id bigint(20)  default null comment '操作人ID（会员 / 师傅 / 管理员）' after role,
    add column source  varchar(16) default null comment '来源端：MP客户小程序 / WORKER师傅小程序 / ADMIN后台 / DEVICE柜机' after user_id,
    add column result  varchar(16) default null comment '开箱结果：SUCCESS成功 / FAIL失败' after source;

-- 工作人员取钥匙的开箱动作（原 action 注释未覆盖）
alter table wash_slot_open_log
    modify column action varchar(16) not null comment 'OPEN_CODE取码 / OPEN开箱 / DEPOSIT存入 / TAKE取回 / RELEASE释放';
