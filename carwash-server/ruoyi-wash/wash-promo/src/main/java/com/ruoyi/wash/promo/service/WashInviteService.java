package com.ruoyi.wash.promo.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.member.service.WashMemberQueryService;
import com.ruoyi.wash.promo.domain.WashCouponTemplate;
import com.ruoyi.wash.promo.domain.WashInvite;
import com.ruoyi.wash.promo.dto.CouponUserVO;
import com.ruoyi.wash.promo.dto.InviteListVO;
import com.ruoyi.wash.promo.dto.InviteVO;
import com.ruoyi.wash.promo.mapper.WashCouponTemplateMapper;
import com.ruoyi.wash.promo.mapper.WashInviteMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 邀请好友（推荐有礼，需求 8.2）。
 *
 * <p>规则：邀请人生成邀请码；被邀请人绑定后得「新人券」作首单优惠；
 * 被邀请人首次成单（支付成功）后，给邀请人发一张 INVITE 类型奖励券（如 10 元）。
 *
 * <p>依赖方向：本模块在 wash-promo（营销域），依赖 wash-member 取邀请码；
 * 发奖由订单域在「支付成功」处调用 {@link #onFirstPaidOrder}（wash-order → wash-promo，无环）。
 */
@Service
public class WashInviteService {

    private static final Logger log = LoggerFactory.getLogger(WashInviteService.class);

    /** 被邀请人首单优惠：复用 NEWBIE 新人券 */
    private static final String TYPE_NEWBIE = "NEWBIE";
    /** 邀请人奖励券类型 */
    private static final String TYPE_INVITE = "INVITE";

    @Autowired
    private WashInviteMapper inviteMapper;
    @Autowired
    private WashCouponTemplateMapper templateMapper;
    @Autowired
    private WashCouponService couponService;
    @Autowired
    private WashMemberQueryService memberQuery;

    /** 我的邀请码（首次调用惰性生成并落库） */
    public String myCode(Long memberId) {
        return memberQuery.ensureInviteCode(memberId);
    }

    /** 绑定邀请关系（被邀请人调用）。绑定后尝试给被邀请人发新人券作首单优惠。 */
    @Transactional(rollbackFor = Exception.class)
    public void bind(Long memberId, String code) {
        if (code == null || code.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "请填写邀请码");
        }
        Long inviterId = memberQuery.findMemberIdByInviteCode(code);
        if (inviterId == null) {
            throw new ApiException(ErrorCode.A0001, "邀请码无效");
        }
        if (inviterId.equals(memberId)) {
            throw new ApiException(ErrorCode.A0001, "不能邀请自己");
        }
        if (inviteMapper.selectByInvitee(memberId) != null) {
            throw new ApiException(ErrorCode.B1002, "你已被邀请过");
        }
        WashInvite inv = new WashInvite();
        inv.setInviterMemberId(inviterId);
        inv.setInviteeMemberId(memberId);
        inv.setStatus("BOUND");
        inviteMapper.insert(inv);
        grantNewbieBestEffort(memberId);
    }

    /**
     * 被邀请人首次成单：给邀请人发奖励券。
     *
     * <p>幂等：关系状态 BOUND → REWARDED 条件更新；未邀请/已发奖直接返回。
     * 调用方应在 try/catch 中 best-effort 调用，发券失败不影响订单支付。
     */
    @Transactional(rollbackFor = Exception.class)
    public void onFirstPaidOrder(Long memberId, String orderNo) {
        WashInvite inv = inviteMapper.selectByInvitee(memberId);
        if (inv == null || !"BOUND".equals(inv.getStatus())) {
            return;
        }
        long now = System.currentTimeMillis();
        WashCouponTemplate t = templateMapper.selectAvailableByType(TYPE_INVITE, now);
        if (t == null) {
            log.warn("[邀请] 未配置 {} 奖励券模板，跳过发奖 order={}", TYPE_INVITE, orderNo);
            return;
        }
        CouponUserVO coupon = couponService.receive(inv.getInviterMemberId(), t.getCouponTemplateId(), now);
        inviteMapper.markRewarded(inv.getInviteId(), orderNo, coupon.couponUserId());
        log.info("[邀请] 首单发奖 order={} inviter={} coupon={}", orderNo, inv.getInviterMemberId(), coupon.couponUserId());
    }

    /** 我邀请的人 + 统计 */
    public InviteListVO myInvites(Long memberId) {
        List<InviteVO> list = new ArrayList<>();
        for (WashInvite i : inviteMapper.selectByInviter(memberId)) {
            list.add(toVO(i));
        }
        InviteListVO vo = new InviteListVO();
        vo.setList(list);
        vo.setInvitedCount(list.size());
        vo.setRewardedCount((int) inviteMapper.countRewarded(memberId));
        return vo;
    }

    /** 被邀请人首单优惠：发新人券；已有（达限领）则忽略，不影响绑定结果 */
    private void grantNewbieBestEffort(Long memberId) {
        try {
            long now = System.currentTimeMillis();
            WashCouponTemplate t = templateMapper.selectAvailableByType(TYPE_NEWBIE, now);
            if (t == null) {
                return;
            }
            couponService.receive(memberId, t.getCouponTemplateId(), now);
        } catch (Exception e) {
            log.warn("[邀请] 被邀请人新人券发放失败（忽略）member={}", memberId, e);
        }
    }

    private InviteVO toVO(WashInvite i) {
        InviteVO vo = new InviteVO();
        vo.setInviteId(i.getInviteId());
        vo.setInviteeMemberId(i.getInviteeMemberId());
        vo.setStatus(i.getStatus());
        vo.setStatusLabel("REWARDED".equals(i.getStatus()) ? "已发奖" : "待首单");
        vo.setFirstOrderNo(i.getFirstOrderNo());
        vo.setCreateTime(i.getCreateTime());
        return vo;
    }
}
