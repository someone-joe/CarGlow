package com.ruoyi.wash.invoice.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wash.invoice.dto.InvoiceVO;
import com.ruoyi.wash.invoice.service.WashInvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 后台发票管理。契约：openapi.yaml /admin-api/wash/invoice/**。
 */
@RestController
public class AdminInvoiceController {

    @Autowired
    private WashInvoiceService service;

    @GetMapping("/admin-api/wash/invoice/status-options")
    @PreAuthorize("@ss.hasPermi('wash:invoice:list')")
    public AjaxResult statusOptions() {
        return AjaxResult.success(service.statusOptions());
    }

    @GetMapping("/admin-api/wash/invoice/list")
    @PreAuthorize("@ss.hasPermi('wash:invoice:list')")
    public TableDataInfo list(@RequestParam(required = false) Long memberId,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String orderNo,
                              @RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> data = service.adminPage(memberId, status, orderNo, pageNum, pageSize);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows((List<?>) data.get("rows"));
        rsp.setTotal(Long.parseLong(String.valueOf(data.get("total"))));
        return rsp;
    }

    @GetMapping("/admin-api/wash/invoice/{invoiceId}")
    @PreAuthorize("@ss.hasPermi('wash:invoice:list')")
    public AjaxResult detail(@PathVariable Long invoiceId) {
        return AjaxResult.success(service.adminDetail(invoiceId));
    }

    @PostMapping("/admin-api/wash/invoice/{invoiceId}/issue")
    @PreAuthorize("@ss.hasPermi('wash:invoice:audit')")
    public AjaxResult issue(@PathVariable Long invoiceId, @RequestBody(required = false) Map<String, String> body) {
        service.issue(invoiceId, body == null ? null : body.get("invoiceNo"));
        return AjaxResult.success("已开票");
    }

    @PostMapping("/admin-api/wash/invoice/{invoiceId}/reject")
    @PreAuthorize("@ss.hasPermi('wash:invoice:audit')")
    public AjaxResult reject(@PathVariable Long invoiceId, @RequestBody(required = false) Map<String, String> body) {
        service.reject(invoiceId, body == null ? null : body.get("reason"));
        return AjaxResult.success("已驳回");
    }
}
