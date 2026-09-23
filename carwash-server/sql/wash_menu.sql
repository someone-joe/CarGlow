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

-- ----------------------------
-- 数据看板（2026-09-19 新增）
-- ----------------------------
delete from sys_menu where menu_id = 2020;
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2020, '数据看板', 2000, 0, 'dashboard', 'wash/dashboard/index', 1, 0, 'C', '0', '0', 'wash:dashboard:list', 'chart', 'admin', sysdate(), '今日单量 / 在洗数 / 异常数');

-- 按钮级权限：看板刷新
delete from sys_menu where menu_id = 2021;
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2021, '看板查询', 2020, 1, '', null, 1, 0, 'F', '0', '0', 'wash:dashboard:list', '#', 'admin', sysdate(), '查看经营数据看板');

-- ----------------------------
-- 服务项管理（2026-09-23 新增，商品可配置）
-- ----------------------------
delete from sys_menu where menu_id in (2030, 2031, 2032, 2033, 2034);
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2030, '服务项管理', 2000, 3, 'service', 'wash/service/index', 1, 0, 'C', '0', '0', 'wash:service:list', 'shopping', 'admin', sysdate(), '洗车服务项配置（名称/价格/工时/上下架）');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2031, '服务项查询', 2030, 1, '', null, 1, 0, 'F', '0', '0', 'wash:service:list', '#', 'admin', sysdate(), '查看服务项列表');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2032, '服务项新增', 2030, 2, '', null, 1, 0, 'F', '0', '0', 'wash:service:add', '#', 'admin', sysdate(), '新增服务项');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2033, '服务项修改', 2030, 3, '', null, 1, 0, 'F', '0', '0', 'wash:service:edit', '#', 'admin', sysdate(), '修改服务项 / 上下架 / 改价');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2034, '服务项删除', 2030, 4, '', null, 1, 0, 'F', '0', '0', 'wash:service:remove', '#', 'admin', sysdate(), '删除服务项（逻辑删除）');
