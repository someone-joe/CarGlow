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

    @Insert("<script>insert into wash_site " +
            "<trim prefix='(' suffix=')' suffixOverrides=','>" +
            "site_name, daily_limit, " +
            "<if test='depositDeadline != null'>deposit_deadline,</if>" +
            "<if test='promiseReturnTime != null'>promise_return_time,</if>" +
            "<if test='businessStart != null'>business_start,</if>" +
            "<if test='businessEnd != null'>business_end,</if>" +
            "<if test='serviceStatus != null'>service_status,</if>" +
            "<if test='closedNotice != null'>closed_notice,</if>" +
            "del_flag, create_time" +
            "</trim> " +
            "<trim prefix='values (' suffix=')' suffixOverrides=','>" +
            "#{siteName}, #{dailyLimit}, " +
            "<if test='depositDeadline != null'>#{depositDeadline},</if>" +
            "<if test='promiseReturnTime != null'>#{promiseReturnTime},</if>" +
            "<if test='businessStart != null'>#{businessStart},</if>" +
            "<if test='businessEnd != null'>#{businessEnd},</if>" +
            "<if test='serviceStatus != null'>#{serviceStatus},</if>" +
            "<if test='closedNotice != null'>#{closedNotice},</if>" +
            "'0', now()" +
            "</trim></script>")
    int insert(WashSite site);

    @Update("<script>update wash_site " +
            "<set>" +
            "<if test='siteName != null'>site_name = #{siteName},</if>" +
            "<if test='dailyLimit != null'>daily_limit = #{dailyLimit},</if>" +
            "<if test='depositDeadline != null'>deposit_deadline = #{depositDeadline},</if>" +
            "<if test='promiseReturnTime != null'>promise_return_time = #{promiseReturnTime},</if>" +
            "<if test='businessStart != null'>business_start = #{businessStart},</if>" +
            "<if test='businessEnd != null'>business_end = #{businessEnd},</if>" +
            "<if test='serviceStatus != null'>service_status = #{serviceStatus},</if>" +
            "<if test='closedNotice != null'>closed_notice = #{closedNotice},</if>" +
            "update_time = now()" +
            "</set> where site_id = #{siteId} and del_flag = '0'</script>")
    int update(WashSite site);

    @Update("update wash_site set del_flag = '1', update_time = now() where site_id = #{siteId} and del_flag = '0'")
    int logicDelete(@Param("siteId") Long siteId);
}
