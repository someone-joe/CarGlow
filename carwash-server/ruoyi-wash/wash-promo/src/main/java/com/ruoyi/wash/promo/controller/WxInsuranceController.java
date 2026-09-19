package com.ruoyi.wash.promo.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.promo.dto.BuyInsuranceRequest;
import com.ruoyi.wash.promo.dto.InsurancePolicyVO;
import com.ruoyi.wash.promo.dto.InsuranceProductVO;
import com.ruoyi.wash.promo.service.WashInsuranceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** C 端保险/保单。契约：openapi.yaml /api/v1/insurances/*。 */
@RestController
public class WxInsuranceController {

    @Autowired
    private WashInsuranceService insuranceService;

    /** 可投保产品列表 */
    @GetMapping("/api/v1/insurances/products")
    public ApiResult<List<InsuranceProductVO>> products() {
        return ApiResult.ok(insuranceService.products());
    }

    /** 投保（dev 模式直接生效） */
    @PostMapping("/api/v1/insurances")
    public ApiResult<InsurancePolicyVO> buy(@RequestBody BuyInsuranceRequest req) {
        long now = System.currentTimeMillis();
        return ApiResult.ok(insuranceService.buy(
                MemberContext.require(), req.productId(), req.vehicleId(), req.orderNo(), now));
    }

    /** 我的保单 */
    @GetMapping("/api/v1/insurances")
    public ApiResult<List<InsurancePolicyVO>> my() {
        return ApiResult.ok(insuranceService.myPolicies(MemberContext.require()));
    }
}
