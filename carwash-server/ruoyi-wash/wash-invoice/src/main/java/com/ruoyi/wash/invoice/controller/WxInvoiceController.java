package com.ruoyi.wash.invoice.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.invoice.dto.InvoiceVO;
import com.ruoyi.wash.invoice.service.WashInvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端发票。契约：openapi.yaml /api/v1/invoices**。
 *
 * <p>金额由后端按订单实付解析，前端不传金额。
 */
@RestController
public class WxInvoiceController {

    @Autowired
    private WashInvoiceService service;

    @PostMapping("/api/v1/invoices")
    public ApiResult<Long> apply(@RequestBody(required = false) ApplyRequest req) {
        if (req == null) {
            throw new ApiException(ErrorCode.A0001, "参数不完整");
        }
        return ApiResult.ok(service.apply(MemberContext.require(), req.orderNo(), req.titleType(),
                req.title(), req.taxNo(), req.email(), req.remark()));
    }

    @GetMapping("/api/v1/invoices")
    public ApiResult<List<InvoiceVO>> list() {
        return ApiResult.ok(service.listByMember(MemberContext.require()));
    }

    @GetMapping("/api/v1/invoices/{invoiceId}")
    public ApiResult<InvoiceVO> detail(@PathVariable Long invoiceId) {
        return ApiResult.ok(service.detail(MemberContext.require(), invoiceId));
    }

    public record ApplyRequest(String orderNo, String titleType, String title,
                               String taxNo, String email, String remark) {
    }
}
