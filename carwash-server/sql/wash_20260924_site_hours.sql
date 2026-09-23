set names utf8mb4;
-- =============================================================
-- 站点营业时间（夜间服务时段起止，整点）
-- 下单页可选时段不再写死 19:00-23:00，改由站点配置下发（CapacityVO）
-- =============================================================
alter table wash_site
    add column business_start varchar(5) not null default '19:00' comment '营业开始时间（整点，如 19:00）' after promise_return_time;
alter table wash_site
    add column business_end varchar(5) not null default '23:00' comment '营业结束时间（整点，如 23:00）' after business_start;

update wash_site set business_start = '19:00', business_end = '23:00';
