package com.ruoyi.wash.promo.mapper;

import com.ruoyi.wash.promo.domain.WashCouponBatchItem;
import com.ruoyi.wash.promo.dto.AdminCouponBatchItemVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 优惠券批量发放明细读写。 */
public interface WashCouponBatchItemMapper {

    @Insert("insert into wash_coupon_batch_item(batch_id, template_id, member_id, result, reason, granted_count) " +
            "values(#{batchId}, #{templateId}, #{memberId}, #{result}, #{reason}, #{grantedCount})")
    @Options(useGeneratedKeys = true, keyProperty = "itemId")
    int insert(WashCouponBatchItem item);

    /** 补发：更新同一会员在该批次的当前结果（UNIQUE(batch_id, member_id)，不新增行）。 */
    @Update("update wash_coupon_batch_item set result = #{result}, reason = #{reason}, granted_count = #{grantedCount} " +
            "where batch_id = #{batchId} and member_id = #{memberId}")
    int updateResult(@Param("batchId") long batchId,
                     @Param("memberId") long memberId,
                     @Param("result") String result,
                     @Param("reason") String reason,
                     @Param("grantedCount") int grantedCount);

    @Select("<script>" +
            "select i.item_id, i.member_id, i.result, i.reason, i.granted_count, " +
            "UNIX_TIMESTAMP(i.create_time) * 1000 as create_time, " +
            "case when m.phone is null or length(m.phone) &lt; 11 then m.phone " +
            "     else concat(substring(m.phone, 1, 3), '****', substring(m.phone, 8, 4)) end as member_phone " +
            "from wash_coupon_batch_item i " +
            "left join wash_member m on m.member_id = i.member_id " +
            "where i.batch_id = #{batchId} " +
            "<if test='result != null and result != &quot;&quot;'> and i.result = #{result} </if>" +
            "order by i.item_id limit #{offset}, #{pageSize}" +
            "</script>")
    List<AdminCouponBatchItemVO> selectByBatch(@Param("batchId") long batchId,
                                              @Param("result") String result,
                                              @Param("offset") int offset,
                                              @Param("pageSize") int pageSize);

    @Select("<script>" +
            "select count(*) from wash_coupon_batch_item where batch_id = #{batchId} " +
            "<if test='result != null and result != &quot;&quot;'> and result = #{result} </if>" +
            "</script>")
    long countByBatch(@Param("batchId") long batchId, @Param("result") String result);

    @Select("select count(*) from wash_coupon_batch_item where batch_id = #{batchId} and result = #{result}")
    int countByResult(@Param("batchId") long batchId, @Param("result") String result);
}
