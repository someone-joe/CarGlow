set names utf8mb4;

-- ============================================================
-- 营销域：优惠券 + 保单/保险
-- 金额单位：分（BIGINT）；业务时间：毫秒时间戳（BIGINT）
-- 审计字段沿用规范：create_time/update_time DATETIME，del_flag CHAR(1)
-- ============================================================

-- 优惠券模板
CREATE TABLE IF NOT EXISTS wash_coupon_template (
  coupon_template_id BIGINT NOT NULL AUTO_INCREMENT,
  name               VARCHAR(64)  NOT NULL DEFAULT '',
  type               VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL / NEWBIE',
  threshold_amount   BIGINT       NOT NULL DEFAULT 0 COMMENT '使用门槛（分），0=无门槛',
  discount_amount    BIGINT       NOT NULL DEFAULT 0 COMMENT '减免金额（分）',
  total              INT          NOT NULL DEFAULT 0 COMMENT '发行总量',
  issued             INT          NOT NULL DEFAULT 0 COMMENT '已领取',
  per_limit          INT          NOT NULL DEFAULT 1 COMMENT '每人限领',
  start_time         BIGINT       NOT NULL DEFAULT 0 COMMENT '生效时间（毫秒）',
  end_time           BIGINT       NOT NULL DEFAULT 0 COMMENT '失效时间（毫秒）',
  status             CHAR(1)      NOT NULL DEFAULT 'Y' COMMENT 'Y上架 N下架',
  site_id            BIGINT       NOT NULL DEFAULT 0,
  community_id       BIGINT       NOT NULL DEFAULT 0,
  create_by          VARCHAR(64)  DEFAULT '',
  create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by          VARCHAR(64)  DEFAULT '',
  update_time        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  del_flag           CHAR(1)      DEFAULT '0',
  PRIMARY KEY (coupon_template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板';

-- 用户优惠券
CREATE TABLE IF NOT EXISTS wash_coupon_user (
  coupon_user_id BIGINT NOT NULL AUTO_INCREMENT,
  template_id    BIGINT       NOT NULL,
  member_id      BIGINT       NOT NULL,
  status         VARCHAR(16)  NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED / USED / EXPIRED',
  order_no       VARCHAR(32)  DEFAULT NULL,
  obtain_time    BIGINT       DEFAULT 0,
  used_time      BIGINT       DEFAULT NULL,
  expire_time    BIGINT       DEFAULT 0,
  create_by      VARCHAR(64)  DEFAULT '',
  create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by      VARCHAR(64)  DEFAULT '',
  update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  del_flag       CHAR(1)      DEFAULT '0',
  PRIMARY KEY (coupon_user_id),
  KEY idx_member (member_id),
  KEY idx_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券';

-- 保险产品（保障方案）
CREATE TABLE IF NOT EXISTS wash_insurance_product (
  insurance_product_id BIGINT NOT NULL AUTO_INCREMENT,
  name                 VARCHAR(64) NOT NULL DEFAULT '',
  price_amount         BIGINT      NOT NULL DEFAULT 0 COMMENT '保费（分）',
  coverage_desc        VARCHAR(255) DEFAULT '' COMMENT '保障说明',
  status               CHAR(1)    NOT NULL DEFAULT 'Y',
  create_by            VARCHAR(64) DEFAULT '',
  create_time          DATETIME    DEFAULT CURRENT_TIMESTAMP,
  update_by            VARCHAR(64) DEFAULT '',
  update_time          DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  del_flag             CHAR(1)    DEFAULT '0',
  PRIMARY KEY (insurance_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保险产品';

-- 保单（客户实际投保记录）
CREATE TABLE IF NOT EXISTS wash_insurance_policy (
  policy_id    BIGINT NOT NULL AUTO_INCREMENT,
  policy_no    VARCHAR(32) NOT NULL DEFAULT '',
  product_id   BIGINT      NOT NULL,
  member_id    BIGINT      NOT NULL,
  vehicle_id   BIGINT      NOT NULL,
  order_no     VARCHAR(32) DEFAULT NULL,
  status       VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / EXPIRED / CANCELED',
  start_time   BIGINT      DEFAULT 0,
  end_time     BIGINT      DEFAULT 0,
  paid_amount  BIGINT      DEFAULT 0 COMMENT '实付保费（分）',
  create_by    VARCHAR(64) DEFAULT '',
  create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP,
  update_by    VARCHAR(64) DEFAULT '',
  update_time  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  del_flag     CHAR(1)    DEFAULT '0',
  PRIMARY KEY (policy_id),
  UNIQUE KEY uk_policy_no (policy_no),
  KEY idx_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保单';

-- 开发种子：两张券模板 + 一个保险产品（end_time 设到 2099 避免过期）
INSERT INTO wash_coupon_template(name, type, threshold_amount, discount_amount, total, per_limit, start_time, end_time, status)
VALUES ('新客洗车券', 'NEWBIE', 0, 500, 1000, 1, 0, 4102444800000, 'Y');
INSERT INTO wash_coupon_template(name, type, threshold_amount, discount_amount, total, per_limit, start_time, end_time, status)
VALUES ('满减券', 'NORMAL', 3900, 300, 500, 2, 0, 4102444800000, 'Y');
INSERT INTO wash_insurance_product(name, price_amount, coverage_desc, status)
VALUES ('车漆保障险', 990, '保障期内因意外导致的车漆损伤，单次最高赔付 2000 元', 'Y');
