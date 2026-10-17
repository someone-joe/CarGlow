package com.ruoyi.wash.pay.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wash.pay.dto.RefundVO;
import com.ruoyi.wash.pay.service.WashPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台退款单。契约：openapi.yaml /admin-api/wash/refund/**（tag admin-refund）。
 *
 * <p>这里是运营判断"钱到底退没退"的地方：**以退款单 status 为准，不看订单状态**。
 * 订单状态 REFUNDING 只表示在处理中，可能卡着等重试；只有退款单 SUCCESS 才是真的退了。
 *
 * <p>人工重试是最后一道手段：超过自动重试上限的单子才会留在这里，
 * 重试点一次就再走一次真实渠道，SUCCESS 的会被直接拒绝（重退等于重复打款）。
 */
@RestController
@RequestMapping("/admin-api/wash/refund")
public class AdminRefundController {

    @Autowired
    private WashPayService payService;

    @PreAuthorize("@ss.hasPermi('wash:refund:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String orderNo,
                              @RequestParam(required = false) String status,
                              @RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize) {
        List<RefundVO> rows = payService.adminList(orderNo, status, pageNum, pageSize);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(rows);
        rsp.setTotal(payService.countAdmin(orderNo, status));
        return rsp;
    }

    /** 人工重试一次退款（仍走真实渠道调用） */
    @PreAuthorize("@ss.hasPermi('wash:refund:edit')")
    @PostMapping("/{refundNo}/retry")
    public AjaxResult retry(@PathVariable String refundNo) {
        payService.manualRetry(refundNo);
        return AjaxResult.success();
    }
}
