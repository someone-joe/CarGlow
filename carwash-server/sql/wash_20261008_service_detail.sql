set names utf8mb4;

-- 服务详情配置（2026-10-08）：服务内容清单 / 不包含项 / 注意事项 / 样图 / 适用车型 / 封面
-- 明细数组字段统一以 JSON 文本存 TEXT，由 JsonStringListTypeHandler 读写，绝不散落格式转换。
-- 幂等：列已存在则跳过（MySQL 不支持 ADD COLUMN IF NOT EXISTS，用存储过程判断）。

DROP PROCEDURE IF EXISTS `wash_add_service_detail_cols`;
DELIMITER $$
CREATE PROCEDURE `wash_add_service_detail_cols`()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'wash_service' AND column_name = 'detail_items'
    ) THEN
        ALTER TABLE wash_service
            ADD COLUMN detail_items         TEXT         DEFAULT NULL COMMENT '服务内容清单（JSON 数组）',
            ADD COLUMN detail_not_included  TEXT         DEFAULT NULL COMMENT '不包含项（JSON 数组）',
            ADD COLUMN detail_notices       TEXT         DEFAULT NULL COMMENT '注意事项（JSON 数组）',
            ADD COLUMN detail_sample_images TEXT         DEFAULT NULL COMMENT '样图（JSON 数组，图片URL）',
            ADD COLUMN applicable_models    VARCHAR(128) DEFAULT NULL COMMENT '适用车型',
            ADD COLUMN cover_url            VARCHAR(512) DEFAULT NULL COMMENT '封面图URL';
    END IF;
END$$
DELIMITER ;
CALL `wash_add_service_detail_cols`();
DROP PROCEDURE IF EXISTS `wash_add_service_detail_cols`;
