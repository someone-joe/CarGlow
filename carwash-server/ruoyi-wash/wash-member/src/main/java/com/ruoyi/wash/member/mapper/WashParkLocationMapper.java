package com.ruoyi.wash.member.mapper;

import com.ruoyi.wash.member.domain.WashParkLocation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 常用停车位置读写。成员自有数据，所有 where 带 member_id 防越权。 */
public interface WashParkLocationMapper {

    @Select("select location_id, member_id, community_id, community_name, building, parking_no, detail, is_default, create_time, update_time " +
            "from wash_park_location where member_id = #{memberId} and del_flag = '0' " +
            "order by is_default desc, create_time desc")
    List<WashParkLocation> selectByMember(@Param("memberId") Long memberId);

    @Select("select location_id, member_id, community_id, community_name, building, parking_no, detail, is_default, create_time, update_time " +
            "from wash_park_location where location_id = #{locationId} and member_id = #{memberId} and del_flag = '0' limit 1")
    WashParkLocation selectByMemberAndId(@Param("memberId") Long memberId, @Param("locationId") Long locationId);

    @Insert("insert into wash_park_location (member_id, community_id, community_name, building, parking_no, detail, is_default, del_flag, create_time) " +
            "values (#{memberId}, #{communityId}, #{communityName}, #{building}, #{parkingNo}, #{detail}, #{isDefault}, '0', sysdate())")
    @Options(useGeneratedKeys = true, keyProperty = "locationId")
    int insert(WashParkLocation location);

    /** 只更新传了值的字段；where 带 member_id，越权改更新 0 行由 Service 报错 */
    @Update("<script>" +
            "update wash_park_location set update_time = sysdate() " +
            "<if test='communityId != null'> , community_id = #{communityId} </if> " +
            "<if test='communityName != null'> , community_name = #{communityName} </if> " +
            "<if test='building != null'> , building = #{building} </if> " +
            "<if test='parkingNo != null'> , parking_no = #{parkingNo} </if> " +
            "<if test='detail != null'> , detail = #{detail} </if> " +
            "<if test='isDefault != null'> , is_default = #{isDefault} </if> " +
            "where location_id = #{locationId} and member_id = #{memberId} and del_flag = '0'" +
            "</script>")
    int update(WashParkLocation location);

    /** 设某条为默认前，先把该会员其它位置取消默认（保证只有一个默认） */
    @Update("update wash_park_location set is_default = 'N', update_time = sysdate() " +
            "where member_id = #{memberId} and location_id != #{locationId} and del_flag = '0'")
    int clearOtherDefault(@Param("memberId") Long memberId, @Param("locationId") Long locationId);

    @Update("update wash_park_location set del_flag = '2', update_time = sysdate() " +
            "where location_id = #{locationId} and member_id = #{memberId} and del_flag = '0'")
    int softDelete(@Param("memberId") Long memberId, @Param("locationId") Long locationId);
}
