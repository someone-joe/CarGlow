package com.ruoyi.wash.promo.mapper;

import com.ruoyi.wash.promo.domain.WashCouponTemplate;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 优惠券模板读写。 */
public interface WashCouponTemplateMapper {

    @Select("select coupon_template_id, name, type, threshold_amount, discount_amount, total, issued, " +
            "per_limit, start_time, end_time, status " +
            "from wash_coupon_template " +
            "where status = 'Y' and start_time <= #{now} and end_time >= #{now} and del_flag = '0'")
    List<WashCouponTemplate> selectAvailable(@Param("now") long now);

    @Select("select coupon_template_id, name, type, threshold_amount, discount_amount, total, issued, " +
            "per_limit, start_time, end_time, status " +
            "from wash_coupon_template where coupon_template_id = #{id} and del_flag = '0'")
    WashCouponTemplate selectById(@Param("id") long id);

    /** 乐观扣减库存：仅当 issued < total 时 +1，返回影响行数（0 = 已领完）。 */
    @Update("update wash_coupon_template set issued = issued + 1 " +
            "where coupon_template_id = #{id} and issued < total")
    int incrementIssued(@Param("id") long id);

    @Insert("insert into wash_coupon_template(name, type, threshold_amount, discount_amount, total, " +
            "per_limit, start_time, end_time, status, create_time) " +
            "values(#{name}, #{type}, #{thresholdAmount}, #{discountAmount}, #{total}, " +
            "#{perLimit}, #{startTime}, #{endTime}, #{status}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "couponTemplateId")
    int insert(WashCouponTemplate template);
}
