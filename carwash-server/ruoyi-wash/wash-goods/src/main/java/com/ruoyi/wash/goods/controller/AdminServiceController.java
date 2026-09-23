package com.ruoyi.wash.goods.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wash.goods.domain.WashService;
import com.ruoyi.wash.goods.domain.WashServiceCategory;
import com.ruoyi.wash.goods.mapper.WashServiceMapper;
import com.ruoyi.wash.goods.service.WashServiceAdminService;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台服务项管理（商品可配置）。
 *
 * <p>权限标识沿用 PRD 6.7.3 的 `wash:service:*`，与 wash:order:* / wash:member:* 同一套命名。
 * 列表返回若依 TableDataInfo，后台前端零改造即可渲染。
 */
@RestController
@RequestMapping("/admin-api/wash/service")
public class AdminServiceController {

    @Autowired
    private WashServiceAdminService adminService;

    @Autowired
    private WashServiceMapper serviceMapper;

    /** 后台服务项列表：含下架项，支持按名称/分类筛选 */
    @PreAuthorize("@ss.hasPermi('wash:service:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String serviceName,
                              @RequestParam(required = false) Long categoryId,
                              @RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize) {
        List<WashService> rows = adminService.list(serviceName, categoryId, pageNum, pageSize);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows(rows);
        rspData.setTotal(adminService.count(serviceName, categoryId));
        return rspData;
    }

    @PreAuthorize("@ss.hasPermi('wash:service:list')")
    @GetMapping("/{serviceId}")
    public AjaxResult get(@PathVariable Long serviceId) {
        return AjaxResult.success(adminService.get(serviceId));
    }

    /** 分类下拉：来源为 wash_service_category 表，前端不得自己写一份分类名单 */
    @PreAuthorize("@ss.hasPermi('wash:service:list')")
    @GetMapping("/category-options")
    public AjaxResult categoryOptions() {
        List<Map<String, Object>> options = new java.util.ArrayList<>();
        for (WashServiceCategory c : serviceMapper.selectCategories()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", c.getCategoryId());
            item.put("label", c.getCategoryName());
            options.add(item);
        }
        return AjaxResult.success(options);
    }

    @PreAuthorize("@ss.hasPermi('wash:service:add')")
    @PostMapping
    public AjaxResult add(@RequestBody WashService service) {
        return AjaxResult.success(adminService.add(service));
    }

    @PreAuthorize("@ss.hasPermi('wash:service:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody WashService service) {
        adminService.edit(service);
        return AjaxResult.success();
    }

    /** 删除为逻辑删除：历史订单仍引用服务名与价格快照 */
    @PreAuthorize("@ss.hasPermi('wash:service:remove')")
    @DeleteMapping("/{serviceId}")
    public AjaxResult remove(@PathVariable Long serviceId) {
        adminService.remove(serviceId);
        return AjaxResult.success();
    }
}
