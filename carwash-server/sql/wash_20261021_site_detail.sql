set names utf8mb4;
-- =============================================================
-- 站点详情配置（服务页 Hero 内容后台可配）
-- detail_title/detail_subtitle/detail_image_url/detail_desc 均为可空：
-- 未配置时 C 端回退站点名与默认卖点文案，不出现空白 Hero。
-- =============================================================
alter table wash_site
    add column detail_title varchar(64) default null comment '站点详情标题（服务页 Hero），空则回退站点名' after closed_notice;
alter table wash_site
    add column detail_subtitle varchar(128) default null comment '站点详情副标题' after detail_title;
alter table wash_site
    add column detail_image_url varchar(255) default null comment '站点详情头图 URL，空则 C 端渲染渐变占位' after detail_subtitle;
alter table wash_site
    add column detail_desc varchar(512) default null comment '站点详情补充文案（可选）' after detail_image_url;

-- 开发种子：给默认站点一套可演示的详情文案（生产由运营在后台维护）
update wash_site set detail_title = '科学洗车', detail_subtitle = '中央站集中精洗 · 标准化 SOP · 全程影像留痕' where detail_title is null;
