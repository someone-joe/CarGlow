package com.ruoyi.wash.order.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.wash.order.dto.AdminAfterSaleVO;
import com.ruoyi.wash.order.service.WashAfterSaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台售后工单。契约：openapi.yaml /admin-api/wash/after-sale/**（tag admin-aftersale）。
 *
 * <p>这里只做「处置决定」——同意还是驳回，**不碰钱**。同意退款后由支付域的事件监听器
 * 真正走渠道退款，结果看退款单（AdminRefundController）。
 * 把两者分开，是因为"运营点了同意"绝不等于"钱已经退回去了"。
 */
@RestController
@RequestMapping("/admin-api/wash/after-sale")
public class AdminAfterSaleController {

    @Autowired
    private WashAfterSaleService afterSaleService;

    @PreAuthorize("@ss.hasPermi('wash:aftersale:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String afterSaleNo,
                              @RequestParam(required = false) String type,
                              @RequestParam(required = false) String status,
                              @RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize) {
        List<AdminAfterSaleVO> rows = afterSaleService.adminList(afterSaleNo, type, status, pageNum, pageSize);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(rows);
        rsp.setTotal(afterSaleService.countAdmin(afterSaleNo, type, status));
        return rsp;
    }

    /** 处置：同意（进入退款 / 转人工处理）或驳回（必须写明为什么） */
    @PreAuthorize("@ss.hasPermi('wash:aftersale:handle')")
    @PutMapping("/{afterSaleNo}/handle")
    public AjaxResult handle(@PathVariable String afterSaleNo,
                             @RequestBody(required = false) HandleRequest body) {
        afterSaleService.handle(afterSaleNo,
                body != null && Boolean.TRUE.equals(body.accept()),
                body == null ? null : body.remark(),
                SecurityUtils.getUsername());
        return AjaxResult.success();
    }

    /** 与契约 request body 逐字段一致 */
    public record HandleRequest(Boolean accept, String remark) {
    }
}
