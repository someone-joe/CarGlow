package com.ruoyi.wash.worker.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.network.domain.WashSite;
import com.ruoyi.wash.network.service.WashSiteQueryService;
import com.ruoyi.wash.worker.domain.WorkerStatus;
import com.ruoyi.wash.worker.dto.AdminWorkerVO;
import com.ruoyi.wash.worker.service.WashWorkerAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台师傅（工作人员）管理。契约：openapi.yaml 的 /admin-api/wash/worker/**（tag admin-worker）。
 *
 * <p>权限标识一律用 PRD 6.7.3 的 `wash:worker:*` 命名，与后端 @PreAuthorize、wash_menu.sql 三处一致。
 * 返回体沿用若依 AjaxResult / TableDataInfo，后台前端零改造即可渲染。
 */
@RestController
@RequestMapping("/admin-api/wash/worker")
public class AdminWorkerController {

    @Autowired
    private WashWorkerAdminService adminService;

    /** 站点下拉：跨模块只调对方 Service，不碰它的 Mapper */
    @Autowired
    private WashSiteQueryService siteQuery;

    @PreAuthorize("@ss.hasPermi('wash:worker:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) Long siteId,
                              @RequestParam(required = false) String status,
                              @RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize) {
        List<AdminWorkerVO> rows = adminService.list(keyword, siteId, status, pageNum, pageSize);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows(rows);
        rspData.setTotal(adminService.count(keyword, siteId, status));
        return rspData;
    }

    /** 状态下拉：来源是 WorkerStatus 枚举（唯一真源），前端不得自带一份状态名单 */
    @PreAuthorize("@ss.hasPermi('wash:worker:list')")
    @GetMapping("/status-options")
    public AjaxResult statusOptions() {
        List<Map<String, String>> options = new ArrayList<>();
        for (WorkerStatus s : WorkerStatus.values()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("value", s.name());
            item.put("label", s.getLabel());
            options.add(item);
        }
        return AjaxResult.success(options);
    }

    @PreAuthorize("@ss.hasPermi('wash:worker:list')")
    @GetMapping("/site-options")
    public AjaxResult siteOptions() {
        List<Map<String, Object>> options = new ArrayList<>();
        for (WashSite s : siteQuery.listSites()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", s.getSiteId());
            item.put("label", s.getSiteName());
            options.add(item);
        }
        return AjaxResult.success(options);
    }

    @PreAuthorize("@ss.hasPermi('wash:worker:list')")
    @GetMapping("/{workerId}")
    public AjaxResult get(@PathVariable Long workerId) {
        return AjaxResult.success(adminService.get(workerId));
    }

    @PreAuthorize("@ss.hasPermi('wash:worker:add')")
    @PostMapping
    public AjaxResult add(@RequestBody AdminWorkerVO worker) {
        return AjaxResult.success(adminService.add(worker));
    }

    @PreAuthorize("@ss.hasPermi('wash:worker:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody AdminWorkerVO worker) {
        adminService.edit(worker);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('wash:worker:remove')")
    @DeleteMapping("/{workerId}")
    public AjaxResult remove(@PathVariable Long workerId) {
        adminService.remove(workerId);
        return AjaxResult.success();
    }

    /** 重置密码：单独的权限，避免有编辑权的人顺手把师傅踢下线 */
    @PreAuthorize("@ss.hasPermi('wash:worker:reset')")
    @PostMapping("/{workerId}/reset-password")
    public AjaxResult resetPassword(@PathVariable Long workerId, @RequestBody Map<String, String> body) {
        String password = body == null ? null : body.get("password");
        if (password == null || password.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "新密码必填");
        }
        adminService.resetPassword(workerId, password);
        return AjaxResult.success();
    }
}
