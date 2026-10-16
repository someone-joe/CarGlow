package com.ruoyi.wash.selfwash.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.wash.selfwash.domain.WashWorkstation;
import com.ruoyi.wash.selfwash.service.WashWorkstationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台自助洗车工位管理。契约：openapi.yaml /admin-api/wash/selfwash/workstation/**。
 */
@RestController
public class AdminWorkstationController {

    @Autowired
    private WashWorkstationService service;

    @GetMapping("/admin-api/wash/selfwash/workstation/list")
    @PreAuthorize("@ss.hasPermi('wash:selfwash:list')")
    public AjaxResult list(@RequestParam(required = false) Long siteId,
                           @RequestParam(required = false) String keyword) {
        return AjaxResult.success(service.adminList(siteId, keyword));
    }

    @PostMapping("/admin-api/wash/selfwash/workstation")
    @PreAuthorize("@ss.hasPermi('wash:selfwash:add')")
    public AjaxResult add(@RequestBody WashWorkstation ws) {
        return AjaxResult.success(service.create(ws.getSiteId(), ws.getCode(), ws.getName(), ws.getEnabled(), ws.getSortOrder()));
    }

    @PutMapping("/admin-api/wash/selfwash/workstation/{id}")
    @PreAuthorize("@ss.hasPermi('wash:selfwash:edit')")
    public AjaxResult edit(@PathVariable Long id, @RequestBody WashWorkstation ws) {
        service.update(id, ws.getCode(), ws.getName(), ws.getEnabled(), ws.getSortOrder());
        return AjaxResult.success("保存成功");
    }

    @DeleteMapping("/admin-api/wash/selfwash/workstation/{id}")
    @PreAuthorize("@ss.hasPermi('wash:selfwash:remove')")
    public AjaxResult remove(@PathVariable Long id) {
        service.delete(id);
        return AjaxResult.success("删除成功");
    }
}
