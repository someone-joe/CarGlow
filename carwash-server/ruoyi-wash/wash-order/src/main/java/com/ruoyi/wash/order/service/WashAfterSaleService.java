package com.ruoyi.wash.order.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.order.domain.AfterSaleStatus;
import com.ruoyi.wash.order.domain.WashAfterSale;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.domain.WashReview;
import com.ruoyi.wash.order.dto.AdminAfterSaleVO;
import com.ruoyi.wash.order.dto.AfterSaleVO;
import com.ruoyi.wash.order.event.AfterSaleRefundEvent;
import com.ruoyi.wash.order.event.RefundSucceededEvent;
import com.ruoyi.wash.order.mapper.WashAfterSaleMapper;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import com.ruoyi.wash.order.mapper.WashReviewMapper;
import com.ruoyi.wash.order.state.WashOrderStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 评价与售后。
 *
 * <p>评价走状态机 REVIEW_SUBMIT（WAIT_REVIEW → FINISHED），不直接改状态。
 * 低星评价要能追溯到人：强制填原因并告警留痕，复盘工单表开工后在这里生成工单。
 */
@Service
public class WashAfterSaleService {

    private static final Logger log = LoggerFactory.getLogger(WashAfterSaleService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> SALE_TYPES = Set.of("REWASH", "REFUND", "CLAIM");
    /** 低于该星数必须填原因（契约：≤4 星强制填原因并生成复盘工单） */
    private static final int REASON_REQUIRED_BELOW = 5;

    @Autowired
    private WashOrderMapper orderMapper;

    @Autowired
    private WashReviewMapper reviewMapper;

    @Autowired
    private WashAfterSaleMapper afterSaleMapper;

    @Autowired
    private WashOrderStateService stateService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /** 提交评价：rating 必填 1-5；≤4 星必须填原因 */
    @Transactional(rollbackFor = Exception.class)
    public void submitReview(String orderNo, Long memberId, Integer rating, List<String> tags, String content) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new ApiException(ErrorCode.A0001, "评分必须为 1-5 星");
        }
        String text = content == null ? null : content.trim();
        if (rating < REASON_REQUIRED_BELOW && (text == null || text.isEmpty())) {
            throw new ApiException(ErrorCode.A0001, "4 星及以下请说明原因，便于我们改进");
        }
        if (reviewMapper.selectIdByOrderNo(orderNo) != null) {
            throw new ApiException(ErrorCode.A0001, "该订单已评价");
        }

        WashOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getMemberId().equals(memberId)) {
            throw new ApiException(ErrorCode.B1001);
        }

        WashReview review = new WashReview();
        review.setOrderNo(orderNo);
        review.setMemberId(memberId);
        review.setRating(rating);
        review.setContent(text);
        review.setTags(tags == null || tags.isEmpty() ? null : String.join(",", tags));
        reviewMapper.insertReview(review);

        // 低星必须留痕并告警：运营要能看到是哪一单、几星、什么原因
        if (rating < REASON_REQUIRED_BELOW) {
            log.warn("[低星评价] 需复盘：orderNo={} rating={} 原因={}", orderNo, rating, text);
        }

        stateService.submitReview(orderNo, memberId);
    }

    /** 提交售后（重洗 / 退款 / 理赔）：只建单，处理流程由后台推进，避免客户端直接改钱 */
    @Transactional(rollbackFor = Exception.class)
    public String createAfterSale(String orderNo, Long memberId, String type, String reason) {
        if (type == null || !SALE_TYPES.contains(type)) {
            throw new ApiException(ErrorCode.A0001, "未知售后类型：" + type);
        }
        WashOrder order = orderMapper.selectByOrderNo(orderNo);
        if (order == null || !order.getMemberId().equals(memberId)) {
            throw new ApiException(ErrorCode.B1001);
        }
        // 理赔必须有影像佐证，但影像模块刚开工，这里只校验订单已结束，不阻断提交
        if ("CLAIM".equals(type) && OrderStatus.WAIT_PAY.name().equals(order.getStatus())) {
            throw new ApiException(ErrorCode.B1002, "未支付订单不能发起理赔");
        }

        WashAfterSale sale = new WashAfterSale();
        sale.setAfterSaleNo(generateNo());
        sale.setOrderNo(orderNo);
        sale.setMemberId(memberId);
        sale.setType(type);
        sale.setReason(reason);
        sale.setStatus("INIT");
        sale.setAmount(order.getPayAmount());
        afterSaleMapper.insertAfterSale(sale);

        log.info("[售后] 新建工单 {} orderNo={} type={}", sale.getAfterSaleNo(), orderNo, type);
        return sale.getAfterSaleNo();
    }

    /** 售后单号：AF + yyyyMMddHHmmss + 6 位随机 */
    private String generateNo() {
        String time = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT).format(new Date());
        return "AF" + time + (100000 + RANDOM.nextInt(900000));
    }

    // ------------------------------------------------------------------ 查询

    /** C 端「我的售后」：只返回本人的工单 */
    public List<AfterSaleVO> listMine(Long memberId, String status) {
        List<WashAfterSale> rows = afterSaleMapper.selectMyList(memberId, status);
        List<AfterSaleVO> out = new ArrayList<>();
        for (WashAfterSale row : rows) {
            out.add(toVO(row));
        }
        return out;
    }

    public List<AdminAfterSaleVO> adminList(String afterSaleNo, String type, String status, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return afterSaleMapper.selectAdminList(afterSaleNo, type, status, offset, pageSize);
    }

    public long countAdmin(String afterSaleNo, String type, String status) {
        return afterSaleMapper.countAdmin(afterSaleNo, type, status);
    }

    // ------------------------------------------------------------------ 处置

    /**
     * 后台处置工单（同意 / 驳回）。
     *
     * <p>同意退款在这里**只做两件事**：写处置留痕 + 发退款事件。本方法返回成功不代表钱已到账——
     * 钱到底退没退以退款单为准（RefundVO.status）。把"运营点了同意"当成"钱已退"是典型的财务事故。
     *
     * <p>REWASH / CLAIM 不自动退款：同意后置 PROCESSING，等人工作业闭环。
     */
    @Transactional(rollbackFor = Exception.class)
    public void handle(String afterSaleNo, boolean accept, String remark, String handler) {
        WashAfterSale sale = afterSaleMapper.selectByNo(afterSaleNo);
        if (sale == null) {
            throw new ApiException(ErrorCode.A0001, "售后工单不存在：" + afterSaleNo);
        }
        String text = remark == null ? null : remark.trim();
        if (!accept && (text == null || text.isEmpty())) {
            throw new ApiException(ErrorCode.A0001, "驳回必须填写处置说明，客户要看到为什么没通过");
        }

        String nextStatus = accept ? "PROCESSING" : "REJECTED";
        // SQL 自带 status='INIT' 做并发幂等：已被人处置过会返回 0 行，而不是静默覆盖前一个人的决定
        int rows = afterSaleMapper.updateHandle(afterSaleNo, nextStatus, handler, text, System.currentTimeMillis());
        if (rows == 0) {
            throw new ApiException(ErrorCode.A0001, "该工单已被处置，请刷新后重试");
        }

        if (accept && "REFUND".equals(sale.getType())) {
            String reason = (text == null || text.isEmpty()) ? sale.getReason() : text;
            eventPublisher.publishEvent(new AfterSaleRefundEvent(
                    sale.getAfterSaleNo(), sale.getOrderNo(), sale.getMemberId(), sale.getAmount(), reason));
        }
        log.info("[售后处置] afterSaleNo={} accept={} handler={} 工单类型={}", afterSaleNo, accept, handler, sale.getType());
    }

    /**
     * 退款到账 → 工单收尾为 DONE。
     *
     * <p>监听而不是让 wash-pay 直接改工单表：工单是订单域的数据，支付域不该知道它长什么样。
     * Mapper 里限定 Process 状态的 REFUND 工单，回调重发也不会把 REWASH 工单误标完成。
     */
    @EventListener
    public void onRefundSucceeded(RefundSucceededEvent event) {
        try {
            int rows = afterSaleMapper.finishByRefundSuccess(event.orderNo(), System.currentTimeMillis());
            if (rows > 0) {
                log.info("[售后] 退款到账，工单收尾 orderNo={} refundNo={}", event.orderNo(), event.refundNo());
            }
        } catch (Exception e) {
            // 吞掉：钱已经退了，工单收尾失败只影响运营视图，绝不能把异常抛回支付域
            log.error("[售后] 工单收尾失败（钱已退）orderNo={} refundNo={}", event.orderNo(), event.refundNo(), e);
        }
    }

    // ------------------------------------------------------------------ 内部

    /** 实体转 VO：出参一律不放实体，避免将来给实体加内部字段时被带出去 */
    private AfterSaleVO toVO(WashAfterSale row) {
        AfterSaleVO vo = new AfterSaleVO();
        vo.setAfterSaleNo(row.getAfterSaleNo());
        vo.setOrderNo(row.getOrderNo());
        vo.setType(row.getType());
        vo.setReason(row.getReason());
        vo.setStatus(row.getStatus());
        // 中文名由后端枚举给，前端只展示（与 OrderStatus 同一口径，不在前端维护第二份名单）
        vo.setStatusLabel(AfterSaleStatus.labelOf(row.getStatus()));
        vo.setAmount(row.getAmount());
        vo.setHandlerBy(row.getHandlerBy());
        vo.setHandleRemark(row.getHandleRemark());
        vo.setHandleTime(row.getHandleTime());
        vo.setCreateTime(row.getCreateTime());
        return vo;
    }
}
