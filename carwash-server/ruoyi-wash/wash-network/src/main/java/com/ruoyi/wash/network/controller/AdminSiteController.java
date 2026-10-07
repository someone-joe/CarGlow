package com.ruoyi.wash.network.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wash.network.domain.WashSite;
import com.ruoyi.wash.network.mapper.WashSiteMapper;
import com.ruoyi.wash.network.service.WashSiteAdminService;
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
 * 后台站点（中央站）管理。契约见 openapi.yaml 的 /admin-api/wash/site/**（tag admin-site）。
 * 权限标识 wash:site:*，与 @PreAuthorize、wash_menu.sql 三处一致。返回体沿用若依 AjaxResult / TableDataInfo。
 */
@RestController
@RequestMapping("/admin-api/wash/site")
public class AdminSiteController {

    @Autowired
    private WashSiteAdminService adminService;
    @Autowired
    private WashSiteMapper siteMapper;

    @PreAuthorize("@ss.hasPermi('wash:site:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String keyword,
                              @RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize) {
        List<WashSite> rows = adminService.list(keyword, pageNum, pageSize);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(rows);
        rsp.setTotal(adminService.count(keyword));
        return rsp;
    }

    /** 站点下拉：给小区管理等页面用，来源 wash_site 表（del_flag=0） */
    @PreAuthorize("@ss.hasPermi('wash:site:list')")
    @GetMapping("/options")
    public AjaxResult options() {
        List<Map<String, Object>> options = new ArrayList<>();
        for (WashSite s : siteMapper.selectAll()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", s.getSiteId());
            item.put("label", s.getSiteName());
            options.add(item);
        }
        return AjaxResult.success(options);
    }

    @PreAuthorize("@ss.hasPermi('wash:site:list')")
    @GetMapping("/{siteId}")
    public AjaxResult get(@PathVariable Long siteId) {
        return AjaxResult.success(adminService.get(siteId));
    }

    @PreAuthorize("@ss.hasPermi('wash:site:add')")
    @PostMapping
    public AjaxResult add(@RequestBody WashSite site) {
        return AjaxResult.success(adminService.add(site));
    }

    @PreAuthorize("@ss.hasPermi('wash:site:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody WashSite site) {
        adminService.edit(site);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('wash:site:remove')")
    @DeleteMapping("/{siteId}")
    public AjaxResult remove(@PathVariable Long siteId) {
        adminService.remove(siteId);
        return AjaxResult.success();
    }
}
