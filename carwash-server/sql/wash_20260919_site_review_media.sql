set names utf8mb4;

-- =============================================================
-- B2 站点 / 小区 + B8 分类与默认车 + B5 评价售后 + B3 影像
-- 建表原则（CODEBUDDY 第 5 节）：带 site_id / community_id、审计字段、del_flag
-- =============================================================

-- ----------------------------
-- 1. 站点表（B2：中央站）
-- ----------------------------
drop table if exists wash_site;
create table wash_site (
    site_id             bigint(20)   not null auto_increment comment '站点ID',
    site_name           varchar(64)  not null                comment '站点名，如 泷景中央站',
    daily_limit         int(11)      not null default 0      comment '单日产能上限，0 表示不限量',
    deposit_deadline    varchar(8)   not null default '23:00' comment '最晚存钥匙时间（站点配置，非硬编码）',
    promise_return_time varchar(16)  not null default '次日 07:00' comment '承诺还车时间文案',
    service_status      varchar(16)  not null default 'OPEN' comment 'OPEN正常 / CLOSED停业 / RAINY雨天模式',
    closed_notice       varchar(255) default null            comment '停业或雨天提示文案',
    del_flag            char(1)      default '0'             comment '删除标志（0存在 1删除）',
    create_by           varchar(64)  default ''              comment '创建者',
    create_time         datetime     default null            comment '创建时间',
    update_by           varchar(64)  default ''              comment '更新者',
    update_time         datetime     default null            comment '更新时间',
    primary key (site_id)
) engine=innodb auto_increment=1 default charset=utf8mb4 comment='站点表';

-- ----------------------------
-- 2. 小区表（B2）
-- ----------------------------
drop table if exists wash_community;
create table wash_community (
    community_id   bigint(20)  not null auto_increment comment '小区ID',
    community_name varchar(64) not null                comment '小区名，如 泷景花园',
    site_id        bigint(20)  not null                comment '所属站点ID',
    access_auth    char(1)     not null default 'Y'    comment '地库通行授权（Y是 N否），N 时禁止该小区下单',
    enabled        char(1)     not null default 'Y'    comment '是否开通（Y是 N否）',
    del_flag       char(1)     default '0'             comment '删除标志',
    create_by      varchar(64) default ''              comment '创建者',
    create_time    datetime    default null            comment '创建时间',
    update_by      varchar(64) default ''              comment '更新者',
    update_time    datetime    default null            comment '更新时间',
    primary key (community_id),
    key idx_wash_community_site (site_id)
) engine=innodb auto_increment=1 default charset=utf8mb4 comment='已开通小区表';

-- ----------------------------
-- 3. 服务分类表（B8）
-- ----------------------------
drop table if exists wash_service_category;
create table wash_service_category (
    category_id   bigint(20)  not null auto_increment comment '分类ID',
    category_name varchar(32) not null                comment '分类名，如 洗车服务 / 美容服务',
    sort_num      int(11)     not null default 0      comment '排序，小的在前',
    del_flag      char(1)     default '0'             comment '删除标志',
    create_time   datetime    default null            comment '创建时间',
    primary key (category_id)
) engine=innodb auto_increment=101 default charset=utf8mb4 comment='服务分类表';

-- 服务项挂分类；车辆加默认车标识
alter table wash_service add column category_id bigint(20) default null comment '服务分类ID' after service_name;
alter table wash_vehicle add column is_default char(1) default 'N' comment '是否默认车辆（Y是 N否）' after is_new_energy;

-- ----------------------------
-- 4. 评价表（B5）
-- ----------------------------
drop table if exists wash_review;
create table wash_review (
    review_id   bigint(20)   not null auto_increment comment '评价ID',
    order_no    varchar(32)  not null                comment '订单号',
    member_id   bigint(20)   not null                comment '会员ID',
    rating      int(11)      not null                comment '评分 1-5 星',
    content     varchar(500) default null            comment '评价内容',
    tags        varchar(255) default null            comment '标签，逗号分隔',
    del_flag    char(1)      default '0'             comment '删除标志',
    create_time datetime     default null            comment '创建时间',
    primary key (review_id),
    unique key uk_wash_review_order (order_no),
    key idx_wash_review_member (member_id)
) engine=innodb default charset=utf8mb4 comment='订单评价表（一单一评）';

-- ----------------------------
-- 5. 售后单表（B5：重洗 / 退款 / 理赔）
-- ----------------------------
drop table if exists wash_after_sale;
create table wash_after_sale (
    after_sale_id bigint(20)   not null auto_increment comment '售后ID',
    after_sale_no varchar(32)  not null                comment '售后单号（唯一，幂等键）',
    order_no      varchar(32)  not null                comment '订单号',
    member_id     bigint(20)   not null                comment '会员ID',
    type          varchar(16)  not null                comment 'REWASH重洗 / REFUND退款 / CLAIM理赔',
    reason        varchar(255) default null            comment '原因',
    status        varchar(16)  not null default 'INIT' comment 'INIT待处理 / PROCESSING处理中 / DONE已完成 / REJECTED已驳回',
    amount        bigint(20)   not null default 0      comment '涉及金额（分）',
    del_flag      char(1)      default '0'             comment '删除标志',
    create_time   datetime     not null default current_timestamp comment '创建时间',
    update_time   datetime     default null            comment '更新时间',
    primary key (after_sale_id),
    unique key uk_wash_after_sale_no (after_sale_no),
    key idx_wash_after_sale_order (order_no)
) engine=innodb default charset=utf8mb4 comment='售后单表';

-- ----------------------------
-- 6. 影像表（B3）
-- ----------------------------
drop table if exists wash_media;
create table wash_media (
    media_id   bigint(20)   not null auto_increment comment '影像ID',
    file_id    varchar(64)  not null                comment '对外文件ID（接口只传这个，不暴露磁盘路径）',
    biz_type   varchar(16)  not null                comment 'PARK停车 / PICK取车 / WASHED洗后 / RETURN还车 / COMPARE对比 / VIDEO视频',
    order_no   varchar(32)  default null            comment '关联订单号',
    member_id  bigint(20)   default null            comment '上传会员ID（取送/作业端上传时为空）',
    file_name  varchar(255) default null            comment '原始文件名',
    store_path varchar(255) not null                comment '相对存储路径（不含根目录）',
    file_size  bigint(20)   default 0               comment '文件大小（字节）',
    upload_by  varchar(16)  default null            comment '上传端 CUSTOMER/PICKER/STATION',
    del_flag   char(1)      default '0'             comment '删除标志',
    create_time datetime    not null default current_timestamp comment '创建时间',
    primary key (media_id),
    unique key uk_wash_media_file (file_id),
    key idx_wash_media_order (order_no, biz_type)
) engine=innodb default charset=utf8mb4 comment='影像证据表';

-- =============================================================
-- 本地开发种子数据
-- =============================================================
insert into wash_site (site_id, site_name, daily_limit, deposit_deadline, promise_return_time, service_status, create_time)
values (1, '泷景中央站', 20, '23:00', '次日 07:00', 'OPEN', sysdate());

insert into wash_community (community_id, community_name, site_id, access_auth, enabled, create_time)
values (1, '泷景花园', 1, 'Y', 'Y', sysdate());

insert into wash_service_category (category_id, category_name, sort_num, create_time)
values (101, '洗车服务', 1, sysdate());

update wash_service set category_id = 101 where service_id = 101;
update wash_vehicle set is_default = 'Y' where vehicle_id = 201;
