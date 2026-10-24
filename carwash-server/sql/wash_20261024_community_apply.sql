-- 小区开通申请（客户在小程序提交 → 后台审核 → 审核通过即建小区并启用）
-- 配套后端：wash-network 的 WxCommunityApplyController / AdminCommunityApplyController
set names utf8mb4;

CREATE TABLE IF NOT EXISTS wash_community_apply (
  apply_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  member_id      BIGINT       NOT NULL                COMMENT '申请人会员ID',
  community_name VARCHAR(64)  NOT NULL                COMMENT '申请开通的小区名称',
  contact_phone  VARCHAR(32)  DEFAULT ''              COMMENT '联系人手机号',
  status         VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  reject_reason  VARCHAR(255) DEFAULT ''              COMMENT '拒绝原因',
  community_id   BIGINT       DEFAULT NULL            COMMENT '审核通过生成的小区ID',
  site_id        BIGINT       DEFAULT NULL            COMMENT '审核通过时指定的归属站点',
  create_time    DATETIME     DEFAULT NULL            COMMENT '创建时间',
  update_time    DATETIME     DEFAULT NULL            COMMENT '更新时间',
  del_flag       VARCHAR(1)   NOT NULL DEFAULT '0'    COMMENT '删除标记 0正常 1删除',
  PRIMARY KEY (apply_id),
  KEY idx_member (member_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小区开通申请（客户申请→后台审核）';
