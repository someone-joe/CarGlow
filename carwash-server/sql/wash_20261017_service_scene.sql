-- =============================================================
-- 服务按入口区分 + 自助洗车计时计价可配置
--   场景 scene：UNSENSITIVE 无感(按次/套餐) / SELFWASH 自助(计时) / ALL 两入口通用
--   自助计价：base_amount 分 / base_minutes 分钟起，超时按 overtime_amount 分 / overtime_minutes 分钟
-- =============================================================

alter table wash_service
  add column scene varchar(16) not null default 'ALL' comment '入口场景: UNSENSITIVE无感/SELFWASH自助/ALL通用' after category_id;

-- 存量服务归位：标准洗车=无感套餐
update wash_service set scene = 'UNSENSITIVE' where service_id = 101 and del_flag = '0';

-- 自助洗车服务项（价格仅作展示参考，实际按计时配置计价）
insert into wash_service (category_id, service_name, price_amount, work_minutes, enabled, scene, del_flag, create_time)
select category_id, '自助洗车', 2000, 30, 'Y', 'SELFWASH', '0', now()
from wash_service_category where category_id = 101
  and not exists (select 1 from wash_service where service_name = '自助洗车' and del_flag = '0');

-- 自助洗车计时计价参数（sys_config，后台可改；单位：分/分钟）
insert into sys_config (config_name, config_key, config_value, config_type, create_time)
select t.n, t.k, t.v, 'N', now() from (
    select '自助洗车基础时长(分钟)'  as n, 'wash.selfwash.base_minutes'     as k, '30'  as v
    union all select '自助洗车基础金额(分)', 'wash.selfwash.base_amount',     '2000'
    union all select '自助洗车超时时长(分钟)', 'wash.selfwash.overtime_minutes', '10'
    union all select '自助洗车超时金额(分)', 'wash.selfwash.overtime_amount', '800'
) t
where not exists (select 1 from sys_config c where c.config_key = t.k);

-- 后台菜单：自助洗车配置（父目录 2000 洗车业务；钱包配置已占 2100-2102）
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '自助洗车配置', 2000, 5, 'selfwash-config', 'wash/selfwash-config/index', 1, 0, 'C', '0', '0', 'wash:selfwash:list', 'tool', 'admin', now(), '自助洗车计时计价参数'
from dual
where not exists (select 1 from sys_menu where menu_name = '自助洗车配置');
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '计价参数保存', 2000, 6, '#', '', 1, 0, 'F', '0', '0', 'wash:selfwash:edit', '#', 'admin', now(), ''
from dual
where not exists (select 1 from sys_menu where menu_name = '计价参数保存');

-- 授权：超级管理员默认给全部新菜单（运营/站长角色后续在后台「角色管理」勾选）
insert ignore into sys_role_menu (role_id, menu_id)
select 1, menu_id from sys_menu where menu_name in ('自助洗车配置', '计价参数保存');
