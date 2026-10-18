-- =============================================================
-- 自助洗车订单：车牌必填 + 新增车型字段（2026-10-18）
--   1) wash_selfwash_order 增加 vehicle_type 列
--   2) 历史数据回填为 OTHER（车型是本次新增的必填项，老单没有这个信息）
-- 幂等：可重复执行（information_schema 判列）。
-- =============================================================

set names utf8mb4;

set @exists := (select count(*) from information_schema.columns
                where table_schema = database() and table_name = 'wash_selfwash_order'
                  and column_name = 'vehicle_type');
set @sql := if(@exists = 0,
    'alter table wash_selfwash_order add column vehicle_type varchar(16) not null default ''OTHER'' comment ''车型 SEDAN小轿车/SUV/MPV商务车/OTHER其他'' after plate_no',
    'select 1');
prepare stmt from @sql; execute stmt; deallocate prepare stmt;

-- 老单没有车型信息，统一回填 OTHER，避免读出来是空值
update wash_selfwash_order set vehicle_type = 'OTHER' where vehicle_type is null or vehicle_type = '';
