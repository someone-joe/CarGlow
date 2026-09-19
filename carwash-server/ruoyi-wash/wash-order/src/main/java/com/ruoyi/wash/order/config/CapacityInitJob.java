package com.ruoyi.wash.order.config;

import com.ruoyi.wash.order.service.WashCapacityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 每日产能初始化。
 *
 * <p>为什么必须要有：下单时「Redis key 存在才扣减，不存在视为不限量」。
 * 没人初始化产能的话，限量形同虚设 —— 站点配了 20 个名额，实际能无限下单。
 *
 * <p>为什么是周期性而不是每天 0 点一次：① 服务重启会错过 0 点；
 * ② 客户预约的是明天甚至后天（capacity key 用的是预约日期），只初始化今天不够。
 * 因此按固定间隔补未来的 N 天，配合 SETNX 天然幂等。
 */
@Configuration
@EnableScheduling
public class CapacityInitJob {

    private static final Logger log = LoggerFactory.getLogger(CapacityInitJob.class);

    @Autowired
    private WashCapacityService capacityService;

    @Value("${wash.capacity.init-enabled:true}")
    private boolean enabled;

    /** 提前初始化多少天（含今天） */
    @Value("${wash.capacity.days-ahead:7}")
    private int daysAhead;

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void init() {
        if (!enabled) {
            return;
        }
        try {
            capacityService.init(daysAhead);
        } catch (Exception e) {
            // 初始化失败不能影响其他定时任务；下一轮继续尝试
            log.error("[产能初始化] 执行失败", e);
        }
    }
}
