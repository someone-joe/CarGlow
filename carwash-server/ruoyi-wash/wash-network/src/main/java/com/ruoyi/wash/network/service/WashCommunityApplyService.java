package com.ruoyi.wash.network.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.network.domain.WashCommunity;
import com.ruoyi.wash.network.domain.WashCommunityApply;
import com.ruoyi.wash.network.mapper.WashCommunityApplyMapper;
import com.ruoyi.wash.network.mapper.WashCommunityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 小区开通申请审核流（C 端提交 → 后台审核 → 通过即建小区并启用）。
 *
 * <p>审核通过：建一条 wash_community 记录（enabled=Y / accessAuth=Y），客户立刻可下单；
 * 申请记录回填 community_id 与 site_id，状态置 APPROVED。拒绝：状态置 REJECTED，原因落库。
 * 状态中文名由后端给出，前端不维护第二份映射。
 */
@Service
public class WashCommunityApplyService {

    private static final Logger log = LoggerFactory.getLogger(WashCommunityApplyService.class);

    @Autowired
    private WashCommunityApplyMapper applyMapper;

    @Autowired
    private WashCommunityMapper communityMapper;

    /** C 端提交申请（memberId 来自 MemberContext，不接收前端传入，防越权） */
    public Long submit(Long memberId, String communityName, String contactPhone) {
        if (communityName == null || communityName.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "小区名称必填");
        }
        WashCommunityApply apply = new WashCommunityApply();
        apply.setMemberId(memberId);
        apply.setCommunityName(communityName);
        apply.setContactPhone(contactPhone == null ? "" : contactPhone);
        applyMapper.insert(apply);
        return apply.getApplyId();
    }

    public List<WashCommunityApply> list(String keyword, String status, int pageNum, int pageSize) {
        List<WashCommunityApply> rows = applyMapper.selectList(keyword, status, (pageNum - 1) * pageSize, pageSize);
        for (WashCommunityApply a : rows) {
            a.setStatusLabel(labelOf(a.getStatus()));
        }
        return rows;
    }

    public int count(String keyword, String status) {
        return applyMapper.count(keyword, status);
    }

    public WashCommunityApply get(Long applyId) {
        WashCommunityApply apply = applyMapper.selectById(applyId);
        if (apply != null) {
            apply.setStatusLabel(labelOf(apply.getStatus()));
        }
        return apply;
    }

    /** 审核通过：建小区并直接启用；一次事务内完成申请状态更新与小区落库。 */
    @Transactional(rollbackFor = Exception.class)
    public Long approve(Long applyId, Long siteId, String communityNameOverride) {
        WashCommunityApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            throw new ApiException(ErrorCode.A0001, "申请不存在");
        }
        if (!"PENDING".equals(apply.getStatus())) {
            throw new ApiException(ErrorCode.A0001, "只有待审核的申请才能通过");
        }
        if (siteId == null) {
            throw new ApiException(ErrorCode.A0001, "归属站点必填");
        }
        WashCommunity community = new WashCommunity();
        community.setCommunityName(communityNameOverride != null && !communityNameOverride.isBlank()
                ? communityNameOverride : apply.getCommunityName());
        community.setSiteId(siteId);
        community.setAccessAuth("Y");
        community.setEnabled("Y");
        communityMapper.insert(community);

        apply.setStatus("APPROVED");
        apply.setCommunityId(community.getCommunityId());
        apply.setSiteId(siteId);
        applyMapper.update(apply);
        log.info("[小区开通审核] 通过 applyId={} → communityId={} siteId={}", applyId, community.getCommunityId(), siteId);
        return community.getCommunityId();
    }

    /** 审核拒绝：状态置 REJECTED，原因必填落库。 */
    public void reject(Long applyId, String reason) {
        WashCommunityApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            throw new ApiException(ErrorCode.A0001, "申请不存在");
        }
        if (!"PENDING".equals(apply.getStatus())) {
            throw new ApiException(ErrorCode.A0001, "只有待审核的申请才能拒绝");
        }
        if (reason == null || reason.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "拒绝原因必填");
        }
        apply.setStatus("REJECTED");
        apply.setRejectReason(reason);
        applyMapper.update(apply);
    }

    private static String labelOf(String status) {
        if ("APPROVED".equals(status)) {
            return "已通过";
        }
        if ("REJECTED".equals(status)) {
            return "已拒绝";
        }
        return "待审核";
    }
}
