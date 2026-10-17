package com.ruoyi.wash.promo.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.promo.domain.WashCouponTemplate;
import com.ruoyi.wash.promo.domain.WashCouponUser;
import com.ruoyi.wash.promo.dto.AdminCouponRecordVO;
import com.ruoyi.wash.promo.dto.AdminCouponTemplateVO;
import com.ruoyi.wash.promo.mapper.WashCouponTemplateMapper;
import com.ruoyi.wash.promo.mapper.WashCouponUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 后台优惠券管理：券模板 CRUD、上下架、发放、领用/核销记录。
 *
 * <p>与 C 端 {@link WashCouponService} 的关系：**发放与领取共用同一套库存扣减**
 * （`incrementIssued` 乐观更新 + 插入用户券），后台发放不是另一条路。
 * 否则迟早出现"后台发了 100 张但库存没扣，C 端还能再领 100 张"这种事故。
 */
@Service
public class WashCouponAdminService {

    /** 券类型：与 C 端 / 种子数据一致，前端不自行发明类型 */
    private static final Set<String> TYPES = Set.of("NORMAL", "NEWBIE", "INVITE");

    @Autowired
    private WashCouponTemplateMapper templateMapper;
    @Autowired
    private WashCouponUserMapper userMapper;

    // ------------------------------------------------------------------ 模板

