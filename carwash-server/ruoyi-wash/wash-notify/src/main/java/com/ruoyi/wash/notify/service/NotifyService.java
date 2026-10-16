package com.ruoyi.wash.notify.service;

import com.ruoyi.wash.member.service.WashMemberQueryService;
import com.ruoyi.wash.notify.domain.NotifyTemplate;
import com.ruoyi.wash.notify.domain.WashNotifyRecord;
import com.ruoyi.wash.notify.mapper.WashNotifyRecordMapper;
import com.ruoyi.wash.notify.transport.MockNotifyTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 订阅消息发送（需求 11 骨架）。
 *
 * <p>流程：开关 → 幂等(bizNo) → 月度限额 → 解析 openid → 通道发送 → 落记录。
 * 模板ID来自配置 wash.notify.templates.*；未配置且非 mock 时跳过并记录原因（不静默丢消息）。
 */
@Service
public class NotifyService {

    private static final Logger log = LoggerFactory.getLogger(NotifyService.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private WashNotifyRecordMapper recordMapper;
    @Autowired
    private WashMemberQueryService memberQuery;
    @Autowired
    private MockNotifyTransport mockTransport;

    @Value("${wash.notify.enabled:true}")
    private boolean enabled;

    @Value("${wash.notify.mock-enabled:true}")
    private boolean mockEnabled;

    @Value("${wash.notify.templates.orderStatus:}")
    private String tplOrderStatus;
    @Value("${wash.notify.templates.keyReady:}")
    private String tplKeyReady;
    @Value("${wash.notify.templates.couponExpire:}")
    private String tplCouponExpire;
    @Value("${wash.notify.templates.balanceLow:}")
    private String tplBalanceLow;
    @Value("${wash.notify.templates.activity:}")
    private String tplActivity;

    /**
     * 发送订阅消息。
     *
     * @param memberId 接收会员
     * @param template 模板
     * @param data     模板变量
     * @param bizNo    业务幂等号（同一业务只发一次；可为 null 表示不做幂等）
     * @return true 表示已发送
     */
    public boolean send(Long memberId, NotifyTemplate template, Map<String, String> data, String bizNo) {
        if (!enabled) {
            log.debug("[notify] 已关闭，跳过 template={}", template.name());
            return false;
        }
        if (bizNo != null && !bizNo.isBlank() && recordMapper.countByBizNo(bizNo) > 0) {
            log.debug("[notify] 幂等命中，跳过 bizNo={}", bizNo);
            return false;
        }
        if (template.monthlyCap() > 0) {
            long used = recordMapper.countSentInMonth(memberId, template.name(), monthStart(), monthEnd());
            if (used >= template.monthlyCap()) {
                record(memberId, null, template, null, bizNo, data, "SKIPPED", "超出每月 " + template.monthlyCap() + " 条上限");
                return false;
            }
        }
        String openid = memberQuery.findOpenid(memberId);
        if (openid == null || openid.isBlank()) {
            record(memberId, null, template, null, bizNo, data, "SKIPPED", "会员无 openid，无法推送");
            return false;
        }
        String templateId = resolveTemplateId(template);
        if (mockEnabled) {
            mockTransport.send(openid, templateId == null ? "" : templateId, data == null ? new HashMap<>() : data);
            record(memberId, openid, template, templateId, bizNo, data, "SENT", null);
            return true;
        }
        if (templateId == null || templateId.isBlank()) {
            record(memberId, openid, template, null, bizNo, data, "SKIPPED", "未配置模板ID");
            return false;
        }
        // 真实通道：接入微信订阅消息 API 后在此调用（需 appid/secret + 用户已授权订阅）
        log.warn("[notify] 真实通道未接入，本次未发送 template={} openid={}", template.name(), openid);
        record(memberId, openid, template, templateId, bizNo, data, "SKIPPED", "真实通道未接入");
        return false;
    }

    /** 便捷：订单状态等场景，data 用业务字段拼装 */
    public boolean send(Long memberId, NotifyTemplate template, Map<String, String> data) {
        return send(memberId, template, data, null);
    }

    private String resolveTemplateId(NotifyTemplate template) {
        return switch (template) {
            case ORDER_STATUS -> tplOrderStatus;
            case KEY_READY -> tplKeyReady;
            case COUPON_EXPIRE -> tplCouponExpire;
            case BALANCE_LOW -> tplBalanceLow;
            case ACTIVITY -> tplActivity;
        };
    }

    private void record(Long memberId, String openid, NotifyTemplate template, String templateId, String bizNo,
                        Map<String, String> data, String status, String reason) {
        WashNotifyRecord r = new WashNotifyRecord();
        r.setMemberId(memberId);
        r.setOpenid(openid);
        r.setTemplateKey(template.name());
        r.setTemplateId(templateId);
        r.setBizNo(bizNo);
        r.setContent(data == null ? "" : data.toString());
        r.setStatus(status);
        r.setFailReason(reason);
        recordMapper.insert(r);
    }

    private Date monthStart() {
        LocalDate first = LocalDate.now(ZONE).withDayOfMonth(1);
        return Date.from(first.atStartOfDay(ZONE).toInstant());
    }

    private Date monthEnd() {
        LocalDate next = LocalDate.now(ZONE).withDayOfMonth(1).plusMonths(1);
        return Date.from(next.atStartOfDay(ZONE).toInstant());
    }
}
