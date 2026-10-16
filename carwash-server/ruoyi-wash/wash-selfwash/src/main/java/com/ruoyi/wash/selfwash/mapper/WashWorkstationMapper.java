package com.ruoyi.wash.selfwash.mapper;

import com.ruoyi.wash.selfwash.domain.WashWorkstation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 自助洗车工位读写。 */
public interface WashWorkstationMapper {

    @Select("select workstation_id, site_id, code, name, enabled, sort_order, create_time, update_time " +
            "from wash_workstation where site_id = #{siteId} and del_flag = '0' order by sort_order asc, code asc")
    List<WashWorkstation> selectBySite(@Param("siteId") Long siteId);

    @Select("<script>" +
            "select workstation_id, site_id, code, name, enabled, sort_order, create_time, update_time " +
            "from wash_workstation where del_flag = '0' " +
            "<if test='siteId != null'> and site_id = #{siteId} </if> " +
            "<if test='keyword != null and keyword != \"\"'> and (code like concat('%', #{keyword}, '%') or name like concat('%', #{keyword}, '%')) </if> " +
            "order by site_id asc, sort_order asc, code asc" +
            "</script>")
    List<WashWorkstation> selectAdminList(@Param("siteId") Long siteId, @Param("keyword") String keyword);

    @Select("select workstation_id, site_id, code, name, enabled, sort_order, create_time, update_time " +
            "from wash_workstation where workstation_id = #{id} and del_flag = '0' limit 1")
    WashWorkstation selectById(@Param("id") Long id);

    @Insert("insert into wash_workstation (site_id, code, name, enabled, sort_order, del_flag, create_time) " +
            "values (#{siteId}, #{code}, #{name}, #{enabled}, #{sortOrder}, '0', sysdate())")
    @Options(useGeneratedKeys = true, keyProperty = "workstationId")
    int insert(WashWorkstation ws);

    @Update("<script>" +
            "update wash_workstation set update_time = sysdate() " +
            "<if test='code != null'> , code = #{code} </if> " +
            "<if test='name != null'> , name = #{name} </if> " +
            "<if test='enabled != null'> , enabled = #{enabled} </if> " +
            "<if test='sortOrder != null'> , sort_order = #{sortOrder} </if> " +
            "where workstation_id = #{workstationId} and del_flag = '0'" +
            "</script>")
    int update(WashWorkstation ws);

    @Update("update wash_workstation set del_flag = '2', update_time = sysdate() " +
            "where workstation_id = #{id} and del_flag = '0'")
    int softDelete(@Param("id") Long id);
}
