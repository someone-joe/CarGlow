package com.ruoyi.wash.promo.mapper;

import com.ruoyi.wash.promo.domain.WashCouponTemplate;
import com.ruoyi.wash.promo.dto.AdminCouponTemplateVO;
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

    /** 按类型取一个可领模板（NEWBIE 新人券 / INVITE 邀请奖励券） */
    @Select("select coupon_template_id, name, type, threshold_amount, discount_amount, total, issued, " +
            "per_limit, start_time, end_time, status " +
            "from wash_coupon_template " +
            "where type = #{type} and status = 'Y' and start_time <= #{now} and end_time >= #{now} and del_flag = '0' " +
            "order by coupon_template_id asc limit 1")
    WashCouponTemplate selectAvailableByType(@Param("type") String type, @Param("now") long now);

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

    // ---------------- 后台 ----------------

    /** 后台列表：含已下架（运营要能找回），不含已删除 */
    @Select("<script>" +
            "select coupon_template_id, name, type, threshold_amount, discount_amount, total, issued, " +
            "per_limit, start_time, end_time, status, UNIX_TIMESTAMP(create_time) * 1000 as create_time " +
            "from wash_coupon_template where del_flag = '0' " +
            "<if test='name != null'> and name like concat('%', #{name}, '%') </if>" +
            "<if test='type != null'> and type = #{type} </if>" +
            "<if test='status != null'> and status = #{status} </if>" +
            "order by coupon_template_id desc limit #{offset}, #{pageSize}" +
            "</script>")
    List<AdminCouponTemplateVO> selectAdminList(@Param("name") String name,
                                                @Param("type") String type,
                                                @Param("status") String status,
                                                @Param("offset") int offset,
                                                @Param("pageSize") int pageSize);

    @Select("<script>" +
            "select count(*) from wash_coupon_template where del_flag = '0' " +
            "<if test='name != null'> and name like concat('%', #{name}, '%') </if>" +
            "<if test='type != null'> and type = #{type} </if>" +
            "<if test='status != null'> and status = #{status} </if>" +
            "</script>")
    long countAdmin(@Param("name") String name, @Param("type") String type, @Param("status") String status);

    /**
     * 改资料。**SQL 里刻意不出现 total / issued**：模板一旦发行过，这两列就是历史账，
     * 改了会让"已领 / 剩余"和真实发出去的券对不上。要加量请发新模板，别改老的。
     */
    @Update("update wash_coupon_template set name = #{name}, type = #{type}, " +
            "threshold_amount = #{thresholdAmount}, discount_amount = #{discountAmount}, " +
            "per_limit = #{perLimit}, start_time = #{startTime}, end_time = #{endTime} " +
            "where coupon_template_id = #{couponTemplateId} and del_flag = '0'")
    int updateTemplate(WashCouponTemplate template);

    /** 上下架：与改资料分成两个接口，避免"改个名字顺手把券下架了" */
    @Update("update wash_coupon_template set status = #{status} " +
            "where coupon_template_id = #{id} and del_flag = '0'")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 逻辑删除：已发出去的券还引用着模板名与金额快照，不做物理删 */
    @Update("update wash_coupon_template set del_flag = '2' where coupon_template_id = #{id}")
    int logicDelete(@Param("id") Long id);
}
