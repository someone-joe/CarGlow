package com.ruoyi.wash.promo.mapper;

import com.ruoyi.wash.promo.domain.WashCouponUser;
import com.ruoyi.wash.promo.dto.AdminCouponRecordVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 用户优惠券读写。 */
public interface WashCouponUserMapper {

    @Insert("insert into wash_coupon_user(template_id, member_id, batch_id, status, obtain_time, expire_time, create_time) " +
            "values(#{templateId}, #{memberId}, #{batchId}, #{status}, #{obtainTime}, #{expireTime}, now())")
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

    @Select("select coupon_user_id, template_id, member_id, status, order_no, obtain_time, used_time, expire_time " +
            "from wash_coupon_user where coupon_user_id = #{id} and del_flag = '0'")
    WashCouponUser selectById(@Param("id") long id);

    /** 核销：仅当仍是 UNUSED 才置为 USED，避免重复核销。 */
    @Update("update wash_coupon_user set status = 'USED', order_no = #{orderNo}, used_time = #{now} " +
            "where coupon_user_id = #{id} and status = 'UNUSED' and del_flag = '0'")
    int updateUsed(@Param("id") long id, @Param("orderNo") String orderNo, @Param("now") long now);

    /**
     * 按订单号找回已核销的券（取消返还用）。
     *
     * <p>为什么用 order_no 反查而不是在订单表存 coupon_user_id：wash_order 未冗余券字段
     * （建表时未规划），而核销时已把 order_no 写进 wash_coupon_user，反查即可，不动表结构。
     * 一张单最多用一张券（order 创建时 applyToOrder 口径），故 limit 1。
     */
    @Select("select coupon_user_id, template_id, member_id, status, order_no, obtain_time, used_time, expire_time " +
            "from wash_coupon_user where order_no = #{orderNo} and status = 'USED' and del_flag = '0' limit 1")
    WashCouponUser selectUsedByOrderNo(@Param("orderNo") String orderNo);

    /** 返还：仅已核销的券置回 UNUSED 并清空订单号与核销时间（幂等，重复调用更新 0 行）。 */
    @Update("update wash_coupon_user set status = 'UNUSED', order_no = null, used_time = null, update_time = sysdate() " +
            "where coupon_user_id = #{id} and status = 'USED' and del_flag = '0'")
    int updateUnused(@Param("id") long id);

    // ---------------- 后台 ----------------

    /**
     * 领用 / 核销记录：join 模板取名字，join 会员取**脱敏后**的手机号。
     *
     * <p>手机号在 SQL 层就脱敏，不把明文带进 Java 层（与后台订单 / 会员列表同一口径）。
     * EXPIRED 是计算值而不是库里状态：库里存的是 UNUSED，只是已过有效期，按 EXPIRED 展示。
     */
    @Select("<script>" +
            "select u.coupon_user_id, u.template_id as coupon_template_id, u.member_id, u.status, u.order_no, " +
            "u.obtain_time, u.used_time, u.expire_time, t.name as template_name, " +
            "case when m.phone is null or length(m.phone) &lt; 11 then m.phone " +
            "     else concat(substring(m.phone, 1, 3), '****', substring(m.phone, 8, 4)) end as member_phone " +
            "from wash_coupon_user u " +
            "left join wash_coupon_template t on t.coupon_template_id = u.template_id " +
            "left join wash_member m on m.member_id = u.member_id " +
            "where u.del_flag = '0' and u.batch_id is null " +
            "<if test='templateId != null'> and u.template_id = #{templateId} </if>" +
            "<if test='phone != null and phone != &quot;&quot;'> and m.phone like concat('%', #{phone}, '%') </if>" +
            "<choose>" +
            "  <when test='status == &quot;EXPIRED&quot;'> and u.status = 'UNUSED' and u.expire_time &lt; #{now} </when>" +
            "  <when test='status != null'> and u.status = #{status} </when>" +
            "</choose>" +
            "order by u.obtain_time desc limit #{offset}, #{pageSize}" +
            "</script>")
    List<AdminCouponRecordVO> selectAdminRecords(@Param("templateId") Long templateId,
                                                @Param("phone") String phone,
                                                @Param("status") String status,
                                                @Param("now") long now,
                                                @Param("offset") int offset,
                                                @Param("pageSize") int pageSize);

    @Select("<script>" +
            "select count(*) from wash_coupon_user u " +
            "left join wash_member m on m.member_id = u.member_id " +
            "where u.del_flag = '0' and u.batch_id is null " +
            "<if test='templateId != null'> and u.template_id = #{templateId} </if>" +
            "<if test='phone != null and phone != &quot;&quot;'> and m.phone like concat('%', #{phone}, '%') </if>" +
            "<choose>" +
            "  <when test='status == &quot;EXPIRED&quot;'> and u.status = 'UNUSED' and u.expire_time &lt; #{now} </when>" +
            "  <when test='status != null'> and u.status = #{status} </when>" +
            "</choose>" +
            "</script>")
    long countAdminRecords(@Param("templateId") Long templateId,
                           @Param("phone") String phone,
                           @Param("status") String status,
                           @Param("now") long now);
}
