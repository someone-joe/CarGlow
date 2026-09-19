package com.ruoyi.wash.promo.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.promo.dto.CouponBestVO;
import com.ruoyi.wash.promo.dto.CouponTemplateVO;
import com.ruoyi.wash.promo.dto.CouponUserVO;
import com.ruoyi.wash.promo.service.WashCouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** C 端优惠券。契约：openapi.yaml /api/v1/coupons/*。 */
@RestController
public class WxCouponController {

    @Autowired
    private WashCouponService couponService;

    /** 领券中心 */
    @GetMapping("/api/v1/coupons/center")
    public ApiResult<List<CouponTemplateVO>> center() {
        long now = System.currentTimeMillis();
        return ApiResult.ok(couponService.center(MemberContext.require(), now));
    }

    /** 领取某模板的券 */
    @PostMapping("/api/v1/coupons/templates/{templateId}/receive")
    public ApiResult<CouponUserVO> receive(@PathVariable Long templateId) {
        long now = System.currentTimeMillis();
        return ApiResult.ok(couponService.receive(MemberContext.require(), templateId, now));
    }

    /** 我的优惠券（?status=UNUSED|USED|EXPIRED；不传查全部） */
    @GetMapping("/api/v1/coupons")
    public ApiResult<List<CouponUserVO>> my(@RequestParam(required = false) String status) {
        long now = System.currentTimeMillis();
        return ApiResult.ok(couponService.myCoupons(MemberContext.require(), status, now));
    }

    /** 下单页算最优券：给定支付金额，返回可减免最多的券 */
    @GetMapping("/api/v1/coupons/best")
    public ApiResult<CouponBestVO> best(@RequestParam long amount) {
        long now = System.currentTimeMillis();
        return ApiResult.ok(couponService.best(MemberContext.require(), amount, now));
    }
}
