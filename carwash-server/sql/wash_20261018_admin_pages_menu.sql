-- =============================================================
-- 后台页面菜单：售后工单 / 退款单 / 优惠券管理
--   背景：T13 与退款、优惠券落地时只建了 F（按钮）权限，页面没有 C 菜单就进不了侧边栏。
--   本次补 3 个 C 菜单，并把对应按钮权限挂到各自菜单下（与「服务项管理」的树形一致）。
--   幂等：menu_id 固定 + where not exists 判重，可重复执行。
-- =============================================================

set names utf8mb4;

-- 1) C 菜单（2150 起，当前 sys_menu 最大 id 为 2149）
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2150, '优惠券管理', 2000, 23, 'coupon', 'wash/coupon/index', 1, 0, 'C', '0', '0', 'wash:coupon:list', 'shopping', 'admin', now(), '券模板 / 发放 / 核销记录'
from dual
where not exists (select 1 from sys_menu where menu_id = 2150);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2151, '售后工单', 2000, 24, 'aftersale', 'wash/aftersale/index', 1, 0, 'C', '0', '0', 'wash:aftersale:list', 'form', 'admin', now(), '售后工单处置（同意退款 / 驳回）'
from dual
where not exists (select 1 from sys_menu where menu_id = 2151);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 2152, '退款单', 2000, 25, 'refund', 'wash/refund/index', 1, 0, 'C', '0', '0', 'wash:refund:list', 'money', 'admin', now(), '退款进度查询与人工重试'
from dual
where not exists (select 1 from sys_menu where menu_id = 2152);

-- 2) 把按钮权限从「洗车业务」目录下移到各自菜单下（树形与「服务项管理」一致）
--    用嵌套 select 包一层：MySQL 不允许 UPDATE 直接子查询同一张表
--    ⚠️ 必须带 menu_type = 'F'：新 C 菜单自己的 perms 同样匹配 'wash:coupon:%'，
--       不过滤会把 C 菜单自己 reparent 成自己（parent_id = menu_id），菜单直接从树上消失。
--       这个坑真踩过（2026-10-18）：侧边栏看不到优惠券管理，查库才发现三菜单自指。
update sys_menu set parent_id = (select m.menu_id from (select menu_id from sys_menu where menu_id = 2150) m)
where parent_id = 2000 and menu_type = 'F' and perms like 'wash:coupon:%';

update sys_menu set parent_id = (select m.menu_id from (select menu_id from sys_menu where menu_id = 2151) m)
where parent_id = 2000 and menu_type = 'F' and perms like 'wash:aftersale:%';

update sys_menu set parent_id = (select m.menu_id from (select menu_id from sys_menu where menu_id = 2152) m)
where parent_id = 2000 and menu_type = 'F' and perms like 'wash:refund:%';

-- 3) 授权：超级管理员默认拿到 3 个新菜单（按钮权限此前已授，这里不重复）
insert ignore into sys_role_menu (role_id, menu_id)
select 1, menu_id from sys_menu where menu_id in (2150, 2151, 2152);
