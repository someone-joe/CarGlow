package com.ruoyi.wash.promo.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wash.promo.domain.WashBanner;
import com.ruoyi.wash.promo.service.WashBannerService;
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
 * 后台首页 Banner 管理。契约：openapi.yaml /admin-api/wash/banner/**。
 */
@RestController
@RequestMapping("/admin-api/wash/banner")
public class AdminBannerController {

    @Autowired
    private WashBannerService bannerService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('wash:banner:list')")
    public TableDataInfo list(@RequestParam(required = false) String title,
                              @RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> data = bannerService.page(title, pageNum, pageSize);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows((List<?>) data.get("rows"));
        rsp.setTotal(Long.parseLong(String.valueOf(data.get("total"))));
        return rsp;
    }

    @GetMapping("/{bannerId}")
    @PreAuthorize("@ss.hasPermi('wash:banner:list')")
    public AjaxResult detail(@PathVariable Long bannerId) {
        return AjaxResult.success(bannerService.get(bannerId));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('wash:banner:add')")
    public AjaxResult add(@RequestBody WashBanner banner) {
        return AjaxResult.success(bannerService.add(banner));
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermi('wash:banner:edit')")
    public AjaxResult edit(@RequestBody WashBanner banner) {
        bannerService.edit(banner);
        return AjaxResult.success();
    }

    @DeleteMapping("/{bannerId}")
    @PreAuthorize("@ss.hasPermi('wash:banner:remove')")
    public AjaxResult remove(@PathVariable Long bannerId) {
        bannerService.remove(bannerId);
        return AjaxResult.success();
    }
}
