-- =============================================================
-- C 端协议富文本配置种子（GET /api/v1/auth/agreements）
-- 2026-10-24：补齐契约已有实现（WashAgreementService 读 sys_config）
--
-- ⚠️ 正文为【开发占位】文本，上线前必须由运营在后台「系统管理 → 参数设置」
--    替换为法务审核后的正式协议全文（改完即时生效，无需发版）。
-- 幂等：按 config_key 判存，重复执行不产生重复行。
-- =============================================================

set names utf8mb4;

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 200, '协议-服务条款标题', 'wash.agreement.service.title', '服务条款与钥匙寄存协议', 'Y', 'admin', sysdate(), '', null, 'C 端协议富文本标题；后台「系统管理→参数设置」可覆盖'
from dual where not exists (select 1 from sys_config where config_key = 'wash.agreement.service.title');

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 201, '协议-服务条款正文', 'wash.agreement.service.content', '<p>【开发占位】服务条款与钥匙寄存协议正文待运营补充，上线前必须替换为法务审核文本。</p><p>要点预告：服务范围、钥匙寄存与保管责任、车辆交接确认、取消与退款规则、免责条款。</p>', 'Y', 'admin', sysdate(), '', null, '富文本 HTML；合规文案，代码不内置假文本'
from dual where not exists (select 1 from sys_config where config_key = 'wash.agreement.service.content');

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 202, '协议-隐私政策标题', 'wash.agreement.privacy.title', '隐私政策', 'Y', 'admin', sysdate(), '', null, 'C 端协议富文本标题；后台「系统管理→参数设置」可覆盖'
from dual where not exists (select 1 from sys_config where config_key = 'wash.agreement.privacy.title');

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 203, '协议-隐私政策正文', 'wash.agreement.privacy.content', '<p>【开发占位】隐私政策正文待运营补充，上线前必须替换为法务审核文本。</p><p>要点预告：收集的信息范围（openid/手机号/车辆信息/影像）、使用目的、存储与保护、共享与删除。</p>', 'Y', 'admin', sysdate(), '', null, '富文本 HTML；合规文案，代码不内置假文本'
from dual where not exists (select 1 from sys_config where config_key = 'wash.agreement.privacy.content');

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 204, '协议-充值协议标题', 'wash.agreement.recharge.title', '充值协议', 'Y', 'admin', sysdate(), '', null, 'C 端协议富文本标题；后台「系统管理→参数设置」可覆盖'
from dual where not exists (select 1 from sys_config where config_key = 'wash.agreement.recharge.title');

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 205, '协议-充值协议正文', 'wash.agreement.recharge.content', '<p>【开发占位】充值协议正文待运营补充，上线前必须替换为法务审核文本。</p><p>要点预告：储值余额与赠送余额的使用顺序、不可提现、退款处理、充值赠送规则。</p>', 'Y', 'admin', sysdate(), '', null, '富文本 HTML；合规文案，代码不内置假文本'
from dual where not exists (select 1 from sys_config where config_key = 'wash.agreement.recharge.content');

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 206, '协议-保险与理赔说明标题', 'wash.agreement.insurance.title', '保险与理赔说明', 'Y', 'admin', sysdate(), '', null, 'C 端协议富文本标题；后台「系统管理→参数设置」可覆盖'
from dual where not exists (select 1 from sys_config where config_key = 'wash.agreement.insurance.title');

insert into sys_config (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
select 207, '协议-保险与理赔说明正文', 'wash.agreement.insurance.content', '<p>【开发占位】保险与理赔说明正文待运营补充，上线前必须替换为法务审核文本。</p><p>要点预告：保障范围、理赔材料（洗前洗后照片/开柜记录）、理赔流程与时效。</p>', 'Y', 'admin', sysdate(), '', null, '富文本 HTML；合规文案，代码不内置假文本'
from dual where not exists (select 1 from sys_config where config_key = 'wash.agreement.insurance.content');
