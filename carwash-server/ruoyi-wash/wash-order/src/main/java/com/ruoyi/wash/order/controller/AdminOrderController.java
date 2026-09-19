package com.ruoyi.wash.order.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.wash.common.statemachine.OrderEvent;
import com.ruoyi.wash.common.statemachine.OrderOperatorType;
import com.ruoyi.wash.common.statemachine.OrderStatus;
import com.ruoyi.wash.common.statemachine.OrderStatusTransitions;
import com.ruoyi.wash.order.dto.AdminOrderVO;
import com.ruoyi.wash.order.mapper.WashOrderMapper;
import com.ruoyi.wash.order.service.WashOrderAdminService;
import com.ruoyi.wash.order.service.WashOrderDetailService;
import com.ruoyi.wash.order.state.WashOrderStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Autowired
    private WashOrderDetailService detailService;

    @Autowired
    private WashOrderStateService stateService;

    @Autowired
    private WashOrderMapper orderMapper;

    /** 后台订单列表：全量视图，权限 wash:order:list */
    @PreAuthorize("@ss.hasPermi('wash:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String orderNo,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) Long memberId,
                              @RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize) {
        List<AdminOrderVO> list = adminService.list(orderNo, status, memberId, pageNum, pageSize);
        long total = adminService.count(orderNo, status, memberId);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(total);
        return rspData;
    }

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

    /**
     * 后台可触发的事件选项：由状态机规则表算出（当前状态 + ADMIN 触发方），
     * 前端下拉只展示合法事件，避免客服误点无效操作。
     */
    @PreAuthorize("@ss.hasPermi('wash:order:edit')")
    @GetMapping("/{orderNo}/event-options")
    public AjaxResult eventOptions(@PathVariable String orderNo) {
        String current = orderMapper.selectStatusByOrderNo(orderNo);
        if (current == null) {
            return AjaxResult.error("订单不存在");
        }
        OrderStatus status = OrderStatus.of(current);
        List<Map<String, String>> options = Arrays.stream(OrderEvent.values())
                // 既有规则表约束，又有后台白名单：客服下拉里看不到不允许触发的事件
                .filter(e -> OrderStatusTransitions.canFire(status, e, OrderOperatorType.ADMIN))
                .filter(WashOrderStateService::isAdminAllowed)
                .map(e -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("value", e.name());
                    item.put("label", e.getLabel());
                    // 前端据此决定是否弹二次确认，规则不写死在页面里
                    item.put("confirmRequired", String.valueOf(WashOrderStateService.isConfirmRequired(e)));
                    return item;
                })
                .toList();
        return AjaxResult.success(options);
    }

    /** 人工推进（异常补救）：事件必须合法，否则状态机拒绝 */
    @PreAuthorize("@ss.hasPermi('wash:order:edit')")
    @PostMapping("/{orderNo}/advance")
    public AjaxResult advance(@PathVariable String orderNo, @RequestBody AdvanceRequest request) {
        OrderEvent event;
        try {
            event = OrderEvent.valueOf(request.event());
        } catch (Exception e) {
            return AjaxResult.error("未知事件：" + request.event());
        }
        // 高危事件必须二次确认：客服在页面勾选后才会带上 confirm=true
        if (WashOrderStateService.isConfirmRequired(event) && !Boolean.TRUE.equals(request.confirm())) {
            return AjaxResult.error("「" + event.getLabel() + "」推进后不可撤销，请勾选二次确认后再提交");
        }
        OrderStatus next = stateService.adminFire(orderNo, event, getUserId(), request.reason());
        return AjaxResult.success("已推进到 " + next.getLabel(), next.name());
    }

    /** 后台取消：不受"客户只能取消车未动"限制，但终态仍不可取消（状态机保证） */
    @PreAuthorize("@ss.hasPermi('wash:order:edit')")
    @PostMapping("/{orderNo}/cancel")
    public AjaxResult cancel(@PathVariable String orderNo, @RequestBody CancelRequest request) {
        OrderStatus next = stateService.adminCancel(orderNo, getUserId(), request == null ? null : request.reason());
        return AjaxResult.success("已取消，当前状态 " + next.getLabel(), next.name());
    }

    /** 后台详情：含 8 节点时间轴与流转日志追溯 */
    @PreAuthorize("@ss.hasPermi('wash:order:query')")
    @GetMapping("/{orderNo}")
    public AjaxResult detail(@PathVariable String orderNo) {
        return AjaxResult.success(detailService.adminDetail(orderNo));
    }

    /** confirm：高危事件（如客户取回钥匙、质检打回）需客服二次勾选 */
    public record AdvanceRequest(String event, String reason, Boolean confirm) {
    }

    public record CancelRequest(String reason) {
    }

    /** 取当前后台登录用户 ID，用于流转日志留痕 */
    private Long getUserId() {
        try {
            return SecurityUtils.getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
