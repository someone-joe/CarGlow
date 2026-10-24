package com.ruoyi.wash.order.config;

import com.ruoyi.wash.order.mapper.WashOrderMapper;
import com.ruoyi.wash.order.state.WashOrderStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * 23:30 未存钥匙顺延次日（规则表 2.2）。
 *
 * <p>客户在预约日 23:30 前仍未把钥匙存入柜机（订单仍 WAIT_KEY），则把订单顺延到次日：
 * 状态不变（WAIT_KEY 自环），预约时间推后一天，释放预占格口供他人使用。
 * 触发走 WashOrderStateService.postpone，与状态机同一入口，禁止散落更新。
 */
@Configuration
@EnableScheduling
public class OrderPostponeJob {

    private static final Logger log = LoggerFactory.getLogger(OrderPostponeJob.class);
    private static final int BATCH = 50;

    @Autowired
    private WashOrderMapper orderMapper;

    @Autowired
    private WashOrderStateService stateService;

    @Value("${wash.order.postpone-enabled:true}")
    private boolean enabled;

    /** 每天 23:30 跑一次 */
    @Scheduled(cron = "${wash.order.postpone-cron:0 30 23 * * ?}")
    public void postponeExpired() {
        runNow(null);
    }

    public void runNow(Map<String, Object> params) {
        if (!enabled) {
            return;
        }
        // asOf 仅运维/接口自动化用：把"今天"对齐到虚拟时刻，便于构造逾期场景（不改订单状态，只影响选单窗口）
        long asOf = asOfMillis(params);
        long startOfToday = LocalDate.ofInstant(Instant.ofEpochMilli(asOf), ZoneId.of("Asia/Shanghai"))
                .atStartOfDay(ZoneId.of("Asia/Shanghai"))
                .toInstant().toEpochMilli();
        List<String> overdue = orderMapper.selectWaitKeyOverdue(startOfToday, BATCH);
        for (String orderNo : overdue) {
            try {
                stateService.postpone(orderNo);
                log.info("[顺延] 已顺延次日 orderNo={}", orderNo);
            } catch (Exception e) {
                log.error("[顺延] 失败 orderNo={}", orderNo, e);
            }
        }
    }

    /** 解析虚拟时刻：运维/接口自动化用，把"今天"对齐到指定毫秒，便于构造逾期场景。默认当前时刻。 */
    private static long asOfMillis(Map<String, Object> params) {
        if (params != null && params.get("asOf") instanceof Number) {
            return ((Number) params.get("asOf")).longValue();
        }
        return System.currentTimeMillis();
    }
}
