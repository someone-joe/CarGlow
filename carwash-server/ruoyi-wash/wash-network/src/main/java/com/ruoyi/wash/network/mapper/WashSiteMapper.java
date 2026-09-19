package com.ruoyi.wash.network.mapper;

import com.ruoyi.wash.network.domain.WashSite;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 站点读写。跨模块请走 WashSiteQueryService。 */
public interface WashSiteMapper {

    @Select("select site_id, site_name, daily_limit, deposit_deadline, promise_return_time, " +
            "service_status, closed_notice from wash_site where site_id = #{siteId} and del_flag = '0' limit 1")
    WashSite selectById(@Param("siteId") Long siteId);

    /** 全部未删除站点：给产能初始化任务用（已停业的站点也要有产能，否则无法限制下单） */
    @Select("select site_id, site_name, daily_limit, deposit_deadline, promise_return_time, " +
            "service_status, closed_notice from wash_site where del_flag = '0' order by site_id asc")
    List<WashSite> selectAll();
}
