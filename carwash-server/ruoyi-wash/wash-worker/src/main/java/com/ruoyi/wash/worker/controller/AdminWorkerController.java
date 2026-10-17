package com.ruoyi.wash.worker.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.network.domain.WashSite;
import com.ruoyi.wash.network.service.WashSiteQueryService;
import com.ruoyi.wash.worker.domain.WashWorker;
import com.ruoyi.wash.worker.domain.WashWorkerSchedule;
import com.ruoyi.wash.worker.domain.WorkerStatus;
import com.ruoyi.wash.worker.dto.AdminWorkerVO;
import com.ruoyi.wash.worker.service.WashDispatchService;
import com.ruoyi.wash.worker.service.WashWorkerAdminService;
import com.ruoyi.wash.worker.service.WashWorkerQualificationService;
import com.ruoyi.wash.worker.service.WashWorkerScheduleService;
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

    @Autowired
    private WashWorkerQualificationService qualificationService;

    @Autowired
    private WashWorkerScheduleService scheduleService;

    @Autowired
    private WashDispatchService dispatchService;

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

    // ---------------- 资质审核（T13） ----------------

    /** 待审核资质分页：默认看 PENDING */
    @PreAuthorize("@ss.hasPermi('wash:worker:audit')")
    @GetMapping("/qualification/page")
    public TableDataInfo qualificationPage(@RequestParam(defaultValue = "PENDING") String reviewStatus,
                                           @RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> data = qualificationService.adminPage(reviewStatus, pageNum, pageSize);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows((List<?>) data.get("rows"));
        rspData.setTotal(Long.parseLong(String.valueOf(data.get("total"))));
        return rspData;
    }

    /** 审核资质：PASSED / REJECTED（驳回必须带原因） */
    @PreAuthorize("@ss.hasPermi('wash:worker:audit')")
    @PutMapping("/qualification/review")
    public AjaxResult reviewQualification(@RequestBody Map<String, String> body) {
        Long workerId = parseLong(body == null ? null : body.get("workerId"));
        qualificationService.review(workerId, body == null ? null : body.get("reviewStatus"),
                body == null ? null : body.get("reviewRemark"), SecurityUtils.getUsername());
        return AjaxResult.success();
    }

    /** 登记人脸核身结果：PASSED / FAILED（provider 区分微信回调与人工登记） */
    @PreAuthorize("@ss.hasPermi('wash:worker:audit')")
    @PutMapping("/qualification/face")
    public AjaxResult updateFace(@RequestBody Map<String, String> body) {
        Long workerId = parseLong(body == null ? null : body.get("workerId"));
        qualificationService.updateFace(workerId, body == null ? null : body.get("faceStatus"),
                body == null ? null : body.get("faceProvider"), body == null ? null : body.get("faceRef"));
        return AjaxResult.success();
    }

    // ---------------- 周排班（T13） ----------------

    @PreAuthorize("@ss.hasPermi('wash:worker:schedule')")
    @GetMapping("/{workerId}/schedule")
    public AjaxResult schedule(@PathVariable Long workerId) {
        return AjaxResult.success(scheduleService.listByWorker(workerId));
    }

    /** 整体覆盖周排班：传空数组 = 清空（不限制派单时段） */
    @PreAuthorize("@ss.hasPermi('wash:worker:schedule')")
    @PutMapping("/{workerId}/schedule")
    public AjaxResult saveSchedule(@PathVariable Long workerId,
                                   @RequestBody(required = false) List<WashWorkerSchedule> schedules) {
        scheduleService.save(workerId, schedules);
        return AjaxResult.success();
    }

    // ---------------- 派单（T13） ----------------

    /** 派单候选：本站点合格师傅，同小区优先 */
    @PreAuthorize("@ss.hasPermi('wash:order:dispatch')")
    @GetMapping("/dispatch/candidates")
    public AjaxResult dispatchCandidates(@RequestParam Long siteId,
                                         @RequestParam(required = false) Long communityId) {
        List<WashWorker> candidates = dispatchService.candidates(siteId, communityId);
        List<Map<String, Object>> options = new ArrayList<>();
        for (WashWorker w : candidates) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", w.getWorkerId());
            item.put("label", w.getName() + "（" + w.getWorkerNo() + "）");
            item.put("communityId", w.getCommunityId());
            item.put("activeCount", dispatchService.activeCount(w.getWorkerId()));
            options.add(item);
        }
        return AjaxResult.success(options);
    }

    /** 手动派单 / 改派：无条件覆盖，用于纠正自动派单结果 */
    @PreAuthorize("@ss.hasPermi('wash:order:dispatch')")
    @PostMapping("/dispatch")
    public AjaxResult dispatch(@RequestBody Map<String, Object> body) {
        String orderNo = body == null ? null : (String) body.get("orderNo");
        Long workerId = parseLong(body == null ? null : body.get("workerId"));
        dispatchService.dispatch(orderNo, workerId);
        return AjaxResult.success();
    }

    /** 后台改派：该师傅进行中的订单（改派时选单用） */
    @PreAuthorize("@ss.hasPermi('wash:order:dispatch')")
    @GetMapping("/{workerId}/tasks")
    public AjaxResult workerTasks(@PathVariable Long workerId) {
        return AjaxResult.success(dispatchService.activeTasks(workerId));
    }

    /** 后台手动派单：本站点待派单池 */
    @PreAuthorize("@ss.hasPermi('wash:order:dispatch')")
    @GetMapping("/dispatch/pool")
    public AjaxResult dispatchPool(@RequestParam Long siteId) {
        return AjaxResult.success(dispatchService.dispatchPool(siteId));
    }

    private Long parseLong(Object v) {
        if (v == null) {
            throw new ApiException(ErrorCode.A0001, "workerId 必填");
        }
        try {
            return Long.valueOf(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.A0001, "workerId 不合法");
        }
    }
}
