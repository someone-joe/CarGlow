package com.ruoyi.wash.member.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.member.domain.WashVehicle;
import com.ruoyi.wash.member.dto.VehicleVO;
import com.ruoyi.wash.member.service.WashVehicleQueryService;
import com.ruoyi.wash.member.service.WashVehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端车辆。契约：openapi.yaml GET / POST /api/v1/vehicles、PUT /api/v1/vehicles/{vehicleId}。
 *
 * <p>只返回当前登录会员的车辆（MemberContext 保证），不接收 memberId 入参，防止越权枚举。
 */
@RestController
public class WxVehicleController {

    @Autowired
    private WashVehicleQueryService vehicleQuery;

    @Autowired
    private WashVehicleService vehicleService;

    @GetMapping("/api/v1/vehicles")
    public ApiResult<List<VehicleVO>> list() {
        return ApiResult.ok(vehicleQuery.listByMember(MemberContext.require()));
    }

    /** 新增车辆。契约：openapi.yaml POST /api/v1/vehicles（plateNo 必填） */
    @PostMapping("/api/v1/vehicles")
    public ApiResult<Long> create(@RequestBody(required = false) VehicleRequest request) {
        if (request == null) {
            throw new ApiException(ErrorCode.A0001, "车牌号必填");
        }
        // 返回新建车辆 ID，前端据此选中刚添加的车，不用再刷一次列表
        WashVehicle created = vehicleService.create(MemberContext.require(), request.plateNo(),
                request.brand(), request.color(), request.isNewEnergy(),
                request.communityId(), request.parkingNo());
        return ApiResult.ok(created.getVehicleId());
    }

    /** 修改车辆。契约：openapi.yaml PUT /api/v1/vehicles/{vehicleId} */
    @PutMapping("/api/v1/vehicles/{vehicleId}")
    public ApiResult<Void> update(@PathVariable Long vehicleId,
                                  @RequestBody(required = false) VehicleRequest request) {
        vehicleService.update(MemberContext.require(), vehicleId,
                request == null ? null : request.plateNo(),
                request == null ? null : request.brand(),
                request == null ? null : request.color(),
                request == null ? null : request.isNewEnergy(),
                request == null ? null : request.communityId(),
                request == null ? null : request.parkingNo());
        return ApiResult.ok();
    }

    /**
     * 契约 VehicleRequest 的"可落库子集"。
     * photoFileId（影像未开工）、isDefault（表无此列）、building/unit/floor（地址模块未开工）暂不支持。
     */
    public record VehicleRequest(String plateNo, String brand, String color, Boolean isNewEnergy,
                                 Long communityId, String parkingNo) {
    }
}
