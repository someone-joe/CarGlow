-- =============================================================
-- 若依后台菜单：洗车业务（订单管理）
-- 权限标识遵循 PRD 6.7.3 的 wash:*:* 命名
-- 导入后需给角色授权（系统管理 → 角色管理 → 菜单权限），超级管理员自动拥有全部
-- =============================================================

-- ---------------------------------------------------------------------------
-- 清理：剔除若依默认但本项目不用的模块（侧边栏纯前端展示由后端菜单驱动）
-- 保留：系统管理全部（用户/角色/菜单/部门/岗位/字典/参数/通知/日志）、日志管理(108)
-- 删除：系统监控(2) 整目录、系统工具(3) 整目录、若依官网(4)
--   日志管理下的 操作日志(500)/登录日志(501) 用的是 monitor/operlog、monitor/logininfor，
--   这两个视图保留，因此只删监控的其余 6 个菜单项
-- ---------------------------------------------------------------------------
delete from sys_menu where menu_id in (2, 3, 4, 109, 110, 111, 112, 113, 114, 115, 116, 117);
delete from sys_role_menu where menu_id in (2, 3, 4, 109, 110, 111, 112, 113, 114, 115, 116, 117);

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

-- ----------------------------
-- 师傅（工作人员）管理（2026-09-27 新增，运营自助维护师傅）
-- ----------------------------
delete from sys_menu where menu_id in (2040, 2041, 2042, 2043, 2044, 2045);
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2040, '师傅管理', 2000, 4, 'worker', 'wash/worker/index', 1, 0, 'C', '0', '0', 'wash:worker:list', 'peoples', 'admin', sysdate(), '师傅（工作人员）维护：新增/禁用/重置密码');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2041, '师傅查询', 2040, 1, '', null, 1, 0, 'F', '0', '0', 'wash:worker:list', '#', 'admin', sysdate(), '查看师傅列表与详情');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2042, '师傅新增', 2040, 2, '', null, 1, 0, 'F', '0', '0', 'wash:worker:add', '#', 'admin', sysdate(), '新增师傅（工号唯一）');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2043, '师傅修改', 2040, 3, '', null, 1, 0, 'F', '0', '0', 'wash:worker:edit', '#', 'admin', sysdate(), '修改资料 / 禁用启用（禁用会立即踢下线）');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2044, '师傅删除', 2040, 4, '', null, 1, 0, 'F', '0', '0', 'wash:worker:remove', '#', 'admin', sysdate(), '删除师傅（逻辑删除）');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2045, '重置师傅密码', 2040, 5, '', null, 1, 0, 'F', '0', '0', 'wash:worker:reset', '#', 'admin', sysdate(), '重置登录密码，重置后旧登录态立即失效');
