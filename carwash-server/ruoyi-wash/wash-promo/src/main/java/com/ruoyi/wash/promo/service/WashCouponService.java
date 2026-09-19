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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** C 端优惠券：领券中心、领取、我的券、下单最优券计算与核销。 */
@Service
public class WashCouponService {

    @Autowired
    private WashCouponTemplateMapper templateMapper;
    @Autowired
    private WashCouponUserMapper userMapper;

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
    public List<CouponUserVO> myCoupons(Long memberId, String status, long now) {
        List<CouponUserVO> vos = new ArrayList<>();
        for (WashCouponUser u : userMapper.selectMy(memberId, status)) {
            WashCouponTemplate t = templateMapper.selectById(u.getTemplateId());
            vos.add(toUserVO(u, t));
        }
        return vos;
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

    private CouponUserVO toUserVO(WashCouponUser u, WashCouponTemplate t) {
        String name = t == null ? "" : t.getName();
        Long threshold = t == null ? 0L : t.getThresholdAmount();
        Long discount = t == null ? 0L : t.getDiscountAmount();
        return new CouponUserVO(u.getCouponUserId(), u.getTemplateId(), name,
                threshold, discount, u.getStatus(), u.getExpireTime(), u.getOrderNo());
    }
}
