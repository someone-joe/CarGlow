-- =============================================================
-- 首页 Banner 轮播（契约 /api/v1/banners，此前只有契约和前端调用，后端未实现）
--   image_url 可为空：无图时 C 端渲染「标题+副标题」文字图，避免没有素材时轮播空档
-- =============================================================

create table if not exists wash_banner (
  banner_id    bigint      not null auto_increment comment '主键',
  scene        varchar(16) not null default 'HOME' comment '展示场景: HOME 首页',
  title        varchar(64) not null comment '标题',
  subtitle     varchar(128) default null comment '副标题(无图时文字图展示)',
  image_url    varchar(512) default null comment '图片URL(为空则渲染文字图)',
  link_type    varchar(16) not null default 'NONE' comment '跳转类型: NONE/ACTIVITY/RECHARGE/SERVICE',
  link_value   varchar(64) default null comment '跳转值(SERVICE 时传 serviceId)',
  sort_order   int         not null default 0 comment '排序(升序)',
  start_time   bigint      default null comment '生效时间(毫秒)，空=长期',
  end_time     bigint      default null comment '失效时间(毫秒)，空=长期',
  enabled      char(1)     not null default 'Y' comment 'Y上架/N下架',
  del_flag     char(1)     not null default '0' comment '删除标记',
  create_time  datetime    default null comment '创建时间',
  update_time  datetime    default null comment '更新时间',
  primary key (banner_id),
  key idx_banner_scene (scene, enabled, sort_order)
) engine=innodb default charset=utf8mb4 comment='首页Banner';

-- 种子：3 张服务理念图（与小程序无配置时的降级文案一致，后台可替换为真图）
insert into wash_banner (scene, title, subtitle, image_url, link_type, link_value, sort_order, start_time, end_time, enabled, del_flag, create_time)
select 'HOME', t.t, t.s, null, 'NONE', null, t.o, null, null, 'Y', '0', now() from (
    select '无感' as t, '到店即走，无需排队等待' as s, 1 as o
    union all select '安全', '取洗还全程影像留痕', 2
    union all select '专业洁净', '标准化SOP，中央站精洗', 3
) t
where not exists (select 1 from wash_banner where scene = 'HOME');

-- 后台菜单（父目录 2000 洗车业务；钱包配置 2100-2102、自助配置 2132-2133）
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'Banner管理', 2000, 7, 'banner', 'wash/banner/index', 1, 0, 'C', '0', '0', 'wash:banner:list', 'picture', 'admin', now(), '首页轮播'
from dual
where not exists (select 1 from sys_menu where menu_name = 'Banner管理' and parent_id = 2000);
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'Banner新增', 2000, 8, '#', '', 1, 0, 'F', '0', '0', 'wash:banner:add', '#', 'admin', now(), ''
from dual
where not exists (select 1 from sys_menu where menu_name = 'Banner新增' and parent_id = 2000);
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'Banner修改', 2000, 9, '#', '', 1, 0, 'F', '0', '0', 'wash:banner:edit', '#', 'admin', now(), ''
from dual
where not exists (select 1 from sys_menu where menu_name = 'Banner修改' and parent_id = 2000);
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'Banner删除', 2000, 10, '#', '', 1, 0, 'F', '0', '0', 'wash:banner:remove', '#', 'admin', now(), ''
from dual
where not exists (select 1 from sys_menu where menu_name = 'Banner删除' and parent_id = 2000);

-- 授权：超级管理员默认给全部新菜单（运营/站长角色后续在后台「角色管理」勾选）
insert ignore into sys_role_menu (role_id, menu_id)
select 1, menu_id from sys_menu where menu_name in ('Banner管理', 'Banner新增', 'Banner修改', 'Banner删除') and parent_id = 2000;
