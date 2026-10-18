package com.ruoyi.wash.pay.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wash.pay.domain.WashWalletGiftRule;
import com.ruoyi.wash.pay.dto.GiftRuleVO;
import com.ruoyi.wash.pay.service.WashWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 后台储值钱包：充值赠送规则配置 + 充值流水查询。契约：openapi.yaml /admin-api/wash/wallet/**。
 */
@RestController
public class AdminWalletController {

    @Autowired
    private WashWalletService walletService;

    @GetMapping("/admin-api/wash/wallet/gift-rules")
    @PreAuthorize("@ss.hasPermi('wash:wallet:list')")
    public AjaxResult rules() {
        return AjaxResult.success(walletService.listAllRules());
    }

    @PutMapping("/admin-api/wash/wallet/gift-rules")
    @PreAuthorize("@ss.hasPermi('wash:wallet:edit')")
    public AjaxResult save(@RequestBody List<GiftRuleVO> rules) {
        walletService.saveRules(rules);
        return AjaxResult.success("保存成功");
    }

    @GetMapping("/admin-api/wash/wallet/recharges")
    @PreAuthorize("@ss.hasPermi('wash:wallet:recharge')")
    public TableDataInfo recharges(@RequestParam(required = false) Long memberId,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(defaultValue = "1") int pageNum,
                                   @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> data = walletService.rechargePage(memberId, status, pageNum, pageSize);
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows((List<?>) data.get("rows"));
        rsp.setTotal(Long.parseLong(String.valueOf(data.get("total"))));
        return rsp;
    }
}
