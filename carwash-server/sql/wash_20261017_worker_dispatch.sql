-- =============================================================
-- T13 师傅资质 / 排班 / 自动派单 / 同小区合并
--   派单时机：钥匙入柜（WAIT_KEY -> KEY_IN）时自动派给「同小区、在线、资质通过」的师傅
--   无候选则进「待派单池」，在线师傅可抢单；后台可手动派单/改派
-- =============================================================

-- 1) 师傅档案：服务小区 + 上下班状态
alter table wash_worker
  add column community_id bigint default null comment '服务小区ID(派单优先同小区)' after site_id;
alter table wash_worker
  add column work_status varchar(16) not null default 'OFFLINE' comment '上下班: OFFLINE/ONLINE/BUSY' after status;

-- 2) 订单关联师傅（派单结果）
alter table wash_order
  add column worker_id bigint default null comment '接单师傅ID' after community_id;
alter table wash_order
  add column assigned_at bigint default null comment '派单时间(毫秒)' after worker_id;
alter table wash_order add key idx_order_worker (worker_id, status);

-- 3) 师傅资质（身份证 + 驾照 + 人脸核身 + 审核）
create table if not exists wash_worker_qualification (
  id                bigint      not null auto_increment comment '主键',
  worker_id         bigint      not null comment '师傅ID',
  id_name           varchar(32) default null comment '身份证姓名',
  id_no             varchar(32) default null comment '身份证号',
  id_front_file     varchar(64) default null comment '身份证正面照fileId',
  id_back_file      varchar(64) default null comment '身份证反面照fileId',
  license_file      varchar(64) default null comment '驾照照fileId',
  face_status       varchar(16) not null default 'PENDING' comment '人脸核身: PENDING/PASSED/FAILED',
  face_provider     varchar(16) default null comment '核身渠道: WECHAT/MANUAL',
  face_ref          varchar(64) default null comment '核身凭证号(微信返回或人工登记)',
  face_time         bigint      default null comment '核身完成时间(毫秒)',
  review_status     varchar(16) not null default 'PENDING' comment '审核: PENDING/PASSED/REJECTED',
  review_remark     varchar(255) default null comment '审核备注(驳回原因)',
  reviewer          varchar(64) default null comment '审核人',
  submit_time       bigint      default null comment '提交时间(毫秒)',
  review_time       bigint      default null comment '审核时间(毫秒)',
  del_flag          char(1)     not null default '0' comment '删除标记',
  create_time       datetime    default null comment '创建时间',
  update_time       datetime    default null comment '更新时间',
  primary key (id),
  unique key uk_qual_worker (worker_id)
) engine=innodb default charset=utf8mb4 comment='师傅资质';

-- 4) 师傅周排班（1=周一 ... 7=周日；时间为当日分钟数，如 540=09:00）
create table if not exists wash_worker_schedule (
  id          bigint  not null auto_increment comment '主键',
  worker_id   bigint  not null comment '师傅ID',
  week_day    int     not null comment '星期(1-7)',
  start_minute int    not null comment '开始(当日分钟数)',
  end_minute  int     not null comment '结束(当日分钟数)',
  enabled     char(1) not null default 'Y' comment 'Y启用/N停用',
  del_flag    char(1) not null default '0' comment '删除标记',
  create_time datetime default null comment '创建时间',
  update_time datetime default null comment '更新时间',
  primary key (id),
  key idx_schedule_worker (worker_id, week_day)
) engine=innodb default charset=utf8mb4 comment='师傅周排班';

-- 5) 后台菜单（父目录 2000 洗车业务；钱包2100-2102/自助2132-2133/Banner2134-2137）
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '资质审核', 2000, 11, '#', '', 1, 0, 'F', '0', '0', 'wash:worker:audit', '#', 'admin', now(), '师傅资质审核'
from dual
where not exists (select 1 from sys_menu where menu_name = '资质审核' and parent_id = 2000);
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '排班维护', 2000, 12, '#', '', 1, 0, 'F', '0', '0', 'wash:worker:schedule', '#', 'admin', now(), '师傅周排班'
from dual
where not exists (select 1 from sys_menu where menu_name = '排班维护' and parent_id = 2000);
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '手动派单', 2000, 13, '#', '', 1, 0, 'F', '0', '0', 'wash:order:dispatch', '#', 'admin', now(), '订单手动派单/改派'
from dual
where not exists (select 1 from sys_menu where menu_name = '手动派单' and parent_id = 2000);

-- 授权：超级管理员默认给全部新菜单
insert ignore into sys_role_menu (role_id, menu_id)
select 1, menu_id from sys_menu where menu_name in ('资质审核', '排班维护', '手动派单') and parent_id = 2000;
