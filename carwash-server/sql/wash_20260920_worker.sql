set names utf8mb4;

-- 师傅（工作人员）表：取送与作业合并为单一师傅端，工号登录。
-- status: NORMAL 正常 / DISABLED 禁用
drop table if exists wash_worker;
create table wash_worker (
    worker_id   bigint       not null auto_increment        comment '师傅ID',
    worker_no   varchar(32)  not null                       comment '工号，登录账号（唯一）',
    password    varchar(64)  not null                       comment '登录密码（MVP 明文，上线前改 BCrypt）',
    name        varchar(32)  not null                       comment '姓名',
    phone       varchar(20)  default null                   comment '手机号',
    site_id     bigint       default null                   comment '所属站点ID',
    status      varchar(16)  not null default 'NORMAL'      comment '状态 NORMAL/DISABLED',
    del_flag    char(1)      not null default '0'           comment '删除标记 0存在 2删除',
    create_by   varchar(64)  default null                   comment '创建者',
    create_time datetime     default current_timestamp      comment '创建时间',
    update_by   varchar(64)  default null                   comment '更新者',
    update_time datetime     default current_timestamp on update current_timestamp comment '更新时间',
    remark      varchar(255) default null                   comment '备注',
    primary key (worker_id),
    unique key uk_worker_no (worker_no)
) engine = innodb default charset = utf8mb4 comment = '师傅（工作人员）表';

-- 开发种子：工号 W001 / 密码 dev123，仅本地联调用
insert into wash_worker (worker_no, password, name, phone, site_id, status, del_flag)
values ('W001', 'dev123', '张师傅', '13800000001', 1, 'NORMAL', '0');