    public Map<String, Object> page(String name, String type, String status, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<AdminCouponTemplateVO> rows = templateMapper.selectAdminList(name, type, status, offset, pageSize);
        for (AdminCouponTemplateVO vo : rows) {
            int issued = vo.getIssued() == null ? 0 : vo.getIssued();
            int total = vo.getTotal() == null ? 0 : vo.getTotal();
            // remain 由后端算好下发：前端自己减会在"运营刚改完 total、前端还没刷新"时出错
            vo.setRemain(Math.max(0, total - issued));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        data.put("total", templateMapper.countAdmin(name, type, status));
        return data;
    }

    public AdminCouponTemplateVO get(Long templateId) {
        WashCouponTemplate t = require(templateId);
        return toVO(t);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long add(WashCouponTemplate template) {
        check(template, true);
        if (template.getStatus() == null) {
            template.setStatus("Y");
        }
        template.setIssued(0);
        templateMapper.insert(template);
        return template.getCouponTemplateId();
    }

    /**
     * 改资料。**不接受 total / issued**：模板一旦发行过，这两列就是历史账，
     * 改了会让"已领 / 剩余"与真实发出的券对不上。要加量请发新模板。
     */
    @Transactional(rollbackFor = Exception.class)
    public void edit(WashCouponTemplate template) {
        if (template.getCouponTemplateId() == null) {
            throw new ApiException(ErrorCode.C1001, "缺少 couponTemplateId");
        }
        require(template.getCouponTemplateId());
        // total 不由这里改，所以编辑时不校验它（校验了反而逼着前端把总量一起传上来）
        check(template, false);
        if (templateMapper.updateTemplate(template) == 0) {
            throw new ApiException(ErrorCode.C1001, "券模板不存在或已删除");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long templateId, String status) {
        if (!"Y".equals(status) && !"N".equals(status)) {
            throw new ApiException(ErrorCode.A0001, "status 只能是 Y 上架 / N 下架");
        }
        require(templateId);
        if (templateMapper.updateStatus(templateId, status) == 0) {
            throw new ApiException(ErrorCode.C1001, "券模板不存在或已删除");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long templateId) {
        require(templateId);
        // 逻辑删除：已发出去的券还引用着模板名与金额快照，不做物理删
        templateMapper.logicDelete(templateId);
    }

    // ------------------------------------------------------------------ 发放

    /**
     * 给指定会员发券（运营手工发放：补偿、线下活动）。
     *
     * <p>校验与 C 端自助领取完全一致：上架、在有效期、未领完（乐观扣库存）、
     * 未超每人限领。**perLimit 对后台发放一样算数**，否则等于绕过了限领规则，
     * 出现"同一个号领了 20 张"的对不上账。
     */
    @Transactional(rollbackFor = Exception.class)
    public void grant(Long templateId, Long memberId) {
        if (templateId == null || memberId == null) {
            throw new ApiException(ErrorCode.A0001, "templateId 与 memberId 必填");
        }
        WashCouponTemplate t = require(templateId);
        long now = System.currentTimeMillis();
        if (!"Y".equals(t.getStatus()) || t.getStartTime() > now || t.getEndTime() < now) {
            throw new ApiException(ErrorCode.C1001, "券模板未上架或不在有效期内，不能发放");
        }
        int claimed = userMapper.countByTemplateAndMember(templateId, memberId);
        if (claimed >= t.getPerLimit()) {
            throw new ApiException(ErrorCode.C1002, "该会员已达本券每人限领上限");
        }
        // 与 C 端领取共用同一个库存扣减：issued < total 才 +1，发不完明确报错，不超发
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
    }

    // ------------------------------------------------------------------ 记录

    public Map<String, Object> records(Long templateId, String phone, String status, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<AdminCouponRecordVO> rows =
                userMapper.selectAdminRecords(templateId, phone, status, System.currentTimeMillis(), offset, pageSize);
        Map<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        data.put("total", userMapper.countAdminRecords(templateId, phone, status, System.currentTimeMillis()));
        return data;
    }

    // ------------------------------------------------------------------ 内部

    private WashCouponTemplate require(Long templateId) {
        if (templateId == null) {
            throw new ApiException(ErrorCode.A0001, "templateId 必填");
        }
        WashCouponTemplate t = templateMapper.selectById(templateId);
        if (t == null) {
            throw new ApiException(ErrorCode.C1001, "券模板不存在");
        }
        return t;
    }

    private AdminCouponTemplateVO toVO(WashCouponTemplate t) {
        AdminCouponTemplateVO vo = new AdminCouponTemplateVO();
        vo.setCouponTemplateId(t.getCouponTemplateId());
        vo.setName(t.getName());
        vo.setType(t.getType());
        vo.setThresholdAmount(t.getThresholdAmount());
        vo.setDiscountAmount(t.getDiscountAmount());
        vo.setTotal(t.getTotal());
        vo.setIssued(t.getIssued());
        int issued = t.getIssued() == null ? 0 : t.getIssued();
        int total = t.getTotal() == null ? 0 : t.getTotal();
        vo.setRemain(Math.max(0, total - issued));
        vo.setPerLimit(t.getPerLimit());
        vo.setStartTime(t.getStartTime());
        vo.setEndTime(t.getEndTime());
        vo.setStatus(t.getStatus());
        return vo;
    }

    /** 公共校验：与前端必填项一致，服务端必须再守一次（前端校验不是防线） */
    private void check(WashCouponTemplate t, boolean requireTotal) {
        if (t.getName() == null || t.getName().trim().isEmpty()) {
            throw new ApiException(ErrorCode.A0001, "券名称必填");
        }
        if (t.getType() == null || !TYPES.contains(t.getType())) {
            throw new ApiException(ErrorCode.A0001, "券类型只支持 NORMAL / NEWBIE / INVITE");
        }
        if (t.getDiscountAmount() == null || t.getDiscountAmount() <= 0) {
            throw new ApiException(ErrorCode.A0001, "减免金额必填且大于 0（单位：分）");
        }
        if (t.getThresholdAmount() == null || t.getThresholdAmount() < 0) {
            throw new ApiException(ErrorCode.A0001, "使用门槛不能为负（0 = 无门槛，单位：分）");
        }
        // 门槛券的减免不能超过门槛，否则等于平台倒贴钱
        if (t.getThresholdAmount() > 0 && t.getDiscountAmount() > t.getThresholdAmount()) {
            throw new ApiException(ErrorCode.A0001, "减免金额不能大于使用门槛，否则等于倒贴");
        }
        // 只有新增校验总量：编辑接口刻意不接受 total（发行过的模板，总量就是历史账）
        if (requireTotal && (t.getTotal() == null || t.getTotal() <= 0)) {
            throw new ApiException(ErrorCode.A0001, "发行总量必填且大于 0");
        }
        if (t.getPerLimit() == null || t.getPerLimit() <= 0) {
            throw new ApiException(ErrorCode.A0001, "每人限领必填且大于 0");
        }
        if (t.getStartTime() == null || t.getEndTime() == null) {
            throw new ApiException(ErrorCode.A0001, "生效与失效时间必填");
        }
        if (t.getStartTime() >= t.getEndTime()) {
            throw new ApiException(ErrorCode.A0001, "生效时间必须早于失效时间");
        }
    }
}
