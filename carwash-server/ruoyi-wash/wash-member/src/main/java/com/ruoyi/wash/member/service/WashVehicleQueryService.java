package com.ruoyi.wash.member.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.member.domain.WashVehicle;
import com.ruoyi.wash.member.dto.AddressVO;
import com.ruoyi.wash.member.dto.VehicleVO;
import com.ruoyi.wash.member.mapper.WashVehicleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** 车辆查询。跨模块只能调 Service，禁止其他模块直接注入 WashVehicleMapper。 */
@Service
public class WashVehicleQueryService {

    private static final String YES = "Y";

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

    /** C 端我的车辆列表 */
    public List<VehicleVO> listByMember(Long memberId) {
        List<VehicleVO> list = new ArrayList<>();
        for (WashVehicle vehicle : vehicleMapper.selectByMember(memberId)) {
            VehicleVO vo = new VehicleVO();
            vo.setVehicleId(vehicle.getVehicleId());
            vo.setPlateNo(vehicle.getPlateNo());
            vo.setBrand(vehicle.getBrand());
            vo.setColor(vehicle.getColor());
            vo.setIsNewEnergy(YES.equals(vehicle.getIsNewEnergy()));
            vo.setIsDefault(YES.equals(vehicle.getIsDefault()));

            AddressVO address = new AddressVO();
            address.setCommunityId(vehicle.getCommunityId());
            address.setParkingNo(vehicle.getParkingNo());
            vo.setAddress(address);

            list.add(vo);
        }
        return list;
    }
}
