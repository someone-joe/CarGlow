package com.ruoyi.wash.pay.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.pay.dto.GiftRuleVO;
import com.ruoyi.wash.pay.dto.RechargeRequest;
import com.ruoyi.wash.pay.dto.RechargeVO;
import com.ruoyi.wash.pay.dto.WalletVO;
import com.ruoyi.wash.pay.service.WashWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * C 端储值钱包。契约：openapi.yaml /api/v1/wallet/**。
 *
 * <p>充值发起走 mock 渠道（微信后补）；mock-pay 到账受 wash.pay.mock-enabled 控制，生产必须 false。
 */
@RestController
public class WxWalletController {

    @Autowired
    private WashWalletService walletService;

    @Value("${wash.pay.mock-enabled:false}")
    private boolean mockEnabled;

    @GetMapping("/api/v1/wallet")
    public ApiResult<WalletVO> get() {
        return ApiResult.ok(walletService.getWallet(MemberContext.require()));
    }

    @GetMapping("/api/v1/wallet/recharge-rules")
    public ApiResult<List<GiftRuleVO>> rules() {
        return ApiResult.ok(walletService.activeRules());
    }

    @GetMapping("/api/v1/wallet/flows")
    public ApiResult<Map<String, Object>> flows(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResult.ok(walletService.flows(MemberContext.require(), pageNum, pageSize));
    }

    @PostMapping("/api/v1/wallet/recharge")
    public ApiResult<RechargeVO> recharge(@RequestBody RechargeRequest req) {
        return ApiResult.ok(walletService.recharge(MemberContext.require(), req));
    }

    @PostMapping("/api/v1/wallet/recharge/{rechargeNo}/mock-pay")
    public ApiResult<WalletVO> mockPay(@PathVariable String rechargeNo) {
        if (!mockEnabled) {
            throw new ApiException(ErrorCode.A0003, "当前环境未开启模拟支付");
        }
        return ApiResult.ok(walletService.mockPayRecharge(MemberContext.require(), rechargeNo));
    }
}
