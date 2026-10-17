-- =============================================================
-- 售后工单处置 + 退款落地（2026-10-18）
--   1) wash_after_sale 补处置留痕列：handler_by / handle_remark / handle_time
--   2) 后台权限：售后工单（查看/处置）、退款单（查看/重试）
-- 说明：可重复执行。MySQL 不支持 add column if not exists，故用 information_schema 动态判列。
-- =============================================================

set names utf8mb4;

-- 1) 售后处置留痕：谁处置的、怎么处置的、什么时候处置的。
--    为什么必须要有：退款是财务动作，事后要能查到"谁批的"，只改 status 不构成凭证。

set @exists := (select count(*) from information_schema.columns
                where table_schema = database() and table_name = 'wash_after_sale'
                  and column_name = 'handler_by');
set @sql := if(@exists = 0,
    'alter table wash_after_sale add column handler_by varchar(64) default null comment ''处置人（后台账号）'' after status',
    'select 1');
prepare stmt from @sql; execute stmt; deallocate prepare stmt;

set @exists := (select count(*) from information_schema.columns
                where table_schema = database() and table_name = 'wash_after_sale'
                  and column_name = 'handle_remark');
set @sql := if(@exists = 0,
    'alter table wash_after_sale add column handle_remark varchar(255) default null comment ''处置说明（驳回必填）'' after handler_by',
    'select 1');
prepare stmt from @sql; execute stmt; deallocate prepare stmt;

set @exists := (select count(*) from information_schema.columns
                where table_schema = database() and table_name = 'wash_after_sale'
                  and column_name = 'handle_time');
set @sql := if(@exists = 0,
    'alter table wash_after_sale add column handle_time bigint default null comment ''处置时间（毫秒时间戳）'' after handle_remark',
    'select 1');
prepare stmt from @sql; execute stmt; deallocate prepare stmt;

-- 2) 后台权限（父目录 2000 洗车业务）
--    menu_type = 'F'（按钮/权限）：本次只做后端能力与契约，页面之后自行挂载。
--    授权方式：超级管理员默认拿到全部新权限。

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '售后工单查询', 2000, 14, '#', '', 1, 0, 'F', '0', '0', 'wash:aftersale:list', '#', 'admin', now(), '售后工单列表'
from dual
where not exists (select 1 from sys_menu where menu_name = '售后工单查询' and parent_id = 2000);

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '售后工单处置', 2000, 15, '#', '', 1, 0, 'F', '0', '0', 'wash:aftersale:handle', '#', 'admin', now(), '同意退款/驳回'
from dual
where not exists (select 1 from sys_menu where menu_name = '售后工单处置' and parent_id = 2000);

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '退款单查询', 2000, 16, '#', '', 1, 0, 'F', '0', '0', 'wash:refund:list', '#', 'admin', now(), '退款单列表'
from dual
where not exists (select 1 from sys_menu where menu_name = '退款单查询' and parent_id = 2000);

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '退款单重试', 2000, 17, '#', '', 1, 0, 'F', '0', '0', 'wash:refund:edit', '#', 'admin', now(), '人工重试退款'
from dual
where not exists (select 1 from sys_menu where menu_name = '退款单重试' and parent_id = 2000);

insert ignore into sys_role_menu (role_id, menu_id)
select 1, menu_id from sys_menu
where parent_id = 2000
  and menu_name in ('售后工单查询', '售后工单处置', '退款单查询', '退款单重试');
