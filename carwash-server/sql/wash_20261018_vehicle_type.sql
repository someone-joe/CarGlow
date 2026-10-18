-- 会员车辆增加车型字段，与自助洗车下单共用 VehicleType 枚举（SEDAN/SUV/MPV/OTHER）
-- 幂等：列已存在则跳过
set @db = database();
set @col = (select count(*) from information_schema.columns
            where table_schema = @db and table_name = 'wash_vehicle' and column_name = 'vehicle_type');
set @sql = if(@col = 0,
  'alter table wash_vehicle add column vehicle_type varchar(16) not null default \'OTHER\' comment \'车型 SEDAN小轿车/SUV/MPV商务车/OTHER其他，与自助洗车共用枚举\' after color',
  'select 1');
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;
