-- =============================================================
-- 运营后台菜单：自助洗车订单监控（2120 系列，父目录 2000 洗车业务）
-- 表 wash_selfwash_order 已在 wash_20261017_selfwash_order.sql 建好，本文件只加菜单。
-- =============================================================

delete from sys_menu where menu_id in (2120, 2121);
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2120, '自助订单监控', 2000, 12, 'selfwash-order', 'wash/selfwash-order/index', 1, 0, 'C', '0', '0', 'wash:selfwash:order:list', 'dict', 'admin', sysdate(), '自助洗车订单监控：状态/站点/工位筛选 + 强制结束异常单');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2121, '强制结束', 2120, 1, '', null, 1, 0, 'F', '0', '0', 'wash:selfwash:order:finish', '#', 'admin', sysdate(), '强制结束卡在服务中的异常单');

insert ignore into sys_role_menu (role_id, menu_id) values (1, 2120), (1, 2121);
