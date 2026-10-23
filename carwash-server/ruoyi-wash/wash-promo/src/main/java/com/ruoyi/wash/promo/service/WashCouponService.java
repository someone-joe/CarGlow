package com.ruoyi.wash.promo.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.promo.domain.WashCouponTemplate;
import com.ruoyi.wash.promo.domain.WashCouponUser;
import com.ruoyi.wash.promo.dto.CouponBestVO;
import com.ruoyi.wash.promo.dto.CouponTemplateVO;
import com.ruoyi.wash.promo.dto.CouponUserVO;
import com.ruoyi.wash.promo.mapper.WashCouponTemplateMapper;
import com.ruoyi.wash.promo.mapper.WashCouponUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** C 端优惠券：领券中心、领取、我的券、下单最优券计算与核销。 */
@Service
public class WashCouponService {

    private static final Logger log = LoggerFactory.getLogger(WashCouponService.class);

    @Autowired
    private WashCouponTemplateMapper templateMapper;
    @Autowired
    private WashCouponUserMapper userMapper;

    /**
     * 取消订单返还券：把该单核销的券置回 UNUSED。
     *
     * <p>依据：订单状态机规则表 2.3「取消副作用：权益（券/体验名额）返还」。
     * 返还失败不能阻断取消流程（券是附属权益，主流程是状态与退款），故静默兜底 + 留日志；
     * 幂等：无券或已返还都直接返回。
     */
    public void returnByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return;
        }
        WashCouponUser used = userMapper.selectUsedByOrderNo(orderNo);
        if (used == null) {
            return;
        }
        userMapper.updateUnused(used.getCouponUserId());
        log.info("[券返还] orderNo={} couponUserId={} memberId={} 已置回 UNUSED",
                orderNo, used.getCouponUserId(), used.getMemberId());
    }

    /** 领券中心：返回当前可领的模板 + 该会员已领数量 + 剩余库存。 */
    public List<CouponTemplateVO> center(Long memberId, long now) {
        List<CouponTemplateVO> vos = new ArrayList<>();
        for (WashCouponTemplate t : templateMapper.selectAvailable(now)) {
            int claimed = userMapper.countByTemplateAndMember(t.getCouponTemplateId(), memberId);
            int remain = Math.max(0, t.getTotal() - (t.getIssued() == null ? 0 : t.getIssued()));
            vos.add(new CouponTemplateVO(t.getCouponTemplateId(), t.getName(), t.getType(),
                    t.getThresholdAmount(), t.getDiscountAmount(), t.getPerLimit(),
                    t.getEndTime(), remain, claimed));
        }
        return vos;
    }

    /** 领取优惠券：校验上架/有效期/每人限领/库存，原子扣减库存后落用户券。 */
    public CouponUserVO receive(Long memberId, Long templateId, long now) {
        WashCouponTemplate t = templateMapper.selectById(templateId);
        if (t == null || !"Y".equals(t.getStatus()) || t.getStartTime() > now || t.getEndTime() < now) {
            throw new ApiException(ErrorCode.C1001);
        }
        int claimed = userMapper.countByTemplateAndMember(templateId, memberId);
        if (claimed >= t.getPerLimit()) {
            throw new ApiException(ErrorCode.C1002);
        }
        if (templateMapper.incrementIssued(templateId) == 0) {
            throw new ApiException(ErrorCode.C1001, "优惠券已领完");
        }
        WashCouponUser u = new WashCouponUser();
        u.setTemplateId(templateId);
        u.setMemberId(memberId);
        u.setStatus("UNUSED");
        u.setObtainTime(now);
        u.setExpireTime(t.getEndTime());
        userMapper.insert(u);
        return toUserVO(u, t);
    }

    /** 我的优惠券：按状态过滤（UNUSED/USED/EXPIRED），null 查全部。 */
    /**
     * 我的优惠券。
     *
     * <p>EXPIRED 是**计算值**而不是库里状态：库里只存 UNUSED / USED，过期的 UNUSED 要展示成 EXPIRED。
     * 契约（openapi CouponUserVO.status 枚举与 /api/v1/coupons 的 status 过滤）已明确包含 EXPIRED，
     * 2026-10-24 修复：此前直接返回库里原值，导致过期券仍显示 UNUSED、按 EXPIRED 过滤恒为空。
     */
    public List<CouponUserVO> myCoupons(Long memberId, String status, long now) {
        List<CouponUserVO> vos = new ArrayList<>();
        // 过滤参数若是 EXPIRED，库里没有这个状态，需先取 UNUSED 再按有效期计算
        String dbStatus = "EXPIRED".equals(status) ? "UNUSED" : status;
        for (WashCouponUser u : userMapper.selectMy(memberId, dbStatus)) {
            WashCouponTemplate t = templateMapper.selectById(u.getTemplateId());
            String effective = effectiveStatus(u, now);
            if (status != null && !status.equals(effective)) {
                continue; // 库状态与计算状态不一致时以计算值为准过滤（UNUSED 过滤要排除已过期的）
            }
            vos.add(toUserVO(u, t, effective));
        }
        return vos;
    }

    /** 券的对外状态：UNUSED 且已过有效期 → EXPIRED（库里仍是 UNUSED，不改数据）。 */
    private String effectiveStatus(WashCouponUser u, long now) {
        if ("UNUSED".equals(u.getStatus()) && u.getExpireTime() != null && u.getExpireTime() < now) {
            return "EXPIRED";
        }
        return u.getStatus();
    }

    /** 给定支付金额，计算当前会员可用的最优券（门槛满足、未过期、减免最大）。 */
    public CouponBestVO best(Long memberId, long payAmount, long now) {
        WashCouponUser best = null;
        long bestDiscount = -1;
        for (WashCouponUser u : userMapper.selectMy(memberId, "UNUSED")) {
            if (u.getExpireTime() != null && u.getExpireTime() < now) {
                continue;
            }
            WashCouponTemplate t = templateMapper.selectById(u.getTemplateId());
            if (t == null || t.getThresholdAmount() == null || t.getThresholdAmount() > payAmount) {
                continue;
            }
            if (t.getDiscountAmount() != null && t.getDiscountAmount() > bestDiscount) {
                bestDiscount = t.getDiscountAmount();
                best = u;
            }
        }
        return best == null ? null : new CouponBestVO(best.getCouponUserId(), bestDiscount);
    }

    /** 核销：下单支付成功时调用，把券置为已用（仅 UNUSED 生效）。 */
    public void markUsed(Long couponUserId, String orderNo, long now) {
        userMapper.updateUsed(couponUserId, orderNo, now);
    }

    /**
     * 下单用券：校验归属/状态/有效期/门槛，扣减应付金额后核销（同一事务内由调用方落库）。
     * @return 抵扣金额（分），调用方用于扣减 payAmount
     */
    public long applyToOrder(Long memberId, Long couponUserId, long payAmount, String orderNo, long now) {
        WashCouponUser u = userMapper.selectById(couponUserId);
        if (u == null || !memberId.equals(u.getMemberId())) {
            throw new ApiException(ErrorCode.C1003, "优惠券不存在或不属于本人");
        }
        if (!"UNUSED".equals(u.getStatus())) {
            throw new ApiException(ErrorCode.C1003, "优惠券已使用或已失效");
        }
        if (u.getExpireTime() != null && u.getExpireTime() < now) {
            throw new ApiException(ErrorCode.C1003, "优惠券已过期");
        }
        WashCouponTemplate t = templateMapper.selectById(u.getTemplateId());
        if (t == null || t.getThresholdAmount() == null || t.getThresholdAmount() > payAmount) {
            throw new ApiException(ErrorCode.C1003, "优惠券不满足使用门槛");
        }
        long discount = t.getDiscountAmount() == null ? 0 : t.getDiscountAmount();
        if (discount <= 0) {
            throw new ApiException(ErrorCode.C1003, "优惠券无可用减免");
        }
        userMapper.updateUsed(couponUserId, orderNo, now);
        return discount;
    }

    private CouponUserVO toUserVO(WashCouponUser u, WashCouponTemplate t) {
        return toUserVO(u, t, u.getStatus());
    }

    private CouponUserVO toUserVO(WashCouponUser u, WashCouponTemplate t, String status) {
        String name = t == null ? "" : t.getName();
        Long threshold = t == null ? 0L : t.getThresholdAmount();
        Long discount = t == null ? 0L : t.getDiscountAmount();
        return new CouponUserVO(u.getCouponUserId(), u.getTemplateId(), name,
                threshold, discount, status, u.getExpireTime(), u.getOrderNo());
    }
}
