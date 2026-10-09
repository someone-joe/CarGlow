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

-- ----------------------------
-- 站点管理（2026-10-07 新增，中央站可配置）
-- ----------------------------
delete from sys_menu where menu_id in (2050, 2051, 2052, 2053, 2054);
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2050, '站点管理', 2000, 5, 'site', 'wash/site/index', 1, 0, 'C', '0', '0', 'wash:site:list', 'tree', 'admin', sysdate(), '中央站配置：产能/营业时段/停业公告');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2051, '站点查询', 2050, 1, '', null, 1, 0, 'F', '0', '0', 'wash:site:list', '#', 'admin', sysdate(), '查看站点列表');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2052, '站点新增', 2050, 2, '', null, 1, 0, 'F', '0', '0', 'wash:site:add', '#', 'admin', sysdate(), '新增站点');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2053, '站点修改', 2050, 3, '', null, 1, 0, 'F', '0', '0', 'wash:site:edit', '#', 'admin', sysdate(), '修改站点配置');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2054, '站点删除', 2050, 4, '', null, 1, 0, 'F', '0', '0', 'wash:site:remove', '#', 'admin', sysdate(), '删除站点（逻辑删除）');

-- ----------------------------
-- 小区管理（2026-10-07 新增，已开通小区）
-- ----------------------------
delete from sys_menu where menu_id in (2060, 2061, 2062, 2063, 2064);
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2060, '小区管理', 2000, 6, 'community', 'wash/community/index', 1, 0, 'C', '0', '0', 'wash:community:list', 'tree', 'admin', sysdate(), '已开通小区：地库授权/启停');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2061, '小区查询', 2060, 1, '', null, 1, 0, 'F', '0', '0', 'wash:community:list', '#', 'admin', sysdate(), '查看小区列表');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2062, '小区新增', 2060, 2, '', null, 1, 0, 'F', '0', '0', 'wash:community:add', '#', 'admin', sysdate(), '新增小区');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2063, '小区修改', 2060, 3, '', null, 1, 0, 'F', '0', '0', 'wash:community:edit', '#', 'admin', sysdate(), '修改小区/授权/启停');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2064, '小区删除', 2060, 4, '', null, 1, 0, 'F', '0', '0', 'wash:community:remove', '#', 'admin', sysdate(), '删除小区（逻辑删除）');

-- ----------------------------
-- 钥匙柜管理（机柜配置，2026-10-09 新增）
-- ----------------------------
delete from sys_menu where menu_id in (2070, 2071, 2072, 2073, 2074);
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2070, '钥匙柜管理', 2000, 7, 'cabinet', 'wash/cabinet/index', 1, 0, 'C', '0', '0', 'wash:cabinet:list', 'lock', 'admin', sysdate(), '智能钥匙柜（机柜）配置：名称/站点/小区/启停');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2071, '柜机查询', 2070, 1, '', null, 1, 0, 'F', '0', '0', 'wash:cabinet:list', '#', 'admin', sysdate(), '查看机柜列表');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2072, '柜机新增', 2070, 2, '', null, 1, 0, 'F', '0', '0', 'wash:cabinet:add', '#', 'admin', sysdate(), '新增机柜');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2073, '柜机修改', 2070, 3, '', null, 1, 0, 'F', '0', '0', 'wash:cabinet:edit', '#', 'admin', sysdate(), '修改机柜 / 启停');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2074, '柜机删除', 2070, 4, '', null, 1, 0, 'F', '0', '0', 'wash:cabinet:remove', '#', 'admin', sysdate(), '删除机柜（逻辑删除，需先删格口）');

-- ----------------------------
-- 格口管理（2026-10-09 新增）
-- ----------------------------
delete from sys_menu where menu_id in (2080, 2081, 2082, 2083, 2084);
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2080, '格口管理', 2000, 8, 'slot', 'wash/slot/index', 1, 0, 'C', '0', '0', 'wash:slot:list', 'list', 'admin', sysdate(), '格口（柜子的格子）增删改 + 开箱留痕审计');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2081, '格口查询', 2080, 1, '', null, 1, 0, 'F', '0', '0', 'wash:slot:list', '#', 'admin', sysdate(), '查看格口列表');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2082, '格口新增', 2080, 2, '', null, 1, 0, 'F', '0', '0', 'wash:slot:add', '#', 'admin', sysdate(), '新增格口');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2083, '格口修改', 2080, 3, '', null, 1, 0, 'F', '0', '0', 'wash:slot:edit', '#', 'admin', sysdate(), '修改格口号 / 状态');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2084, '格口删除', 2080, 4, '', null, 1, 0, 'F', '0', '0', 'wash:slot:remove', '#', 'admin', sysdate(), '删除格口（使用中不可删）');

-- 角色菜单授权：admin 角色(1) 可见本文件新增的菜单（幂等，重复插入忽略）
-- 原有 2000-2045 的授权此前已在库中存在，本文件未删，故此处只补新增项
insert ignore into sys_role_menu (role_id, menu_id) values
(1, 2050), (1, 2051), (1, 2052), (1, 2053), (1, 2054),
(1, 2060), (1, 2061), (1, 2062), (1, 2063), (1, 2064),
(1, 2070), (1, 2071), (1, 2072), (1, 2073), (1, 2074),
(1, 2080), (1, 2081), (1, 2082), (1, 2083), (1, 2084);
