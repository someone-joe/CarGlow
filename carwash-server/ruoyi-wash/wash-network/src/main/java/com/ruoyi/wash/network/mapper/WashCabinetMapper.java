package com.ruoyi.wash.network.mapper;

import com.ruoyi.wash.network.domain.WashCabinet;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 机柜读写。下单需校验机柜存在且启用；后台管理 CRUD 走 WashCabinetAdminService。 */
public interface WashCabinetMapper {

    @Select("select cabinet_id, site_id, community_id, cabinet_name, enabled " +
            "from wash_cabinet where cabinet_id = #{cabinetId} and enabled = 'Y' and del_flag = '0' limit 1")
    WashCabinet selectEnabledById(@Param("cabinetId") Long cabinetId);

    /** 按主键查（不问启用状态）：订单详情补全柜名时，柜子可能已停用，仍要显示历史名称 */
    @Select("select cabinet_id, site_id, community_id, cabinet_name, enabled " +
            "from wash_cabinet where cabinet_id = #{cabinetId} and del_flag = '0' limit 1")
    WashCabinet selectById(@Param("cabinetId") Long cabinetId);

    /** C 端机柜列表：按小区过滤 + 按名称模糊搜（楼栋/单元） */
    @Select("<script>" +
            "select cabinet_id, site_id, community_id, cabinet_name, enabled " +
            "from wash_cabinet where enabled = 'Y' and del_flag = '0' " +
            "<if test='communityId != null'> and community_id = #{communityId} </if> " +
            "<if test='keyword != null and keyword != \"\"'> and cabinet_name like concat('%', #{keyword}, '%') </if> " +
            "order by cabinet_id asc" +
            "</script>")
    List<WashCabinet> selectEnabledList(@Param("communityId") Long communityId, @Param("keyword") String keyword);

    /** 后台列表：带站点名 / 小区名（左连接，柜子可能已删站点/小区） */
    @Select("<script>" +
            "select c.cabinet_id, c.site_id, c.community_id, c.cabinet_name, c.enabled, " +
            "s.site_name, m.community_name " +
            "from wash_cabinet c " +
            "left join wash_site s on s.site_id = c.site_id " +
            "left join wash_community m on m.community_id = c.community_id " +
            "where c.del_flag = '0' " +
            "<if test='keyword != null and keyword != \"\"'> and c.cabinet_name like concat('%', #{keyword}, '%') </if> " +
            "<if test='communityId != null'> and c.community_id = #{communityId} </if> " +
            "order by c.cabinet_id desc limit #{offset}, #{limit}" +
            "</script>")
    List<WashCabinet> selectList(@Param("keyword") String keyword,
                                 @Param("communityId") Long communityId,
                                 @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>select count(*) from wash_cabinet where del_flag = '0' " +
            "<if test='keyword != null and keyword != \"\"'> and cabinet_name like concat('%', #{keyword}, '%') </if> " +
            "<if test='communityId != null'> and community_id = #{communityId} </if></script>")
    int count(@Param("keyword") String keyword, @Param("communityId") Long communityId);

    /** 下拉：全部未删除机柜（del_flag=0） */
    @Select("select cabinet_id, site_id, community_id, cabinet_name, enabled " +
            "from wash_cabinet where del_flag = '0' order by cabinet_id asc")
    List<WashCabinet> selectAll();

    @Options(useGeneratedKeys = true, keyProperty = "cabinetId")
    @Insert("<script>insert into wash_cabinet " +
            "<trim prefix='(' suffix=')' suffixOverrides=','>" +
            "site_id, community_id, cabinet_name, " +
            "<if test='enabled != null'>enabled,</if>" +
            "del_flag, create_time" +
            "</trim> " +
            "<trim prefix='values (' suffix=')' suffixOverrides=','>" +
            "#{siteId}, #{communityId}, #{cabinetName}, " +
            "<if test='enabled != null'>#{enabled},</if>" +
            "'0', now()" +
            "</trim></script>")
    int insert(WashCabinet cabinet);

    @Update("<script>update wash_cabinet " +
            "<set>" +
            "<if test='siteId != null'>site_id = #{siteId},</if>" +
            "<if test='communityId != null'>community_id = #{communityId},</if>" +
            "<if test='cabinetName != null'>cabinet_name = #{cabinetName},</if>" +
            "<if test='enabled != null'>enabled = #{enabled},</if>" +
            "update_time = now()" +
            "</set> where cabinet_id = #{cabinetId} and del_flag = '0'</script>")
    int update(WashCabinet cabinet);

    @Update("update wash_cabinet set del_flag = '1', update_time = now() where cabinet_id = #{cabinetId} and del_flag = '0'")
    int logicDelete(@Param("cabinetId") Long cabinetId);
}
