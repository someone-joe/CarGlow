package com.ruoyi.wash.member.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.member.dto.VehicleVO;
import com.ruoyi.wash.member.service.WashVehicleQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端车辆。契约：openapi.yaml GET /api/v1/vehicles。
 *
 * <p>只返回当前登录会员的车辆（MemberContext 保证），不接收 memberId 入参，防止越权枚举。
 * 新增 / 修改车辆（POST、PUT）随车辆管理模块开工再补。
 */
@RestController
public class WxVehicleController {

    @Autowired
    private WashVehicleQueryService vehicleQuery;

    @GetMapping("/api/v1/vehicles")
    public ApiResult<List<VehicleVO>> list() {
        return ApiResult.ok(vehicleQuery.listByMember(MemberContext.require()));
    }
}
