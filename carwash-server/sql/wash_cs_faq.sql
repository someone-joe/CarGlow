-- =============================================================
-- 客服常见问题（您可能会遇到的问题）
-- 小程序客服 tab「您可能会遇到的问题」由后台「客服配置」维护。
-- 全局配置，不按站点拆分（与 sys_config 口径一致），故不带 site_id / community_id。
-- 为什么独立建表而非放入 sys_config：sys_config.config_value 仅 varchar(500)，
-- 放不下 FAQ 列表（问题+解答 + 多条），故独立成表，单一事实来源仍是后台「客服配置」页。
-- =============================================================

create table if not exists wash_cs_faq (
  faq_id      bigint        not null auto_increment comment '主键',
  question    varchar(255)  not null comment '问题文案',
  answer      varchar(1000) default '' comment '解答文案，可空；空则小程序弹通用占位',
  sort_order  int           not null default 0 comment '排序，升序展示',
  del_flag    char(1)       not null default '0' comment '删除标记（0正常 2删除）',
  create_time datetime      default null comment '创建时间',
  update_time datetime      default null comment '更新时间',
  primary key (faq_id)
) engine=innodb default charset=utf8mb4 comment='客服常见问题';

-- 种子数据：保留小程序原硬编码的 13 个高频问题（答案留空，由运营在后台填写）。
-- 幂等：仅当有效数据为 0 时插入一次。
insert into wash_cs_faq (question, answer, sort_order, del_flag, create_time)
select question, answer, sort_order, '0', sysdate() from (
  select '无法打开柜门'   as question, '' as answer, 1  as sort_order union all
  select '忘记存放钥匙'   as question, '' as answer, 2  as sort_order union all
  select '想修改订单'     as question, '' as answer, 3  as sort_order union all
  select '咨询排队情况'   as question, '' as answer, 4  as sort_order union all
  select '是否要取送车'   as question, '' as answer, 5  as sort_order union all
  select '普洗精洗区别'   as question, '' as answer, 6  as sort_order union all
  select '未按服务时间'   as question, '' as answer, 7  as sort_order union all
  select '取消预约'       as question, '' as answer, 8  as sort_order union all
  select '其他小区服务'   as question, '' as answer, 9  as sort_order union all
  select '车主未按时存车' as question, '' as answer, 10 as sort_order union all
  select '具体位置'       as question, '' as answer, 11 as sort_order union all
  select '最近折扣'       as question, '' as answer, 12 as sort_order union all
  select '其他需求'       as question, '' as answer, 13 as sort_order
) as seed
where (select count(*) from wash_cs_faq where del_flag = '0') = 0;
