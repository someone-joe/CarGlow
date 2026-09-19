package com.ruoyi.wash.order.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.order.domain.WashAfterSale;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.domain.WashReview;
import com.ruoyi.wash.order.mapper.WashAfterSaleMapper;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import com.ruoyi.wash.order.mapper.WashReviewMapper;
import com.ruoyi.wash.order.state.WashOrderStateService;
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
}
