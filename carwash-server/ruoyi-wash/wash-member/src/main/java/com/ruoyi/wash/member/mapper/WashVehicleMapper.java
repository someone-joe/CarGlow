package com.ruoyi.wash.member.mapper;

import com.ruoyi.wash.member.domain.WashVehicle;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 车辆读写。校验「车辆属于本人」是下单前置条件，必须带 memberId 查。 */
public interface WashVehicleMapper {

    @Select("select vehicle_id, member_id, plate_no, brand, color, is_new_energy, community_id, parking_no " +
            "from wash_vehicle where vehicle_id = #{vehicleId} and member_id = #{memberId} and del_flag = '0' limit 1")
    WashVehicle selectByVehicleAndMember(@Param("vehicleId") Long vehicleId, @Param("memberId") Long memberId);
}
