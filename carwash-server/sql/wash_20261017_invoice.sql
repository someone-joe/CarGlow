-- =============================================================
-- 发票申请（需求 10.6 按订单开票）—— 新模块 wash-invoice
-- 金额以订单实付为准（后端权威），前端不传金额。
-- =============================================================

create table if not exists wash_invoice (
  invoice_id    bigint       not null auto_increment comment '主键',
  member_id     bigint       not null comment '会员ID',
  order_no      varchar(32)  not null comment '订单号(无感/自助均可)',
  amount        bigint       not null comment '开票金额(分)，取自订单实付',
  title_type    varchar(16)  not null default 'PERSONAL' comment 'PERSONAL个人/COMPANY企业',
  title         varchar(128) not null comment '发票抬头',
  tax_no        varchar(64)  default null comment '税号(企业必填)',
  email         varchar(128) not null comment '接收邮箱',
  status        varchar(16)  not null default 'APPLYING' comment 'APPLYING/ISSUED/REJECTED',
  invoice_no    varchar(64)  default null comment '开票后回填的发票号',
  reject_reason varchar(255) default null comment '驳回原因',
  remark        varchar(255) default null comment '备注',
  del_flag      char(1)      not null default '0' comment '删除标记',
  create_time   datetime     default null comment '创建时间',
  update_time   datetime     default null comment '更新时间',
  primary key (invoice_id),
  key idx_inv_member (member_id, invoice_id),
  key idx_inv_order (order_no)
) engine=innodb default charset=utf8mb4 comment='发票申请';

-- ----------------------------
-- 运营后台菜单：发票管理（2130 系列，父目录 2000 洗车业务）
-- ----------------------------
delete from sys_menu where menu_id in (2130, 2131);
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2130, '发票管理', 2000, 13, 'invoice', 'wash/invoice/index', 1, 0, 'C', '0', '0', 'wash:invoice:list', 'documentation', 'admin', sysdate(), '发票申请审核：开票/驳回');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values (2131, '审核开票', 2130, 1, '', null, 1, 0, 'F', '0', '0', 'wash:invoice:audit', '#', 'admin', sysdate(), '开票/驳回处理');

insert ignore into sys_role_menu (role_id, menu_id) values (1, 2130), (1, 2131);
