-- =============================================================
-- 编码防复发：本文件为 UTF-8，导入前必须让客户端知晓。
-- mysql 客户端默认 latin1（MySQL 的 latin1 实为 cp1252 超集），
-- 不显式声明会把 UTF-8 字节按单字节解释、再以 utf8mb4 存库，造成双重编码乱码。
-- =============================================================
set names utf8mb4;

-- =============================================================
-- 本地开发种子数据（仅开发库使用，生产不要执行！）
-- 导入命令必须带 --default-character-set=utf8mb4，否则中文会被双重编码：
--   docker cp sql/wash_seed_dev.sql carglow-mysql:/tmp/seed.sql
--   docker exec carglow-mysql sh -c "mysql -uroot -pcarglow_dev --default-character-set=utf8mb4 carwash < /tmp/seed.sql"
-- =============================================================

truncate table wash_service;
truncate table wash_vehicle;
truncate table wash_cabinet;

insert into wash_service (service_id, service_name, price_amount, work_minutes, enabled, create_time)
values (101, '标准洗车（外观+内饰吸尘）', 3900, 45, 'Y', sysdate());

insert into wash_vehicle (vehicle_id, member_id, plate_no, brand, color, is_new_energy, community_id, parking_no, create_time)
values (201, 1, '粤A12345', '丰田 凯美瑞', '白色', 'N', 1, 'A区23号车位', sysdate());

insert into wash_cabinet (cabinet_id, site_id, community_id, cabinet_name, enabled, create_time)
values (301, 1, 1, '3栋负一层钥匙柜', 'Y', sysdate());

-- 格口复位：联调下单会把格口占成 RESERVED/OCCUPIED，重跑种子时若不释放，
-- 再下单会一直报 B2003（无空闲格口）。开箱流水日志只增不删，这里不清理。
update wash_slot set status = 'FREE', order_no = null, member_id = null, update_time = sysdate();
