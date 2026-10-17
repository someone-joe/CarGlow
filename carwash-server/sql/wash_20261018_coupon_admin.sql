-- =============================================================
-- 后台优惠券管理（券模板 CRUD + 发放 + 核销记录）
--   幂等：可重复执行。MySQL 不支持 create if not exists 之外的行级判重，
--   菜单用 where not exists 保底，权限用 insert ignore。
-- =============================================================

set names utf8mb4;

-- 权限标识与 PRD 6.7.3 的 wash:coupon:* 命名一致，三处（openapi x-permission /
-- 后端 @PreAuthorize / 本 SQL）必须逐字相同。
-- 父目录 2000 = 洗车业务；order_num 接在售后/退款（14-17）之后。

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '优惠券模板', 2000, 18, '#', '', 1, 0, 'F', '0', '0', 'wash:coupon:list', '#', 'admin', now(), '券模板查询/详情'
from dual
where not exists (select 1 from sys_menu where menu_name = '优惠券模板' and parent_id = 2000);

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '新增券模板', 2000, 19, '#', '', 1, 0, 'F', '0', '0', 'wash:coupon:add', '#', 'admin', now(), '新增券模板'
from dual
where not exists (select 1 from sys_menu where menu_name = '新增券模板' and parent_id = 2000);

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '修改券模板', 2000, 20, '#', '', 1, 0, 'F', '0', '0', 'wash:coupon:edit', '#', 'admin', now(), '修改资料与上下架'
from dual
where not exists (select 1 from sys_menu where menu_name = '修改券模板' and parent_id = 2000);

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '删除券模板', 2000, 21, '#', '', 1, 0, 'F', '0', '0', 'wash:coupon:remove', '#', 'admin', now(), '逻辑删除'
from dual
where not exists (select 1 from sys_menu where menu_name = '删除券模板' and parent_id = 2000);

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '优惠券发放', 2000, 22, '#', '', 1, 0, 'F', '0', '0', 'wash:coupon:grant', '#', 'admin', now(), '给指定会员发券'
from dual
where not exists (select 1 from sys_menu where menu_name = '优惠券发放' and parent_id = 2000);

-- 授权：超级管理员默认拿到全部新权限（换角色需另行授权）
insert ignore into sys_role_menu (role_id, menu_id)
select 1, menu_id from sys_menu
where parent_id = 2000
  and perms in ('wash:coupon:list', 'wash:coupon:add', 'wash:coupon:edit',
                'wash:coupon:remove', 'wash:coupon:grant');
