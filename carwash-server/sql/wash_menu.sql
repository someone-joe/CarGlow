-- =============================================================
-- 若依后台菜单：洗车业务（订单管理）
-- 权限标识遵循 PRD 6.7.3 的 wash:*:* 命名
-- 导入后需给角色授权（系统管理 → 角色管理 → 菜单权限），超级管理员自动拥有全部
-- =============================================================

-- 一级目录：洗车业务
delete from sys_menu where menu_id in (2000, 2001);
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2000, '洗车业务', 0, 5, 'wash', null, 1, 0, 'M', '0', '0', '', 'build', 'admin', sysdate(), '洗车业务目录');

-- 二级菜单：订单管理
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2001, '订单管理', 2000, 1, 'order', 'wash/order/index', 1, 0, 'C', '0', '0', 'wash:order:list', 'shopping', 'admin', sysdate(), '订单管理菜单');

-- ----------------------------
-- 会员管理（2026-09-18 新增）
-- ----------------------------
delete from sys_menu where menu_id in (2010, 2011, 2012);
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2010, '会员管理', 2000, 2, 'member', null, 1, 0, 'M', '0', '0', '', 'peoples', 'admin', sysdate(), '会员管理目录');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2011, '会员列表', 2010, 1, 'index', 'wash/member/index', 1, 0, 'C', '0', '0', 'wash:member:list', 'user', 'admin', sysdate(), '会员列表（手机号脱敏展示）');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2012, '车辆列表', 2010, 2, 'vehicle', 'wash/vehicle/index', 1, 0, 'C', '0', '0', 'wash:vehicle:list', 'drive', 'admin', sysdate(), '会员车辆列表');

-- 按钮级权限（menu_type = F，前端用 v-hasPermi 控制显示）
delete from sys_menu where menu_id in (2002, 2003);
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2002, '订单查询', 2001, 1, '', null, 1, 0, 'F', '0', '0', 'wash:order:query', '#', 'admin', sysdate(), '查看订单详情与时间轴');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2003, '订单干预', 2001, 2, '', null, 1, 0, 'F', '0', '0', 'wash:order:edit', '#', 'admin', sysdate(), '人工推进状态 / 后台取消');
