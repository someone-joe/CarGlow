package com.ruoyi.wash.member.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.member.domain.WashVehicle;
import com.ruoyi.wash.member.mapper.WashVehicleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 车辆查询。跨模块只能调 Service，禁止其他模块直接注入 WashVehicleMapper。 */
@Service
public class WashVehicleQueryService {

    @Autowired
    private WashVehicleMapper vehicleMapper;

    /** 取本名下的车辆，不属于本人或不存在一律拒绝（防止越权下单）。 */
    public WashVehicle requireOwned(Long memberId, Long vehicleId) {
        WashVehicle vehicle = vehicleMapper.selectByVehicleAndMember(vehicleId, memberId);
        if (vehicle == null) {
            throw new ApiException(ErrorCode.A0001, "车辆不存在或不属于当前用户");
        }
        return vehicle;
    }
}
