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

/**
 * 支付超时自动取消。
 *
 * <p>为什么必须要有：下单时就预占了格口并扣了当晚产能。客户下单不付钱就走了，
 * 订单会永远停在 WAIT_PAY，格口和产能一直被占着 —— 柜子只有几十个格子，
 * 很快就被"僵尸单"占满，后面的真实客户下不了单（B2003）。
 *
 * <p>取消走 WashOrderStateService，因此回补产能、释放格口、写流转日志全部复用，
 * 不在这里另写一套（禁止散落状态更新）。
 */
@Configuration
@EnableScheduling
public class OrderPayTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(OrderPayTimeoutJob.class);

    /** 单次处理上限：防止一次扫出巨量数据把库拖住 */
    private static final int BATCH = 50;

    @Autowired
    private WashOrderMapper orderMapper;

    @Autowired
    private WashOrderStateService stateService;

    @Value("${wash.order.pay-timeout-enabled:true}")
    private boolean enabled;

    /** 每分钟扫一次：支付倒计时以毫秒计，分钟级精度足够，且不会对库造成压力 */
    @Scheduled(fixedDelay = 60 * 1000)
    public void cancelExpired() {
        if (!enabled) {
            return;
        }
        List<String> expired = orderMapper.selectExpiredWaitPay(System.currentTimeMillis(), BATCH);
        for (String orderNo : expired) {
            try {
                stateService.systemCancel(orderNo);
                log.info("[支付超时] 已自动取消订单 {}", orderNo);
            } catch (Exception e) {
                // 单条失败不能影响整批，下一轮会继续重试
                log.error("[支付超时] 取消失败 orderNo={}", orderNo, e);
            }
        }
    }
}
