package com.ruoyi.wash.promo.mapper;

import com.ruoyi.wash.promo.domain.WashCouponUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 用户优惠券读写。 */
public interface WashCouponUserMapper {

    @Insert("insert into wash_coupon_user(template_id, member_id, status, obtain_time, expire_time, create_time) " +
            "values(#{templateId}, #{memberId}, #{status}, #{obtainTime}, #{expireTime}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "couponUserId")
    int insert(WashCouponUser user);

    @Select("select coupon_user_id, template_id, member_id, status, order_no, obtain_time, used_time, expire_time " +
            "from wash_coupon_user " +
            "where member_id = #{memberId} and (#{status} is null or status = #{status}) and del_flag = '0' " +
            "order by obtain_time desc")
    List<WashCouponUser> selectMy(@Param("memberId") long memberId, @Param("status") String status);

    @Select("select count(1) from wash_coupon_user " +
            "where template_id = #{templateId} and member_id = #{memberId} and del_flag = '0'")
    int countByTemplateAndMember(@Param("templateId") long templateId, @Param("memberId") long memberId);

    /** 核销：仅当仍是 UNUSED 才置为 USED，避免重复核销。 */
    @Update("update wash_coupon_user set status = 'USED', order_no = #{orderNo}, used_time = #{now} " +
            "where coupon_user_id = #{id} and status = 'UNUSED' and del_flag = '0'")
    int updateUsed(@Param("id") long id, @Param("orderNo") String orderNo, @Param("now") long now);
}
