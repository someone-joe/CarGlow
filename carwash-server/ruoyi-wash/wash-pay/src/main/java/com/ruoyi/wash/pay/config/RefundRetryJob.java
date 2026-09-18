package com.ruoyi.wash.pay.config;

import com.ruoyi.wash.pay.domain.WashRefundOrder;
import com.ruoyi.wash.pay.service.WashPayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * 退款失败重试（本地轻量定时任务）。
 *
 * <p>为什么必须要有：渠道调用会失败（网络抖动、余额不足、证书过期），
 * 一旦失败就"假装退款成功"，钱没退、状态却变已退款，是财务事故。
 * 失败一律留在退款单里，由这里兜底重试，超过次数上限则报警人工处理。
 */
@Configuration
@EnableScheduling
public class RefundRetryJob {

    private static final Logger log = LoggerFactory.getLogger(RefundRetryJob.class);

    /** 最大重试次数，超过后不再自动重试（需人工介入） */
    private static final int MAX_RETRY = 5;

    @Autowired
    private WashPayService payService;

    @Value("${wash.pay.refund-retry-enabled:true}")
    private boolean enabled;

    /** 每 5 分钟扫一次待退款单 */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void retry() {
        if (!enabled) {
            return;
        }
        List<WashRefundOrder> pending = payService.listPending(MAX_RETRY, 50);
        for (WashRefundOrder refund : pending) {
            log.info("[退款重试] refundNo={} 第 {} 次", refund.getRefundNo(),
                    (refund.getRetryCount() == null ? 0 : refund.getRetryCount()) + 1);
            try {
                payService.doRefund(refund);
            } catch (Exception e) {
                log.error("[退款重试异常] refundNo={}", refund.getRefundNo(), e);
            }
        }
    }
}
