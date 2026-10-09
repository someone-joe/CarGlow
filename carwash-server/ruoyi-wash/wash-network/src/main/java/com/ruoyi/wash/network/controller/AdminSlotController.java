package com.ruoyi.wash.network.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wash.network.domain.WashSlot;
import com.ruoyi.wash.network.mapper.WashSlotMapper;
import com.ruoyi.wash.network.service.WashSlotAdminService;
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

import java.util.List;
import java.util.Map;

/**
 * 后台格口管理。权限标识 wash:slot:*，与 @PreAuthorize、wash_menu.sql 三处一致。
 * 开箱留痕审计：GET /open-log（wash:slot:list 权限即可查看）。
 */
@RestController
@RequestMapping("/admin-api/wash/slot")
public class AdminSlotController {

    @Autowired
    private WashSlotAdminService adminService;
    @Autowired
    private WashSlotMapper slotMapper;

    @PreAuthorize("@ss.hasPermi('wash:slot:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) Long cabinetId,
                              @RequestParam(required = false) String status,
                              @RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize) {
        List<WashSlot> rows = adminService.list(cabinetId, status, pageNum, pageSize);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(rows);
        rsp.setTotal(adminService.count(cabinetId, status));
        return rsp;
    }

    /** 格口下拉：按机柜过滤，给订单等页面用 */
    @PreAuthorize("@ss.hasPermi('wash:slot:list')")
    @GetMapping("/options")
    public AjaxResult options(@RequestParam(required = false) Long cabinetId) {
        List<WashSlot> slots = cabinetId != null
                ? slotMapper.selectByCabinet(cabinetId)
                : slotMapper.selectList(null, null, 0, 10000);
        return AjaxResult.success(slots.stream().map(s -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("value", s.getSlotId());
            m.put("label", s.getSlotNo());
            m.put("cabinetId", s.getCabinetId());
            m.put("status", s.getStatus());
            return m;
        }).toList());
    }

    @PreAuthorize("@ss.hasPermi('wash:slot:list')")
    @GetMapping("/{slotId}")
    public AjaxResult get(@PathVariable Long slotId) {
        return AjaxResult.success(adminService.get(slotId));
    }

    @PreAuthorize("@ss.hasPermi('wash:slot:add')")
    @PostMapping
    public AjaxResult add(@RequestBody WashSlot slot) {
        return AjaxResult.success(adminService.add(slot));
    }

    @PreAuthorize("@ss.hasPermi('wash:slot:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody WashSlot slot) {
        adminService.edit(slot);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('wash:slot:remove')")
    @DeleteMapping("/{slotId}")
    public AjaxResult remove(@PathVariable Long slotId) {
        adminService.remove(slotId);
        return AjaxResult.success();
    }

    /** 开箱留痕审计：按机柜 / 格口过滤 */
    @PreAuthorize("@ss.hasPermi('wash:slot:list')")
    @GetMapping("/open-log")
    public TableDataInfo openLog(@RequestParam(required = false) Long cabinetId,
                                 @RequestParam(required = false) Long slotId,
                                 @RequestParam(defaultValue = "1") int pageNum,
                                 @RequestParam(defaultValue = "10") int pageSize) {
        List<Map<String, Object>> rows = adminService.listOpenLog(cabinetId, slotId, pageNum, pageSize);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(rows);
        rsp.setTotal(adminService.countOpenLog(cabinetId, slotId));
        return rsp;
    }
}
