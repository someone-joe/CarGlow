package com.ruoyi.wash.network.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wash.network.domain.WashCabinet;
import com.ruoyi.wash.network.mapper.WashCabinetMapper;
import com.ruoyi.wash.network.service.WashCabinetAdminService;
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
 * 后台机柜（钥匙柜）管理。权限标识 wash:cabinet:*，与 @PreAuthorize、wash_menu.sql 三处一致。
 * 返回体沿用若依 AjaxResult / TableDataInfo。
 */
@RestController
@RequestMapping("/admin-api/wash/cabinet")
public class AdminCabinetController {

    @Autowired
    private WashCabinetAdminService adminService;
    @Autowired
    private WashCabinetMapper cabinetMapper;

    @PreAuthorize("@ss.hasPermi('wash:cabinet:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) Long communityId,
                              @RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize) {
        List<WashCabinet> rows = adminService.list(keyword, communityId, pageNum, pageSize);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(rows);
        rsp.setTotal(adminService.count(keyword, communityId));
        return rsp;
    }

    /** 机柜下拉：给格口管理等页面用，来源 wash_cabinet 表（del_flag=0） */
    @PreAuthorize("@ss.hasPermi('wash:cabinet:list')")
    @GetMapping("/options")
    public AjaxResult options() {
        List<Map<String, Object>> options = new ArrayList<>();
        for (WashCabinet c : cabinetMapper.selectAll()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", c.getCabinetId());
            item.put("label", c.getCabinetName());
            item.put("siteId", c.getSiteId());
            item.put("communityId", c.getCommunityId());
            options.add(item);
        }
        return AjaxResult.success(options);
    }

    @PreAuthorize("@ss.hasPermi('wash:cabinet:list')")
    @GetMapping("/{cabinetId}")
    public AjaxResult get(@PathVariable Long cabinetId) {
        return AjaxResult.success(adminService.get(cabinetId));
    }

    @PreAuthorize("@ss.hasPermi('wash:cabinet:add')")
    @PostMapping
    public AjaxResult add(@RequestBody WashCabinet cabinet) {
        return AjaxResult.success(adminService.add(cabinet));
    }

    @PreAuthorize("@ss.hasPermi('wash:cabinet:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody WashCabinet cabinet) {
        adminService.edit(cabinet);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('wash:cabinet:remove')")
    @DeleteMapping("/{cabinetId}")
    public AjaxResult remove(@PathVariable Long cabinetId) {
        adminService.remove(cabinetId);
        return AjaxResult.success();
    }
}
