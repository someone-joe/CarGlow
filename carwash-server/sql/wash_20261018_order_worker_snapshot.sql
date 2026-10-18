-- =============================================================
-- 订单快照派单师傅姓名（C 端展示「谁接单」）
--   为什么用快照而不是 C 端去 join wash_worker：
--   wash-order 不能反向依赖 wash-worker（会成环），跨模块又不能直接查对方的表。
--   项目里已有同一模式的先例：service_name 就是下单时从商品域快照进来的。
--   派单时把师傅姓名一起写进订单，C 端详情/列表直接读，零跨模块调用。
-- =============================================================

set names utf8mb4;

set @exists := (select count(*) from information_schema.columns
                where table_schema = database() and table_name = 'wash_order'
                  and column_name = 'worker_name');
set @sql := if(@exists = 0,
    'alter table wash_order add column worker_name varchar(64) default null comment ''派单师傅姓名快照(C端展示用)'' after assigned_at',
    'select 1');
prepare stmt from @sql; execute stmt; deallocate prepare stmt;
