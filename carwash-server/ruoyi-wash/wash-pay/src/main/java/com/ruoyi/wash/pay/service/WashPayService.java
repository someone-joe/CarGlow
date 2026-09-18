package com.ruoyi.wash.pay.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderEvent;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.event.OrderCanceledEvent;
import com.ruoyi.wash.order.service.WashOrderPaySupportService;
import com.ruoyi.wash.order.state.WashOrderStateService;
import com.ruoyi.wash.pay.domain.WashRefundOrder;
import com.ruoyi.wash.pay.dto.PrepayVO;
import com.ruoyi.wash.pay.mapper.WashRefundOrderMapper;
import com.ruoyi.wash.pay.provider.PaymentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 支付与退款业务。
 *
 * <p>渠道差异全部收口在 PaymentProvider，本类只管：参数校验、幂等、退款单、状态机推进、失败重试。
 */
@Service
public class WashPayService {

    private static final Logger log = LoggerFactory.getLogger(WashPayService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private PaymentProvider provider;

    @Autowired
    private WashOrderPaySupportService orderSupport;

    @Autowired
    private WashOrderStateService stateService;

    @Autowired
    private WashRefundOrderMapper refundMapper;

    /** 下单后获取支付参数 */
    public PrepayVO prepay(String orderNo, Long memberId) {
        WashOrder order = orderSupport.requireOwned(orderNo, memberId);
        if (!OrderStatus.WAIT_PAY.name().equals(order.getStatus())) {
            throw new ApiException(ErrorCode.B1002, "订单不是待支付状态，当前：" + order.getStatus());
        }
        if (order.getPayExpireAt() != null && order.getPayExpireAt() < System.currentTimeMillis()) {
            throw new ApiException(ErrorCode.B3001);
        }
        return provider.prepay(order);
    }

    /**
     * 支付成功回调：幂等（已支付直接返回），成功后走状态机 PAY_SUCCESS。
     *
     * @return true 表示本次真正推进了状态；false 表示重复通知已忽略
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean handlePaidNotify(String orderNo) {
        WashOrder order = orderSupport.require(orderNo);
        if (!OrderStatus.WAIT_PAY.name().equals(order.getStatus())) {
            // 重复通知：微信会多次回调，忽略即可，不能报错（否则微信一直重发）
            log.info("[支付回调] 忽略重复通知，订单 {} 当前状态 {}", orderNo, order.getStatus());
            return false;
        }
        stateService.paySuccess(orderNo, order.getMemberId());
        return true;
    }

    /**
     * 取消后退款：建退款单 → 调渠道 → 成功则推进状态机到已退款。
     * 失败不抛异常吞掉：退款单留在 INIT/FAILED，由定时任务重试。
     */
    @Transactional(rollbackFor = Exception.class)
    public void refundForCanceled(OrderCanceledEvent event) {
        WashRefundOrder refund = new WashRefundOrder();
        refund.setRefundNo(generateRefundNo());
        refund.setOrderNo(event.orderNo());
        refund.setMemberId(event.memberId());
        refund.setAmount(event.amount());
        refund.setReason(event.reason());
        refund.setProvider(provider.code());
        refundMapper.insertRefund(refund);

        doRefund(refund);
    }

    /** 执行退款（首次与重试共用） */
    @Transactional(rollbackFor = Exception.class)
    public void doRefund(WashRefundOrder refund) {
        int retryCount = (refund.getRetryCount() == null ? 0 : refund.getRetryCount()) + 1;
        PaymentProvider.RefundResult result;
        try {
            result = provider.refund(refund);
        } catch (Exception e) {
            log.error("[退款失败] refundNo={} 渠道异常", refund.getRefundNo(), e);
            result = PaymentProvider.RefundResult.fail(e.getMessage());
        }

        if (result.success()) {
            refundMapper.updateResult(refund.getRefundNo(), "SUCCESS", result.tradeNo(), null, retryCount);
            // 退款成功：先置退款中，再置已退款（两步都在规则表内，留痕完整）
            stateService.systemFire(refund.getOrderNo(), OrderEvent.APPLY_REFUND, "退款成功");
            stateService.systemFire(refund.getOrderNo(), OrderEvent.REFUND_SUCCESS, "退款成功");
            log.info("[退款成功] refundNo={} orderNo={}", refund.getRefundNo(), refund.getOrderNo());
            return;
        }

        refundMapper.updateResult(refund.getRefundNo(), "FAILED", null, result.failReason(), retryCount);
        log.warn("[退款失败待重试] refundNo={} 原因={}", refund.getRefundNo(), result.failReason());
    }

    /** 定时任务调用：重试未成功的退款单 */
    public List<WashRefundOrder> listPending(int maxRetry, int limit) {
        return refundMapper.selectPending(maxRetry, limit);
    }

    /** 退款单号：RF + yyyyMMddHHmmss + 6 位随机 */
    private String generateRefundNo() {
        String time = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT).format(new Date());
        return "RF" + time + (100000 + RANDOM.nextInt(900000));
    }
}
