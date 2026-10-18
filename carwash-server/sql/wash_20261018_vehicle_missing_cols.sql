-- 补齐 wash_vehicle 缺失的两列（运行库用旧版建表脚本重建时漏掉了）
-- 幂等：列已存在则跳过。与 wash_20261018_vehicle_type.sql / wash_20260919_site_review_media.sql 中的 ALTER 互补。
set @db = database();

set @c1 = (select count(*) from information_schema.columns
           where table_schema = @db and table_name = 'wash_vehicle' and column_name = 'vehicle_type');
set @s1 = if(@c1 = 0,
  'alter table wash_vehicle add column vehicle_type varchar(16) not null default \'OTHER\' comment \'车型 SEDAN小轿车/SUV/MPV商务车/OTHER其他，与自助洗车共用枚举\' after color',
  'select 1');
prepare stmt from @s1; execute stmt; deallocate prepare stmt;

set @c2 = (select count(*) from information_schema.columns
           where table_schema = @db and table_name = 'wash_vehicle' and column_name = 'is_default');
set @s2 = if(@c2 = 0,
  'alter table wash_vehicle add column is_default char(1) default \'N\' comment \'是否默认车辆（Y是 N否）\' after is_new_energy',
  'select 1');
prepare stmt from @s2; execute stmt; deallocate prepare stmt;
