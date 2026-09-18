package com.ruoyi.wash.order.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.wash.order.dto.DashboardStatsVO;
import com.ruoyi.wash.order.service.WashOrderStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台首页看板接口。纯只读聚合，不做任何写操作（因此无需留痕）。
 *
 * <p>权限标识沿用 PRD 6.7.3 的 wash:*:* 命名（CODEBUDDY 第 4 节）。
 */
@RestController
@RequestMapping("/admin-api/wash/dashboard")
public class AdminDashboardController {

    @Autowired
    private WashOrderStatsService statsService;

    /** 今日单量 / 在洗数 / 待存钥匙 / 异常数 + 状态分布 */
    @PreAuthorize("@ss.hasPermi('wash:dashboard:list')")
    @GetMapping("/stats")
    public AjaxResult stats() {
        DashboardStatsVO vo = statsService.stats();
        return AjaxResult.success(vo);
    }
}
