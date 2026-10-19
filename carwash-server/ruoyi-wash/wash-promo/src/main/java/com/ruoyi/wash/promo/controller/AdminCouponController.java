package com.ruoyi.wash.promo.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.wash.promo.domain.WashCouponTemplate;
import com.ruoyi.wash.promo.dto.AdminCouponTemplateVO;
import com.ruoyi.wash.promo.dto.CouponBatchGrantVO;
import com.ruoyi.wash.promo.dto.CouponGrantAudience;
import com.ruoyi.wash.promo.service.WashCouponAdminService;
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
 * 后台优惠券管理。契约：openapi.yaml /admin-api/wash/coupon/**（tag admin-coupon）。
 *
 * <p>权限标识统一走 PRD 6.7.3 的 `wash:coupon:*` 命名，
 * 与 openapi 的 x-permission、wash_20261018_coupon_admin.sql 三处逐字一致。
 *
 * <p>两个刻意分开的口子：
 * 1. 改资料（PUT /template）不接受 total/issued —— 发行过的模板，总量就是历史账；
 * 2. 上下架走 POST /template/{id}/status —— 不跟资料编辑混在一起，
 *    免得"改个名字顺手把券下架了"。
 */
@RestController
@RequestMapping("/admin-api/wash/coupon")
public class AdminCouponController {

    @Autowired
    private WashCouponAdminService adminService;

    @GetMapping("/template/list")
    @PreAuthorize("@ss.hasPermi('wash:coupon:list')")
    public TableDataInfo list(@RequestParam(required = false) String name,
                              @RequestParam(required = false) String type,
                              @RequestParam(required = false) String status,
                              @RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> data = adminService.page(name, type, status, pageNum, pageSize);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows((List<?>) data.get("rows"));
        rsp.setTotal(Long.parseLong(String.valueOf(data.get("total"))));
        return rsp;
    }

    @GetMapping("/template/{templateId}")
    @PreAuthorize("@ss.hasPermi('wash:coupon:list')")
    public AjaxResult detail(@PathVariable Long templateId) {
        return AjaxResult.success(adminService.get(templateId));
    }

    @PostMapping("/template")
    @PreAuthorize("@ss.hasPermi('wash:coupon:add')")
    public AjaxResult add(@RequestBody WashCouponTemplate template) {
        return AjaxResult.success(adminService.add(template));
    }

    @PutMapping("/template")
    @PreAuthorize("@ss.hasPermi('wash:coupon:edit')")
    public AjaxResult edit(@RequestBody WashCouponTemplate template) {
        adminService.edit(template);
        return AjaxResult.success();
    }

    /** 上下架：Y 上架 / N 下架 */
    @PostMapping("/template/{templateId}/status")
    @PreAuthorize("@ss.hasPermi('wash:coupon:edit')")
    public AjaxResult changeStatus(@PathVariable Long templateId, @RequestBody StatusRequest body) {
        adminService.changeStatus(templateId, body == null ? null : body.status());
        return AjaxResult.success();
    }

    /** 逻辑删除：已发出的券仍引用模板名与金额快照 */
    @DeleteMapping("/template/{templateId}")
    @PreAuthorize("@ss.hasPermi('wash:coupon:remove')")
    public AjaxResult remove(@PathVariable Long templateId) {
        adminService.remove(templateId);
        return AjaxResult.success();
    }

    /** 给指定会员发券（补偿 / 线下活动）。每人限领同样生效，库存乐观扣减，不超发。 */
    @PostMapping("/grant")
    @PreAuthorize("@ss.hasPermi('wash:coupon:grant')")
    public AjaxResult grant(@RequestBody GrantRequest body) {
        adminService.grant(body == null ? null : body.templateId(), body == null ? null : body.memberId());
        return AjaxResult.success();
    }

    /** 领用 / 核销记录：会员手机号一律脱敏返回 */
    @GetMapping("/record/list")
    @PreAuthorize("@ss.hasPermi('wash:coupon:list')")
    public TableDataInfo records(@RequestParam(required = false) Long templateId,
                                 @RequestParam(required = false) String phone,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> data = adminService.records(templateId, phone, status, pageNum, pageSize);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows((List<?>) data.get("rows"));
        rsp.setTotal(Long.parseLong(String.valueOf(data.get("total"))));
        return rsp;
    }

    /** 批量发放优惠券（同步）：全量 / 注册时间 / 小区受众，逐人复用单人发放校验，返回汇总。 */
    @PostMapping("/batch")
    @PreAuthorize("@ss.hasPermi('wash:coupon:grant')")
    public AjaxResult batchGrant(@RequestBody CouponBatchGrantRequest body) {
        CouponGrantAudience a = body == null ? null : body.audience();
        CouponBatchGrantVO vo = adminService.batchGrant(
                body == null ? null : body.templateId(),
                body == null ? null : body.perCount(), a, SecurityUtils.getUsername());
        return AjaxResult.success(vo);
    }

    /** 批量发放批次列表：每条批次一行（记录视图里批量发放折叠成单条据此展示）。 */
    @GetMapping("/batch/list")
    @PreAuthorize("@ss.hasPermi('wash:coupon:list')")
    public TableDataInfo batchList(@RequestParam(required = false) Long templateId,
                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> data = adminService.batchList(templateId, pageNum, pageSize);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows((List<?>) data.get("rows"));
        rsp.setTotal(Long.parseLong(String.valueOf(data.get("total"))));
        return rsp;
    }

    /** 批量发放批次详情 + 明细（记录弹窗点开看完整发放结果）。 */
    @GetMapping("/batch/{batchId}")
    @PreAuthorize("@ss.hasPermi('wash:coupon:list')")
    public AjaxResult batchDetail(@PathVariable Long batchId,
                                  @RequestParam(required = false) String result,
                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return AjaxResult.success(adminService.batchDetail(batchId, result, pageNum, pageSize));
    }

    /** 批量补发：对失败 / 跳过会员重新发放，更新明细结果并重算汇总。 */
    @PostMapping("/batch/{batchId}/regrant")
    @PreAuthorize("@ss.hasPermi('wash:coupon:grant')")
    public AjaxResult regrant(@PathVariable Long batchId, @RequestBody CouponBatchRegrantRequest body) {
        List<Long> ids = body == null ? null : body.memberIds();
        return AjaxResult.success(adminService.regrant(batchId, ids, SecurityUtils.getUsername()));
    }

    /** 与契约 request body 逐字段一致 */
    public record StatusRequest(String status) {
    }

    /** 与契约 request body 逐字段一致 */
    public record GrantRequest(Long templateId, Long memberId) {
    }

    /** 与契约 request body 逐字段一致 */
    public record CouponBatchGrantRequest(Long templateId, Integer perCount, CouponGrantAudience audience) {
    }

    /** 与契约 request body 逐字段一致 */
    public record CouponBatchRegrantRequest(List<Long> memberIds) {
    }
}
