package com.ruoyi.wash.network.mapper;

import com.ruoyi.wash.network.domain.WashCommunity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 小区读写。跨模块请走 WashSiteQueryService；后台管理 CRUD 走 WashCommunityAdminService。 */
public interface WashCommunityMapper {

    @Select("select community_id, community_name, site_id, access_auth, enabled " +
            "from wash_community where enabled = 'Y' and del_flag = '0' order by community_id asc")
    List<WashCommunity> selectEnabledList();

    @Select("select community_id, community_name, site_id, access_auth, enabled " +
            "from wash_community where site_id = #{siteId} and enabled = 'Y' and del_flag = '0' " +
            "order by community_id asc")
    List<WashCommunity> selectBySite(@Param("siteId") Long siteId);

    @Select("select community_id, community_name, site_id, access_auth, enabled " +
            "from wash_community where community_id = #{communityId} and del_flag = '0' limit 1")
    WashCommunity selectById(@Param("communityId") Long communityId);

    @Select("<script>select community_id, community_name, site_id, access_auth, enabled " +
            "from wash_community where del_flag = '0' " +
            "<if test='communityName != null and communityName != \"\"'>and community_name like concat('%', #{communityName}, '%')</if> " +
            "<if test='siteId != null'>and site_id = #{siteId}</if> " +
            "order by community_id asc limit #{offset}, #{limit}</script>")
    List<WashCommunity> selectList(@Param("communityName") String communityName, @Param("siteId") Long siteId,
                                   @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>select count(*) from wash_community where del_flag = '0' " +
            "<if test='communityName != null and communityName != \"\"'>and community_name like concat('%', #{communityName}, '%')</if> " +
            "<if test='siteId != null'>and site_id = #{siteId}</if></script>")
    int count(@Param("communityName") String communityName, @Param("siteId") Long siteId);

    @Insert("insert into wash_community (community_name, site_id, access_auth, enabled, del_flag, create_time) " +
            "values (#{communityName}, #{siteId}, #{accessAuth}, #{enabled}, '0', now())")
    @Options(useGeneratedKeys = true, keyProperty = "communityId")
    int insert(WashCommunity community);

    @Update("update wash_community set community_name = #{communityName}, site_id = #{siteId}, " +
            "access_auth = #{accessAuth}, enabled = #{enabled}, update_time = now() " +
            "where community_id = #{communityId} and del_flag = '0'")
    int update(WashCommunity community);

    @Update("update wash_community set del_flag = '1', update_time = now() where community_id = #{communityId} and del_flag = '0'")
    int logicDelete(@Param("communityId") Long communityId);
}
