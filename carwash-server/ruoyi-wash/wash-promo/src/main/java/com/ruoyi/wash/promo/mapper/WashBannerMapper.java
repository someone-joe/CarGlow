package com.ruoyi.wash.promo.mapper;

import com.ruoyi.wash.promo.domain.WashBanner;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 首页 Banner 读写（注解式，与项目其它 wash mapper 一致）。 */
public interface WashBannerMapper {

    /** C 端：上架 + 在生效窗口内 + 未删除，按 sortOrder 升序 */
    @Select("select banner_id, scene, title, subtitle, image_url, link_type, link_value, sort_order, " +
            "start_time, end_time, enabled from wash_banner " +
            "where enabled = 'Y' and del_flag = '0' and scene = #{scene} " +
            "and (start_time is null or start_time <= #{now}) and (end_time is null or end_time >= #{now}) " +
            "order by sort_order asc, banner_id asc")
    List<WashBanner> selectActive(@Param("scene") String scene, @Param("now") long now);

    @Select("<script>" +
            "select banner_id, scene, title, subtitle, image_url, link_type, link_value, sort_order, " +
            "start_time, end_time, enabled from wash_banner where del_flag = '0' " +
            "<if test='title != null and title != \"\"'> and title like concat('%', #{title}, '%') </if> " +
            "order by sort_order asc, banner_id desc limit #{offset}, #{size}" +
            "</script>")
    List<WashBanner> selectAdminList(@Param("title") String title, @Param("offset") int offset, @Param("size") int size);

    @Select("<script>" +
            "select count(*) from wash_banner where del_flag = '0' " +
            "<if test='title != null and title != \"\"'> and title like concat('%', #{title}, '%') </if>" +
            "</script>")
    long countAdmin(@Param("title") String title);

    @Select("select banner_id, scene, title, subtitle, image_url, link_type, link_value, sort_order, " +
            "start_time, end_time, enabled from wash_banner where banner_id = #{bannerId} and del_flag = '0' limit 1")
    WashBanner selectById(@Param("bannerId") Long bannerId);

    @Insert("insert into wash_banner (scene, title, subtitle, image_url, link_type, link_value, sort_order, " +
            "start_time, end_time, enabled, del_flag, create_time) " +
            "values (#{scene}, #{title}, #{subtitle}, #{imageUrl}, #{linkType}, #{linkValue}, #{sortOrder}, " +
            "#{startTime}, #{endTime}, #{enabled}, '0', sysdate())")
    @Options(useGeneratedKeys = true, keyProperty = "bannerId")
    int insert(WashBanner banner);

    @Update("update wash_banner set scene = #{scene}, title = #{title}, subtitle = #{subtitle}, " +
            "image_url = #{imageUrl}, link_type = #{linkType}, link_value = #{linkValue}, sort_order = #{sortOrder}, " +
            "start_time = #{startTime}, end_time = #{endTime}, enabled = #{enabled}, update_time = sysdate() " +
            "where banner_id = #{bannerId} and del_flag = '0'")
    int update(WashBanner banner);

    @Update("update wash_banner set del_flag = '2' where banner_id = #{bannerId}")
    int logicDelete(@Param("bannerId") Long bannerId);
}
