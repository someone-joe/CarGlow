package com.ruoyi.wash.promo.service;

import com.ruoyi.wash.promo.domain.WashCouponTemplate;
import com.ruoyi.wash.promo.domain.WashCouponUser;
import com.ruoyi.wash.promo.mapper.WashCouponTemplateMapper;
import com.ruoyi.wash.promo.mapper.WashCouponUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 批量发放「单会员」发放器。
 *
 * <p>每个会员独立一个事务（REQUIRES_NEW）：一人失败只回滚该人、不影响他人，
 * 也与外层批量循环不在同一个事务里——避免「一个大事务要么全成要么全滚」导致
 * 一人库存不足就把整批已发券全部回滚。
 *
 * <p>校验与单人发放 {@link WashCouponAdminService#grant} 完全一致：每人限领 + 库存乐观扣减
 * （发不完明确失败，不超发）。
 */
@Service
public class CouponBatchGranter {

    public static final String SUCCESS = "SUCCESS";
    public static final String SKIPPED = "SKIPPED";
    public static final String FAILED = "FAILED";

    @Autowired
    private WashCouponTemplateMapper templateMapper;
    @Autowired
    private WashCouponUserMapper userMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public GrantOutcome grant(WashCouponTemplate t, Long memberId, int perCount, Long batchId) {
        long now = System.currentTimeMillis();
        int claimed = userMapper.countByTemplateAndMember(t.getCouponTemplateId(), memberId);
        int granted = 0;
        String result = null;
        String reason = null;
        for (int i = 0; i < perCount; i++) {
            if (claimed >= t.getPerLimit()) {
                result = SKIPPED;
                reason = "已达每人限领（" + t.getPerLimit() + "张）";
                break;
            }
            // 与 C 端领取共用库存扣减：issued < total 才 +1，发不完返回 0
            if (templateMapper.incrementIssued(t.getCouponTemplateId()) == 0) {
                result = FAILED;
                reason = "优惠券已领完（库存不足）";
                break;
            }
            WashCouponUser u = new WashCouponUser();
            u.setTemplateId(t.getCouponTemplateId());
            u.setMemberId(memberId);
            u.setBatchId(batchId);
            u.setStatus("UNUSED");
            u.setObtainTime(now);
            u.setExpireTime(t.getEndTime());
            userMapper.insert(u);
            granted++;
            claimed++;
        }
        if (granted > 0) {
            result = SUCCESS;
            // 部分发放：比如每人发 2 张，第 2 张因限领/库存没发出去
            if (granted < perCount && reason != null) {
                reason = "部分发放：" + granted + "/" + perCount + "（" + reason + "）";
            } else {
                reason = null;
            }
        } else if (result == null) {
            result = FAILED;
            reason = "发放失败";
        }
        return new GrantOutcome(result, granted, reason);
    }

    public record GrantOutcome(String result, int granted, String reason) {
    }
}
