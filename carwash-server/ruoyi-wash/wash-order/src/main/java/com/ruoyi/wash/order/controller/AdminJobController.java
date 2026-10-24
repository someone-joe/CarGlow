package com.ruoyi.wash.order.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.wash.common.service.JobTriggerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台手动触发定时任务（运维 / 接口自动化用）。契约见 openapi.yaml /admin-api/wash/job/trigger。
 * 权限 wash:job:trigger，与 @PreAuthorize、wash_menu.sql 三处一致。白名单在 JobTriggerService 内维护。
 */
@RestController
@RequestMapping("/admin-api/wash/job")
public class AdminJobController {

    @Autowired
    private JobTriggerService triggerService;

    @PreAuthorize("@ss.hasPermi('wash:job:trigger')")
    @PostMapping("/trigger")
    public AjaxResult trigger(@RequestBody JobTriggerRequest request) {
        return AjaxResult.success(triggerService.trigger(request.job(), request.params()));
    }

    /** 请求体：job 为白名单内的任务名；params 可选（refund-retry 支持 forceFail）。 */
    public record JobTriggerRequest(String job, Map<String, Object> params) {
    }
}
