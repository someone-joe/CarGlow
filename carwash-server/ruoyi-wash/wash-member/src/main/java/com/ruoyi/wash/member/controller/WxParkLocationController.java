package com.ruoyi.wash.member.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.member.dto.ParkLocationVO;
import com.ruoyi.wash.member.service.WashParkLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端常用停车位置。契约：openapi.yaml /api/v1/park-locations。
 *
 * <p>只操作当前登录会员的位置（MemberContext 保证），不接收 memberId 入参，防止越权。
 */
@RestController
public class WxParkLocationController {

    @Autowired
    private WashParkLocationService service;

    @GetMapping("/api/v1/park-locations")
    public ApiResult<List<ParkLocationVO>> list() {
        return ApiResult.ok(service.listByMember(MemberContext.require()));
    }

    @PostMapping("/api/v1/park-locations")
    public ApiResult<Long> create(@RequestBody(required = false) ParkLocationRequest request) {
        if (request == null) {
            throw new ApiException(ErrorCode.A0001, "请选择所在小区");
        }
        Long id = service.create(MemberContext.require(), request.communityId(), request.communityName(),
                request.building(), request.parkingNo(), request.detail(), request.isDefault());
        return ApiResult.ok(id);
    }

    @PutMapping("/api/v1/park-locations/{locationId}")
    public ApiResult<Void> update(@PathVariable Long locationId,
                                  @RequestBody(required = false) ParkLocationRequest request) {
        service.update(MemberContext.require(), locationId,
                request == null ? null : request.communityId(),
                request == null ? null : request.communityName(),
                request == null ? null : request.building(),
                request == null ? null : request.parkingNo(),
                request == null ? null : request.detail(),
                request == null ? null : request.isDefault());
        return ApiResult.ok();
    }

    @DeleteMapping("/api/v1/park-locations/{locationId}")
    public ApiResult<Void> delete(@PathVariable Long locationId) {
        service.delete(MemberContext.require(), locationId);
        return ApiResult.ok();
    }

    /** 契约 ParkLocationRequest。communityId 必填（所在小区），其余选填。 */
    public record ParkLocationRequest(Long communityId, String communityName, String building,
                                      String parkingNo, String detail, String isDefault) {
    }
}
