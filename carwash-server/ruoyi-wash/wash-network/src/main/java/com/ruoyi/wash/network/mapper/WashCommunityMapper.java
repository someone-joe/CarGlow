package com.ruoyi.wash.network.mapper;

import com.ruoyi.wash.network.domain.WashCommunity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 小区读写。跨模块请走 WashSiteQueryService。 */
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
}
