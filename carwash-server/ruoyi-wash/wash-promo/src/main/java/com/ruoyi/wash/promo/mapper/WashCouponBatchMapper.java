package com.ruoyi.wash.promo.mapper;

import com.ruoyi.wash.promo.domain.WashCouponBatch;
import com.ruoyi.wash.promo.dto.AdminCouponBatchVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 优惠券批量发放批次读写。 */
public interface WashCouponBatchMapper {

    @Insert("insert into wash_coupon_batch(template_id, per_count, operator, audience_type, community_id, " +
            "community_name, time_from, time_to, total_count, success_count, skipped_count, failed_count) " +
            "values(#{templateId}, #{perCount}, #{operator}, #{audienceType}, #{communityId}, " +
            "#{communityName}, #{timeFrom}, #{timeTo}, #{totalCount}, #{successCount}, #{skippedCount}, #{failedCount})")
    @Options(useGeneratedKeys = true, keyProperty = "batchId")
    int insert(WashCouponBatch batch);

    @Select("select batch_id, template_id, per_count, operator, audience_type, community_id, community_name, " +
            "time_from, time_to, total_count, success_count, skipped_count, failed_count, " +
            "UNIX_TIMESTAMP(create_time) * 1000 as create_time " +
            "from wash_coupon_batch where batch_id = #{id} and del_flag = '0'")
    WashCouponBatch selectById(@Param("id") long id);

    @Select("<script>" +
            "select b.batch_id, b.template_id as coupon_template_id, t.name as template_name, b.per_count, " +
            "b.operator, b.audience_type, b.community_id, b.community_name, b.time_from, b.time_to, " +
            "b.total_count, b.success_count, b.skipped_count, b.failed_count, " +
            "UNIX_TIMESTAMP(b.create_time) * 1000 as create_time " +
            "from wash_coupon_batch b " +
            "left join wash_coupon_template t on t.coupon_template_id = b.template_id " +
            "where b.del_flag = '0' " +
            "<if test='templateId != null'> and b.template_id = #{templateId} </if>" +
            "order by b.batch_id desc limit #{offset}, #{pageSize}" +
            "</script>")
    List<AdminCouponBatchVO> selectAdminList(@Param("templateId") Long templateId,
                                             @Param("offset") int offset,
                                             @Param("pageSize") int pageSize);

    @Select("<script>" +
            "select count(*) from wash_coupon_batch where del_flag = '0' " +
            "<if test='templateId != null'> and template_id = #{templateId} </if>" +
            "</script>")
    long countAdmin(@Param("templateId") Long templateId);

    @Update("update wash_coupon_batch set total_count = #{totalCount}, success_count = #{successCount}, " +
            "skipped_count = #{skippedCount}, failed_count = #{failedCount} where batch_id = #{batchId}")
    int updateCounts(@Param("batchId") long batchId,
                     @Param("successCount") int successCount,
                     @Param("skippedCount") int skippedCount,
                     @Param("failedCount") int failedCount,
                     @Param("totalCount") int totalCount);

    /**
     * 批量发放受众会员（去重）。会员表无 community_id，小区取「常用停车位置 ∪ 车辆常停小区」，
     * 按 member_id 去重，不会因交集重复发。communityId / timeFrom / timeTo 全为空 = 全量会员。
     */
    @Select("<script>" +
            "select distinct m.member_id from wash_member m where m.del_flag = '0' " +
            "<if test='communityId != null'> and ( " +
            "  exists (select 1 from wash_park_location p where p.member_id = m.member_id " +
            "          and p.community_id = #{communityId} and p.del_flag = '0') " +
            "  or exists (select 1 from wash_vehicle v where v.member_id = m.member_id " +
            "          and v.community_id = #{communityId} and v.del_flag = '0') ) </if>" +
            "<if test='timeFrom != null'> and UNIX_TIMESTAMP(m.create_time) * 1000 &gt;= #{timeFrom} </if>" +
            "<if test='timeTo != null'> and UNIX_TIMESTAMP(m.create_time) * 1000 &lt;= #{timeTo} </if>" +
            "order by m.member_id" +
            "</script>")
    List<Long> selectAudienceMemberIds(@Param("communityId") Long communityId,
                                      @Param("timeFrom") Long timeFrom,
                                      @Param("timeTo") Long timeTo);

    /** 小区名快照（审计用）。 */
    @Select("select community_name from wash_community where community_id = #{communityId} and del_flag = '0'")
    String selectCommunityName(@Param("communityId") long communityId);
}
