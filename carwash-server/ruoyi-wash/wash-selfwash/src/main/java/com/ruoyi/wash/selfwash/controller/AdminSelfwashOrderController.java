package com.ruoyi.wash.selfwash.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.wash.selfwash.dto.SelfwashOrderVO;
import com.ruoyi.wash.selfwash.service.WashSelfwashOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台自助洗车订单监控。契约：openapi.yaml /admin-api/wash/selfwash/order/**。
 *
 * <p>只读监控 + 强制结束异常单（卡在服务中的单），不散落状态更新。
 */
@RestController
public class AdminSelfwashOrderController {

    @Autowired
    private WashSelfwashOrderService service;

    @GetMapping("/admin-api/wash/selfwash/order/status-options")
    @PreAuthorize("@ss.hasPermi('wash:selfwash:order:list')")
    public AjaxResult statusOptions() {
        return AjaxResult.success(service.statusOptions());
    }

    @GetMapping("/admin-api/wash/selfwash/order/list")
    @PreAuthorize("@ss.hasPermi('wash:selfwash:order:list')")
    public AjaxResult list(@RequestParam(required = false) Long memberId,
                           @RequestParam(required = false) Long siteId,
                           @RequestParam(required = false) Long workstationId,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String orderNo,
                           @RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "10") int pageSize) {
        return AjaxResult.success(service.adminPage(memberId, siteId, workstationId, status, orderNo, pageNum, pageSize));
    }

    @GetMapping("/admin-api/wash/selfwash/order/{orderNo}")
    @PreAuthorize("@ss.hasPermi('wash:selfwash:order:list')")
    public AjaxResult detail(@PathVariable String orderNo) {
        return AjaxResult.success(service.adminDetail(orderNo));
    }

    @PostMapping("/admin-api/wash/selfwash/order/{orderNo}/finish")
    @PreAuthorize("@ss.hasPermi('wash:selfwash:order:finish')")
    public AjaxResult finish(@PathVariable String orderNo) {
        service.adminForceFinish(orderNo);
        return AjaxResult.success("已强制结束");
    }
}
