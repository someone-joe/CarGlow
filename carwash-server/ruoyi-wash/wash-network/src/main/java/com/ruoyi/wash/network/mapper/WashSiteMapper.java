package com.ruoyi.wash.network.mapper;

import com.ruoyi.wash.network.domain.WashSite;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 站点读写。跨模块请走 WashSiteQueryService；后台管理 CRUD 走 WashSiteAdminService。 */
public interface WashSiteMapper {

    @Select("select site_id, site_name, daily_limit, deposit_deadline, promise_return_time, " +
            "business_start, business_end, service_status, closed_notice from wash_site where site_id = #{siteId} and del_flag = '0' limit 1")
    WashSite selectById(@Param("siteId") Long siteId);

    /** 全部未删除站点：给产能初始化任务用（已停业的站点也要有产能，否则无法限制下单） */
    @Select("select site_id, site_name, daily_limit, deposit_deadline, promise_return_time, " +
            "business_start, business_end, service_status, closed_notice from wash_site where del_flag = '0' order by site_id asc")
    List<WashSite> selectAll();

    @Select("<script>select site_id, site_name, daily_limit, deposit_deadline, promise_return_time, " +
            "business_start, business_end, service_status, closed_notice from wash_site where del_flag = '0' " +
            "<if test='siteName != null and siteName != \"\"'>and site_name like concat('%', #{siteName}, '%')</if> " +
            "order by site_id asc limit #{offset}, #{limit}</script>")
    List<WashSite> selectList(@Param("siteName") String siteName,
                              @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>select count(*) from wash_site where del_flag = '0' " +
            "<if test='siteName != null and siteName != \"\"'>and site_name like concat('%', #{siteName}, '%')</if></script>")
    int count(@Param("siteName") String siteName);

    @Insert("insert into wash_site (site_name, daily_limit, deposit_deadline, promise_return_time, " +
            "business_start, business_end, service_status, closed_notice, del_flag, create_time) " +
            "values (#{siteName}, #{dailyLimit}, #{depositDeadline}, #{promiseReturnTime}, " +
            "#{businessStart}, #{businessEnd}, #{serviceStatus}, #{closedNotice}, '0', now())")
    int insert(WashSite site);

    @Update("update wash_site set site_name = #{siteName}, daily_limit = #{dailyLimit}, " +
            "deposit_deadline = #{depositDeadline}, promise_return_time = #{promiseReturnTime}, " +
            "business_start = #{businessStart}, business_end = #{businessEnd}, " +
            "service_status = #{serviceStatus}, closed_notice = #{closedNotice}, update_time = now() " +
            "where site_id = #{siteId} and del_flag = '0'")
    int update(WashSite site);

    @Update("update wash_site set del_flag = '1', update_time = now() where site_id = #{siteId} and del_flag = '0'")
    int logicDelete(@Param("siteId") Long siteId);
}
