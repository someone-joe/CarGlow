-- =============================================================
-- 自助洗车工位（T9：软件闭环 + 硬件留口）
-- 洗车站内的自助工位（如 A1/A2/A3）。实时状态（空闲/服务中/排队）由自助订单推导，不落库。
-- 归属新模块 wash-selfwash，与无感洗车订单（wash_order + OrderStatus）完全隔离，互不影响状态机。
-- =============================================================

create table if not exists wash_workstation (
  workstation_id bigint       not null auto_increment comment '主键',
  site_id        bigint       not null comment '所属站点(洗车站)',
  code           varchar(32)  not null comment '工位编号，如 A1',
  name           varchar(64)  default null comment '工位名称',
  enabled        char(1)      not null default 'Y' comment '是否启用 Y/N',
  sort_order     int          not null default 0 comment '排序，升序',
  del_flag       char(1)      not null default '0' comment '删除标记',
  create_time    datetime     default null comment '创建时间',
  update_time    datetime     default null comment '更新时间',
  primary key (workstation_id),
  key idx_ws_site (site_id, sort_order)
) engine=innodb default charset=utf8mb4 comment='自助洗车工位';

-- ----------------------------
-- 运营后台菜单：自助工位管理（2110 系列，父目录 2000 洗车业务）
-- ----------------------------
delete from sys_menu where menu_id in (2110, 2111, 2112, 2113);
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2110, '自助工位管理', 2000, 11, 'selfwash-workstation', 'wash/selfwash-workstation/index', 1, 0, 'C', '0', '0', 'wash:selfwash:list', 'platform', 'admin', sysdate(), '自助洗车工位（站点下 A1/A2/A3）增删改 + 实时状态');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2111, '工位新增', 2110, 1, '', null, 1, 0, 'F', '0', '0', 'wash:selfwash:add', '#', 'admin', sysdate(), '新增工位');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2112, '工位修改', 2110, 2, '', null, 1, 0, 'F', '0', '0', 'wash:selfwash:edit', '#', 'admin', sysdate(), '修改工位编号/名称/启用');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2113, '工位删除', 2110, 3, '', null, 1, 0, 'F', '0', '0', 'wash:selfwash:remove', '#', 'admin', sysdate(), '删除工位');

insert ignore into sys_role_menu (role_id, menu_id) values (1, 2110), (1, 2111), (1, 2112), (1, 2113);
