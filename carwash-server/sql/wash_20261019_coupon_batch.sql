set names utf8mb4;

-- ============================================================
-- 优惠券批量发放（2026-10-19）
-- 口径：
--   - 受众：全量 / 按注册时间 / 按小区；小区 = 常用停车位置 ∪ 车辆常停小区（按 member_id 去重，不会因交集重复发）
--   - 每人张数 per_count 默认 1、可改；每人限领 perLimit 与库存仍生效（与单人发放同一套校验）
--   - 部分失败落明细：SUCCESS 成功 / SKIPPED 跳过(达限领) / FAILED 失败(库存等)，支持手动补发
--   - wash_coupon_user.batch_id：批量发出的券挂批次，记录视图据此把一批折叠成一条、单人/C端领取为空
-- ============================================================

-- 批量发放批次头
CREATE TABLE IF NOT EXISTS wash_coupon_batch (
  batch_id       BIGINT       NOT NULL AUTO_INCREMENT,
  template_id    BIGINT       NOT NULL COMMENT '券模板ID',
  per_count      INT          NOT NULL DEFAULT 1 COMMENT '每人发放张数',
  operator       VARCHAR(64)  DEFAULT '' COMMENT '操作人',
  audience_type  VARCHAR(16)  NOT NULL DEFAULT 'ALL' COMMENT 'ALL全量/COMMUNITY小区/TIME时间',
  community_id   BIGINT       DEFAULT NULL COMMENT '受众小区ID（audience_type=COMMUNITY）',
  community_name VARCHAR(64)  DEFAULT NULL COMMENT '小区名快照',
  time_from      BIGINT       DEFAULT NULL COMMENT '注册时间起（毫秒）',
  time_to        BIGINT       DEFAULT NULL COMMENT '注册时间止（毫秒）',
  total_count    INT          NOT NULL DEFAULT 0 COMMENT '受众总数',
  success_count  INT          NOT NULL DEFAULT 0,
  skipped_count  INT          NOT NULL DEFAULT 0,
  failed_count   INT          NOT NULL DEFAULT 0,
  create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  del_flag       CHAR(1)      DEFAULT '0',
  PRIMARY KEY (batch_id),
  KEY idx_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券批量发放批次';

-- 批次明细：每个会员一条当前结果（补发为 update，不新增，保证汇总=按会员统计）
CREATE TABLE IF NOT EXISTS wash_coupon_batch_item (
  item_id       BIGINT       NOT NULL AUTO_INCREMENT,
  batch_id      BIGINT       NOT NULL,
  template_id   BIGINT       NOT NULL,
  member_id     BIGINT       NOT NULL,
  result        VARCHAR(16)  NOT NULL COMMENT 'SUCCESS成功/SKIPPED跳过(达限领)/FAILED失败(库存等)',
  reason        VARCHAR(255) DEFAULT NULL COMMENT '跳过/失败原因',
  granted_count INT          NOT NULL DEFAULT 0 COMMENT '实际发出张数',
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (item_id),
  UNIQUE KEY uk_batch_member (batch_id, member_id),
  KEY idx_batch_result (batch_id, result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券批量发放明细';

-- wash_coupon_user 增加 batch_id（幂等：列已存在则跳过）
set @db = database();
set @col = (select count(*) from information_schema.columns
            where table_schema = @db and table_name = 'wash_coupon_user' and column_name = 'batch_id');
set @sql = if(@col = 0,
  'alter table wash_coupon_user add column batch_id bigint(20) default null comment ''批量发放批次ID，非空=批量发出'' after member_id',
  'select 1');
prepare stmt from @sql; execute stmt; deallocate prepare stmt;

