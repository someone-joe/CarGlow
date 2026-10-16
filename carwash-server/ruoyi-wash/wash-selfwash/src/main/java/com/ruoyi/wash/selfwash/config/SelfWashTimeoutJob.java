package com.ruoyi.wash.selfwash.config;

import com.ruoyi.wash.selfwash.service.WashSelfwashOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 自助洗车服务到点自动结束（软件闭环的"断电"）。
 *
 * <p>SERVING 且 expire_at 已过的订单：置 DONE + 触发设备断电(mock) + 开始该工位下一排队单。
 * 精度 30s，足够演示；接真实设备后由设备回传替代亦可保留本任务做兜底。
 */
@Configuration
@EnableScheduling
public class SelfWashTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(SelfWashTimeoutJob.class);

    @Autowired
    private WashSelfwashOrderService orderService;

    @Value("${wash.selfwash.timeout-enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelay = 30 * 1000)
    public void finishExpired() {
        if (!enabled) {
            return;
        }
        try {
            orderService.processExpiredServing();
        } catch (Exception e) {
            log.warn("[自助洗车] 到点结束任务异常", e);
        }
    }
}
