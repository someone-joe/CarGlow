package com.ruoyi.wash.member.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.member.domain.WashVehicle;
import com.ruoyi.wash.member.mapper.WashVehicleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 车辆写操作（C 端新增 / 修改）。查询走 WashVehicleQueryService。
 *
 * <p>契约：openapi.yaml POST /api/v1/vehicles、PUT /api/v1/vehicles/{vehicleId}。
 * 契约里的 photoFileId、isDefault、building/unit/floor 暂不支持：
 * 影像模块未开工、wash_vehicle 无 is_default 列、小区地址模块未开工 —— 不臆造字段。
 */
@Service
public class WashVehicleService {

    private static final String YES = "Y";
    private static final String NO = "N";

    @Autowired
    private WashVehicleMapper vehicleMapper;

    /** 新增车辆：车牌必填（契约 required），其余可选 */
    public WashVehicle create(Long memberId, String plateNo, String brand, String color,
                             Boolean isNewEnergy, Long communityId, String parkingNo) {
        String plate = requirePlateNo(plateNo);
        WashVehicle vehicle = new WashVehicle();
        vehicle.setMemberId(memberId);
        vehicle.setPlateNo(plate);
        vehicle.setBrand(blankToNull(brand));
        vehicle.setColor(blankToNull(color));
        vehicle.setIsNewEnergy(isNewEnergy != null && isNewEnergy ? YES : NO);
        vehicle.setCommunityId(communityId);
        vehicle.setParkingNo(blankToNull(parkingNo));
        vehicleMapper.insertVehicle(vehicle);
        return vehicle;
    }

    /**
     * 修改车辆：只改传了值的字段。
     *
     * <p>更新 0 行即视为"车辆不存在或不属于当前用户"，不能用返回值区分两种情况，
     * 统一报 A0001 + 明确文案，避免通过报错差异去枚举别人的车辆 ID。
     */
    public void update(Long memberId, Long vehicleId, String plateNo, String brand, String color,
                       Boolean isNewEnergy, Long communityId, String parkingNo) {
        WashVehicle vehicle = new WashVehicle();
        vehicle.setVehicleId(vehicleId);
        vehicle.setMemberId(memberId);
        vehicle.setPlateNo(plateNo == null ? null : requirePlateNo(plateNo));
        vehicle.setBrand(blankToNull(brand));
        vehicle.setColor(blankToNull(color));
        vehicle.setIsNewEnergy(isNewEnergy == null ? null : (isNewEnergy ? YES : NO));
        vehicle.setCommunityId(communityId);
        vehicle.setParkingNo(blankToNull(parkingNo));

        int rows = vehicleMapper.updateVehicle(vehicle);
        if (rows == 0) {
            throw new ApiException(ErrorCode.A0001, "车辆不存在或不属于当前用户");
        }
    }

    private String requirePlateNo(String plateNo) {
        if (plateNo == null || plateNo.isBlank()) {
            throw new ApiException(ErrorCode.A0001, "车牌号必填");
        }
        return plateNo.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
