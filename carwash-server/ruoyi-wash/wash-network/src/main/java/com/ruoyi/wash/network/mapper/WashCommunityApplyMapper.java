package com.ruoyi.wash.network.mapper;

import com.ruoyi.wash.network.domain.WashCommunityApply;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 小区开通申请读写。审核流见 WashCommunityApplyService。 */
public interface WashCommunityApplyMapper {

    @Insert("insert into wash_community_apply (member_id, community_name, contact_phone, status, create_time, update_time, del_flag) " +
            "values (#{memberId}, #{communityName}, #{contactPhone}, 'PENDING', now(), now(), '0')")
    @Options(useGeneratedKeys = true, keyProperty = "applyId")
    int insert(WashCommunityApply apply);

    @Select("select apply_id, member_id, community_name, contact_phone, status, reject_reason, " +
            "community_id, site_id, date_format(create_time,'%Y-%m-%d %H:%i:%s') as create_time " +
            "from wash_community_apply where apply_id = #{applyId} and del_flag = '0' limit 1")
    WashCommunityApply selectById(@Param("applyId") Long applyId);

    @Select("<script>select apply_id, member_id, community_name, contact_phone, status, reject_reason, " +
            "community_id, site_id, date_format(create_time,'%Y-%m-%d %H:%i:%s') as create_time " +
            "from wash_community_apply where del_flag = '0' " +
            "<if test='keyword != null and keyword != \"\"'>and (community_name like concat('%', #{keyword}, '%') or contact_phone like concat('%', #{keyword}, '%'))</if> " +
            "<if test='status != null and status != \"\"'>and status = #{status}</if> " +
            "order by apply_id desc limit #{offset}, #{limit}</script>")
    List<WashCommunityApply> selectList(@Param("keyword") String keyword, @Param("status") String status,
                                       @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>select count(*) from wash_community_apply where del_flag = '0' " +
            "<if test='keyword != null and keyword != \"\"'>and (community_name like concat('%', #{keyword}, '%') or contact_phone like concat('%', #{keyword}, '%'))</if> " +
            "<if test='status != null and status != \"\"'>and status = #{status}</if></script>")
    int count(@Param("keyword") String keyword, @Param("status") String status);

    @Update("update wash_community_apply set status = #{status}, reject_reason = #{rejectReason}, " +
            "community_id = #{communityId}, site_id = #{siteId}, update_time = now() " +
            "where apply_id = #{applyId} and del_flag = '0'")
    int update(WashCommunityApply apply);
}
