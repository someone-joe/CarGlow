package com.ruoyi.wash.invoice.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.invoice.domain.InvoiceStatus;
import com.ruoyi.wash.invoice.domain.WashInvoice;
import com.ruoyi.wash.invoice.dto.InvoiceVO;
import com.ruoyi.wash.invoice.mapper.WashInvoiceMapper;
import com.ruoyi.wash.order.domain.WashOrder;
import com.ruoyi.wash.order.service.WashOrderPaySupportService;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.selfwash.domain.SelfWashStatus;
import com.ruoyi.wash.selfwash.domain.WashSelfwashOrder;
import com.ruoyi.wash.selfwash.service.WashSelfwashOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发票申请。按订单开票：金额取自订单实付（后端权威），前端不传金额。
 *
 * <p>订单归属校验跨两类单：无感在 wash-order、自助在 wash-selfwash；二者均不反向依赖本模块。
 */
@Service
public class WashInvoiceService {

    @Autowired
    private WashInvoiceMapper mapper;
    @Autowired
    private WashOrderPaySupportService orderSupport;
    @Autowired
    private WashSelfwashOrderService selfwashService;

    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long memberId, String orderNo, String titleType, String title,
                      String taxNo, String email, String remark) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "请选择要开票的订单");
        }
        if (title == null || title.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "发票抬头必填");
        }
        if (email == null || !email.contains("@")) {
            throw new ApiException(ErrorCode.A0001, "请填写正确的接收邮箱");
        }
        String type = (titleType == null || titleType.isBlank()) ? "PERSONAL" : titleType.trim();
        if ("COMPANY".equals(type) && (taxNo == null || taxNo.isBlank())) {
            throw new ApiException(ErrorCode.A0001, "企业开票必须填写税号");
        }
        if (mapper.selectApplyingByOrder(memberId, orderNo.trim()) != null) {
            throw new ApiException(ErrorCode.B1002, "该订单已申请开票，请勿重复提交");
        }
        long amount = resolveInvoiceableAmount(memberId, orderNo.trim());

        WashInvoice inv = new WashInvoice();
        inv.setMemberId(memberId);
        inv.setOrderNo(orderNo.trim());
        inv.setAmount(amount);
        inv.setTitleType(type);
        inv.setTitle(title.trim());
        inv.setTaxNo(taxNo == null ? null : taxNo.trim());
        inv.setEmail(email.trim());
        inv.setStatus(InvoiceStatus.APPLYING.name());
        inv.setRemark(remark);
        mapper.insert(inv);
        return inv.getInvoiceId();
    }

    public List<InvoiceVO> listByMember(Long memberId) {
        List<InvoiceVO> out = new ArrayList<>();
        for (WashInvoice i : mapper.selectByMember(memberId)) {
            out.add(toVO(i));
        }
        return out;
    }

    public InvoiceVO detail(Long memberId, Long invoiceId) {
        WashInvoice i = mapper.selectByMemberAndId(memberId, invoiceId);
        if (i == null) {
            throw new ApiException(ErrorCode.B1001, "发票申请不存在");
        }
        return toVO(i);
    }

    // ---------------- 后台 ----------------

    public Map<String, Object> adminPage(Long memberId, String status, String orderNo, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("status", status);
        params.put("orderNo", orderNo);
        params.put("offset", (pageNum - 1) * pageSize);
        params.put("limit", pageSize);
        Map<String, Object> data = new HashMap<>();
        List<InvoiceVO> rows = new ArrayList<>();
        for (WashInvoice i : mapper.selectAdminPage(params)) {
            rows.add(toVO(i));
        }
        data.put("rows", rows);
        data.put("total", mapper.countAdmin(params));
        return data;
    }

    public InvoiceVO adminDetail(Long invoiceId) {
        WashInvoice i = mapper.selectById(invoiceId);
        if (i == null) {
            throw new ApiException(ErrorCode.B1001, "发票申请不存在");
        }
        return toVO(i);
    }

    @Transactional(rollbackFor = Exception.class)
    public void issue(Long invoiceId, String invoiceNo) {
        WashInvoice i = requireApplying(invoiceId);
        i.setStatus(InvoiceStatus.ISSUED.name());
        i.setInvoiceNo(invoiceNo == null || invoiceNo.isBlank() ? null : invoiceNo.trim());
        i.setRejectReason(null);
        mapper.update(i);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long invoiceId, String reason) {
        WashInvoice i = requireApplying(invoiceId);
        if (reason == null || reason.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "驳回原因必填");
        }
        i.setStatus(InvoiceStatus.REJECTED.name());
        i.setRejectReason(reason.trim());
        mapper.update(i);
    }

    public List<Map<String, String>> statusOptions() {
        List<Map<String, String>> out = new ArrayList<>();
        for (InvoiceStatus s : InvoiceStatus.values()) {
            out.add(Map.of("value", s.name(), "label", s.getLabel()));
        }
        return out;
    }

    // ---------------- 内部 ----------------

    /** 解析可开票订单金额：先按无感单查，再按自助单查；都不存在报错。 */
    private long resolveInvoiceableAmount(Long memberId, String orderNo) {
        WashOrder o = null;
        try {
            o = orderSupport.requireOwned(orderNo, memberId);
        } catch (ApiException e) {
            // 不是本人无感单（或不存在），继续按自助单找
        }
        if (o != null) {
            OrderStatus st = OrderStatus.of(o.getStatus());
            if (st == OrderStatus.WAIT_PAY) {
                throw new ApiException(ErrorCode.B1002, "订单未支付，支付后可申请开票");
            }
            if (st == OrderStatus.CANCELED || st == OrderStatus.REFUNDING || st == OrderStatus.REFUNDED) {
                throw new ApiException(ErrorCode.B1002, "订单已取消/退款，不可开票");
            }
            return o.getPayAmount() == null ? 0L : o.getPayAmount();
        }
        WashSelfwashOrder s = selfwashService.findOwned(memberId, orderNo);
        if (s != null) {
            SelfWashStatus sst = SelfWashStatus.of(s.getStatus());
            if (sst == SelfWashStatus.WAIT_PAY) {
                throw new ApiException(ErrorCode.B1002, "订单未支付，支付后可申请开票");
            }
            if (sst == SelfWashStatus.CANCELED) {
                throw new ApiException(ErrorCode.B1002, "订单已取消，不可开票");
            }
            return s.getPayAmount() == null ? 0L : s.getPayAmount();
        }
        throw new ApiException(ErrorCode.B1001, "订单不存在或不属于本人");
    }

    private WashInvoice requireApplying(Long invoiceId) {
        WashInvoice i = mapper.selectById(invoiceId);
        if (i == null) {
            throw new ApiException(ErrorCode.B1001, "发票申请不存在");
        }
        if (!InvoiceStatus.APPLYING.name().equals(i.getStatus())) {
            throw new ApiException(ErrorCode.B1002, "仅申请中的发票可处理，当前：" + label(i.getStatus()));
        }
        return i;
    }

    private InvoiceVO toVO(WashInvoice i) {
        InvoiceVO vo = new InvoiceVO();
        vo.setInvoiceId(i.getInvoiceId());
        vo.setMemberId(i.getMemberId());
        vo.setOrderNo(i.getOrderNo());
        vo.setAmount(i.getAmount());
        vo.setTitleType(i.getTitleType());
        vo.setTitle(i.getTitle());
        vo.setTaxNo(i.getTaxNo());
        vo.setEmail(i.getEmail());
        vo.setStatus(i.getStatus());
        vo.setStatusLabel(label(i.getStatus()));
        vo.setInvoiceNo(i.getInvoiceNo());
        vo.setRejectReason(i.getRejectReason());
        vo.setRemark(i.getRemark());
        vo.setCreateTime(i.getCreateTime());
        return vo;
    }

    private String label(String status) {
        try {
            return InvoiceStatus.of(status).getLabel();
        } catch (IllegalArgumentException e) {
            return status;
        }
    }
}
