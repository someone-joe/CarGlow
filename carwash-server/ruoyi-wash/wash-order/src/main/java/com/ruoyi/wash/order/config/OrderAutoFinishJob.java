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

import java.util.List;
import java.util.Map;

/**
 * T+3 超时自动完成（规则表 2.1 / 规则 5）。
 *
 * <p>订单进入 WAIT_REVIEW（待评价）满 T+3 天后，客户仍未评价，由定时任务发 AUTO_FINISH 自动收尾。
 * 阈值 wash.order.auto-finish-days 可配（默认 3），不硬编码。触发走 WashOrderStateService.autoFinish。
 */
@Configuration
@EnableScheduling
public class OrderAutoFinishJob {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoFinishJob.class);
    private static final int BATCH = 50;

    @Autowired
    private WashOrderMapper orderMapper;

    @Autowired
    private WashOrderStateService stateService;

    @Value("${wash.order.auto-finish-enabled:true}")
    private boolean enabled;

    @Value("${wash.order.auto-finish-days:3}")
    private int days;

    /** 每天凌晨 2 点跑一次（避开高峰） */
    @Scheduled(cron = "${wash.order.auto-finish-cron:0 0 2 * * ?}")
    public void autoFinishExpired() {
        runNow(null);
    }

    public void runNow(Map<String, Object> params) {
        if (!enabled) {
            return;
        }
        // asOf 仅运维/接口自动化用：把"现在"对齐到虚拟时刻，便于构造 T+3 边界（不改订单状态，只影响选单窗口）
        long asOf = (params != null && params.get("asOf") instanceof Number)
                ? ((Number) params.get("asOf")).longValue() : System.currentTimeMillis();
        long threshold = asOf - (long) days * 24L * 3600L * 1000L;
        List<String> expired = orderMapper.selectWaitReviewExpired(threshold, BATCH);
        for (String orderNo : expired) {
            try {
                stateService.autoFinish(orderNo);
                log.info("[T+3自动完成] orderNo={}", orderNo);
            } catch (Exception e) {
                log.error("[T+3自动完成] 失败 orderNo={}", orderNo, e);
            }
        }
    }
}
