package com.ruoyi.wash.network.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wash.network.domain.WashCommunity;
import com.ruoyi.wash.network.mapper.WashCommunityMapper;
import com.ruoyi.wash.network.service.WashCommunityAdminService;
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
 * 后台小区管理。契约见 openapi.yaml 的 /admin-api/wash/community/**（tag admin-community）。
 * 权限标识 wash:community:*，与 @PreAuthorize、wash_menu.sql 三处一致。返回体沿用若依 AjaxResult / TableDataInfo。
 */
@RestController
@RequestMapping("/admin-api/wash/community")
public class AdminCommunityController {

    @Autowired
    private WashCommunityAdminService adminService;
    @Autowired
    private WashCommunityMapper communityMapper;

    /** 小区下拉：可选按站点过滤；柜机表单用 */
    @PreAuthorize("@ss.hasPermi('wash:community:list')")
    @GetMapping("/options")
    public AjaxResult options(@RequestParam(required = false) Long siteId) {
        List<WashCommunity> list = siteId != null
                ? communityMapper.selectBySite(siteId)
                : communityMapper.selectEnabledList();
        List<Map<String, Object>> options = new ArrayList<>();
        for (WashCommunity c : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", c.getCommunityId());
            item.put("label", c.getCommunityName());
            item.put("siteId", c.getSiteId());
            options.add(item);
        }
        return AjaxResult.success(options);
    }

    @PreAuthorize("@ss.hasPermi('wash:community:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) Long siteId,
                              @RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize) {
        List<WashCommunity> rows = adminService.list(keyword, siteId, pageNum, pageSize);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(rows);
        rsp.setTotal(adminService.count(keyword, siteId));
        return rsp;
    }

    @PreAuthorize("@ss.hasPermi('wash:community:list')")
    @GetMapping("/{communityId}")
    public AjaxResult get(@PathVariable Long communityId) {
        return AjaxResult.success(adminService.get(communityId));
    }

    @PreAuthorize("@ss.hasPermi('wash:community:add')")
    @PostMapping
    public AjaxResult add(@RequestBody WashCommunity community) {
        return AjaxResult.success(adminService.add(community));
    }

    @PreAuthorize("@ss.hasPermi('wash:community:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody WashCommunity community) {
        adminService.edit(community);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('wash:community:remove')")
    @DeleteMapping("/{communityId}")
    public AjaxResult remove(@PathVariable Long communityId) {
        adminService.remove(communityId);
        return AjaxResult.success();
    }
}
