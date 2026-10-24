package com.ruoyi.wash.network.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wash.network.domain.WashCommunityApply;
import com.ruoyi.wash.network.service.WashCommunityApplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台小区开通申请审核。契约见 openapi.yaml /admin-api/wash/community-apply/**（tag admin-community-apply）。
 * 权限 wash:community-apply:*，与 @PreAuthorize、wash_menu.sql 三处一致。返回体沿用若依 AjaxResult / TableDataInfo。
 */
@RestController
@RequestMapping("/admin-api/wash/community-apply")
public class AdminCommunityApplyController {

    @Autowired
    private WashCommunityApplyService applyService;

    @PreAuthorize("@ss.hasPermi('wash:community-apply:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String status,
                              @RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize) {
        List<WashCommunityApply> rows = applyService.list(keyword, status, pageNum, pageSize);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(rows);
        rsp.setTotal(applyService.count(keyword, status));
        return rsp;
    }

    @PreAuthorize("@ss.hasPermi('wash:community-apply:list')")
    @GetMapping("/{applyId}")
    public AjaxResult get(@PathVariable Long applyId) {
        return AjaxResult.success(applyService.get(applyId));
    }

    @PreAuthorize("@ss.hasPermi('wash:community-apply:approve')")
    @PostMapping("/{applyId}/approve")
    public AjaxResult approve(@PathVariable Long applyId, @RequestBody ApproveRequest req) {
        Long communityId = applyService.approve(applyId, req.siteId(), req.communityName());
        return AjaxResult.success(communityId);
    }

    @PreAuthorize("@ss.hasPermi('wash:community-apply:reject')")
    @PostMapping("/{applyId}/reject")
    public AjaxResult reject(@PathVariable Long applyId, @RequestBody RejectRequest req) {
        applyService.reject(applyId, req.reason());
        return AjaxResult.success();
    }

    public record ApproveRequest(Long siteId, String communityName) {
    }

    public record RejectRequest(String reason) {
    }
}
