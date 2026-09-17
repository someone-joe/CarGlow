package com.ruoyi.wash.order.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.order.dto.AdminOrderVO;
import com.ruoyi.wash.order.service.WashOrderAdminService;
import org.apache.ibatis.session.SqlSessionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台订单管理接口（若依体系）。
 *
 * <p>权限标识一律用 PRD 6.7.3 的 `wash:*:*` 命名，不得另起一套（CODEBUDDY 第 4 节）。
 * 返回体沿用若依 TableDataInfo，这样后台前端（RuoYi-Vue3）零改造即可渲染。
 */
@RestController
@RequestMapping("/admin-api/wash/order")
public class AdminOrderController {

    @Autowired
    private WashOrderAdminService adminService;

    /**
     * 状态下拉选项：来源为 OrderStatus 枚举（唯一真源），前端不得自己写一份状态名单。
     */
    @PreAuthorize("@ss.hasPermi('wash:order:list')")
    @GetMapping("/status-options")
    public AjaxResult statusOptions() {
        List<Map<String, String>> options = Arrays.stream(OrderStatus.values())
                .map(s -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("value", s.name());
                    item.put("label", s.getLabel());
                    return item;
                })
                .toList();
        return AjaxResult.success(options);
    }

    @PreAuthorize("@ss.hasPermi('wash:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String orderNo,
                              @RequestParam(required = false) String status,
                              @RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize) {
        List<AdminOrderVO> list = adminService.list(orderNo, status, pageNum, pageSize);
        long total = adminService.count(orderNo, status);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(total);
        return rspData;
    }
}
