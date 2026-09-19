package com.ruoyi.wash.member.mapper;

import com.ruoyi.wash.member.domain.WashVehicle;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 车辆读写。后台查询也走这里，跨域调用请走 WashMemberAdminService。 */
public interface WashVehicleMapper {

    @Select("select vehicle_id, member_id, plate_no, brand, color, is_new_energy, is_default, community_id, parking_no, create_time " +
            "from wash_vehicle where vehicle_id = #{vehicleId} and member_id = #{memberId} and del_flag = '0' limit 1")
    WashVehicle selectByVehicleAndMember(@Param("vehicleId") Long vehicleId, @Param("memberId") Long memberId);

    /** 后台车辆分页：可按车牌模糊查、按会员过滤 */
    @Select("<script>" +
            "select vehicle_id, member_id, plate_no, brand, color, is_new_energy, is_default, community_id, parking_no, create_time " +
            "from wash_vehicle where del_flag = '0' " +
            "<if test='plateNo != null and plateNo != \"\"'> and plate_no like concat('%', #{plateNo}, '%') </if> " +
            "<if test='memberId != null'> and member_id = #{memberId} </if> " +
            "order by create_time desc limit #{offset}, #{pageSize}" +
            "</script>")
    List<WashVehicle> selectAdminPage(@Param("plateNo") String plateNo,
                                      @Param("memberId") Long memberId,
                                      @Param("offset") int offset,
                                      @Param("pageSize") int pageSize);

    /** C 端我的车辆：只看本人名下，按添加时间倒序（前端按首项预选） */
    @Select("select vehicle_id, member_id, plate_no, brand, color, is_new_energy, is_default, community_id, parking_no, create_time " +
            "from wash_vehicle where member_id = #{memberId} and del_flag = '0' order by is_default desc, create_time desc")
    List<WashVehicle> selectByMember(@Param("memberId") Long memberId);

    @Select("<script>" +
            "select count(*) from wash_vehicle where del_flag = '0' " +
            "<if test='plateNo != null and plateNo != \"\"'> and plate_no like concat('%', #{plateNo}, '%') </if> " +
            "<if test='memberId != null'> and member_id = #{memberId} </if>" +
            "</script>")
    long countAdmin(@Param("plateNo") String plateNo, @Param("memberId") Long memberId);

    /** 新增车辆：车牌必填由 Service 校验，这里只管落库并回填主键 */
    @Insert("insert into wash_vehicle (member_id, plate_no, brand, color, is_new_energy, community_id, parking_no, del_flag, create_time) " +
            "values (#{memberId}, #{plateNo}, #{brand}, #{color}, #{isNewEnergy}, #{communityId}, #{parkingNo}, '0', sysdate())")
    @Options(useGeneratedKeys = true, keyProperty = "vehicleId")
    int insertVehicle(WashVehicle vehicle);

    /**
     * 修改车辆：只更新传了值的字段（契约 VehicleRequest 除 plateNo 外都是可选的）。
     * where 带 member_id，越权改别人的车会更新 0 行，由 Service 报错。
     */
    @Update("<script>" +
            "update wash_vehicle set update_time = sysdate() " +
            "<if test='plateNo != null'> , plate_no = #{plateNo} </if> " +
            "<if test='brand != null'> , brand = #{brand} </if> " +
            "<if test='color != null'> , color = #{color} </if> " +
            "<if test='isNewEnergy != null'> , is_new_energy = #{isNewEnergy} </if> " +
            "<if test='communityId != null'> , community_id = #{communityId} </if> " +
            "<if test='parkingNo != null'> , parking_no = #{parkingNo} </if> " +
            "where vehicle_id = #{vehicleId} and member_id = #{memberId} and del_flag = '0'" +
            "</script>")
    int updateVehicle(WashVehicle vehicle);
}
