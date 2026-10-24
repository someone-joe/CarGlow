package com.ruoyi.wash.pay.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderEvent;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.event.AfterSaleRefundEvent;
import com.ruoyi.wash.order.event.OrderCanceledEvent;
import com.ruoyi.wash.order.event.RefundSucceededEvent;
import com.ruoyi.wash.order.service.WashOrderPaySupportService;
import com.ruoyi.wash.order.state.WashOrderStateService;
import com.ruoyi.wash.pay.domain.RefundStatus;
import com.ruoyi.wash.pay.domain.WashRefundOrder;
import com.ruoyi.wash.pay.dto.PrepayVO;
import com.ruoyi.wash.pay.dto.RefundVO;
import com.ruoyi.wash.pay.mapper.WashRefundOrderMapper;
import com.ruoyi.wash.pay.provider.MockPaymentProvider;
import com.ruoyi.wash.pay.provider.PaymentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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

    @Autowired
    private WashWalletService walletService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

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
     * 储值余额全额支付（本期渠道之一）：校验 → 扣款 → 状态机 PAY_SUCCESS。
     * 与 handlePaidNotify 同一事务，扣款与状态推进同成功同回滚；余额不足抛 B3004。
     */
    @Transactional(rollbackFor = Exception.class)
    public void payWithBalance(String orderNo, Long memberId) {
        WashOrder order = orderSupport.requireOwned(orderNo, memberId);
        if (!OrderStatus.WAIT_PAY.name().equals(order.getStatus())) {
            throw new ApiException(ErrorCode.B1002, "订单不是待支付状态，当前：" + order.getStatus());
        }
        if (order.getPayExpireAt() != null && order.getPayExpireAt() < System.currentTimeMillis()) {
            throw new ApiException(ErrorCode.B3001);
        }
        long amount = order.getPayAmount() == null ? 0L : order.getPayAmount();
        if (amount <= 0) {
            throw new ApiException(ErrorCode.A0001, "订单金额异常");
        }
        walletService.deductForOrder(memberId, orderNo, amount);
        stateService.paySuccess(orderNo, memberId);
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
        createThenRefund(event.orderNo(), event.memberId(), event.amount(), event.reason());
    }

    /**
     * 售后同意退款。与取消退款共用同一套"建单 + 调渠道"逻辑——两者只有来源不同
     * （reason 与后续对账口径），**钱怎么退出去必须只有一条路**，否则两边逻辑早晚漂移。
     */
    @Transactional(rollbackFor = Exception.class)
    public void refundForAfterSale(AfterSaleRefundEvent event) {
        createThenRefund(event.orderNo(), event.memberId(), event.amount(), event.reason());
    }

    /** 退款单先落库再调渠道：渠道失败也留得住，由 RefundRetryJob 兜底重试 */
    private void createThenRefund(String orderNo, Long memberId, Long amount, String reason) {
        WashRefundOrder refund = new WashRefundOrder();
        refund.setRefundNo(generateRefundNo());
        refund.setOrderNo(orderNo);
        refund.setMemberId(memberId);
        refund.setAmount(amount);
        refund.setReason(reason);
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
            advanceAfterRefund(refund);
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

    /**
     * 仅 mock 渠道支持：切换"强制失败"开关，用于接口自动化构造退款失败→重试场景（解锁 P07）。
     * 非 mock 渠道（wechat）忽略，避免误触真实渠道。
     */
    public void setMockForceFail(boolean forceFail) {
        if (provider instanceof MockPaymentProvider m) {
            m.setForceFail(forceFail);
        }
    }

    // ------------------------------------------------------------------ 查询

    /** C 端「我的退款单」：一律按会员过滤，不给越权留口子 */
    public List<RefundVO> listMine(Long memberId, String orderNo) {
        return withLabel(refundMapper.selectVOByMember(memberId, orderNo));
    }

    public List<RefundVO> adminList(String orderNo, String status, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return withLabel(refundMapper.selectAdminList(orderNo, status, offset, pageSize));
    }

    /** 补状态中文名：前端只展示不翻译（不在前端维护第二份状态名单） */
    private List<RefundVO> withLabel(List<RefundVO> rows) {
        for (RefundVO vo : rows) {
            vo.setStatusLabel(RefundStatus.labelOf(vo.getStatus()));
        }
        return rows;
    }

    public long countAdmin(String orderNo, String status) {
        return refundMapper.countAdmin(orderNo, status);
    }

    /** 人工重试（超过自动重试上限或运营催办）。SUCCESS 的单坚决不退第二次。 */
    public void manualRetry(String refundNo) {
        WashRefundOrder refund = refundMapper.selectByRefundNo(refundNo);
        if (refund == null) {
            throw new ApiException(ErrorCode.A0001, "退款单不存在：" + refundNo);
        }
        if ("SUCCESS".equals(refund.getStatus())) {
            throw new ApiException(ErrorCode.A0001, "该退款单已退款成功，不能重复退款");
        }
        doRefund(refund);
    }

    // ------------------------------------------------------------------ 内部

    /**
     * 退款到账后的收尾：推进订单状态机，并通知订单域把售后工单收尾。
     *
     * <p>为什么这里必须单独 try 兜住异常：钱已经在渠道退掉了，如果抛异常让外层事务回滚，
     * 退款单会退回 INIT/FAILED，定时任务下一轮就会**再退一次**——那是真正的重复打款事故。
     * 宁可让订单状态暂时没跟上（运营在退款单页能看到 SUCCESS），也不能把同一笔钱退两遍。
     */
    private void advanceAfterRefund(WashRefundOrder refund) {
        try {
            // 先置退款中，再置已退款（两步都在规则表内，留痕完整）
            stateService.systemFire(refund.getOrderNo(), OrderEvent.APPLY_REFUND, "退款成功");
            stateService.systemFire(refund.getOrderNo(), OrderEvent.REFUND_SUCCESS, "退款成功");
            eventPublisher.publishEvent(new RefundSucceededEvent(
                    refund.getOrderNo(), refund.getRefundNo(), refund.getAmount()));
        } catch (Exception e) {
            log.error("[退款已到账但收尾失败] 需人工处理 refundNo={} orderNo={} amount={}分",
                    refund.getRefundNo(), refund.getOrderNo(), refund.getAmount(), e);
        }
    }

    /** 退款单号：RF + yyyyMMddHHmmss + 6 位随机 */
    private String generateRefundNo() {
        String time = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT).format(new Date());
        return "RF" + time + (100000 + RANDOM.nextInt(900000));
    }
}
