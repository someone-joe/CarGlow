package com.ruoyi.wash.selfwash.mapper;

import com.ruoyi.wash.selfwash.domain.WashSelfwashOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/** 自助洗车订单读写。状态流转用条件更新（where 带 from 状态）保证幂等/不越态。 */
public interface WashSelfwashOrderMapper {

    @Insert("insert into wash_selfwash_order (order_no, member_id, site_id, workstation_id, service_id, service_name, plate_no, vehicle_type, " +
            "service_minutes, pay_amount, pay_channel, status, coupon_user_id, coupon_discount, added_seconds, pay_expire_at, del_flag, create_time) " +
            "values (#{orderNo}, #{memberId}, #{siteId}, #{workstationId}, #{serviceId}, #{serviceName}, #{plateNo}, #{vehicleType}, " +
            "#{serviceMinutes}, #{payAmount}, #{payChannel}, #{status}, #{couponUserId}, #{couponDiscount}, 0, #{payExpireAt}, '0', sysdate())")
    @Options(useGeneratedKeys = true, keyProperty = "orderId")
    int insert(WashSelfwashOrder order);

    @Select("select order_id, order_no, plate_no, member_id, site_id, workstation_id, service_id, service_name, vehicle_type, service_minutes, " +
            "pay_amount, pay_channel, coupon_user_id, coupon_discount, status, queue_no, started_at, expire_at, added_seconds, pay_expire_at, cancel_reason, create_time, update_time " +
            "from wash_selfwash_order where order_no = #{orderNo} and del_flag = '0' limit 1")
    WashSelfwashOrder selectByOrderNo(@Param("orderNo") String orderNo);

    @Select("select order_id, order_no, plate_no, member_id, site_id, workstation_id, service_id, service_name, vehicle_type, service_minutes, " +
            "pay_amount, pay_channel, coupon_user_id, coupon_discount, status, queue_no, started_at, expire_at, added_seconds, pay_expire_at, cancel_reason, create_time, update_time " +
            "from wash_selfwash_order where order_no = #{orderNo} and member_id = #{memberId} and del_flag = '0' limit 1")
    WashSelfwashOrder selectByMemberAndNo(@Param("memberId") Long memberId, @Param("orderNo") String orderNo);

    @Select({"<script>",
            "select order_id, order_no, plate_no, member_id, site_id, workstation_id, service_id, service_name, vehicle_type, service_minutes, ",
            "pay_amount, pay_channel, coupon_user_id, coupon_discount, status, queue_no, started_at, expire_at, added_seconds, pay_expire_at, cancel_reason, create_time, update_time ",
            "from wash_selfwash_order where member_id = #{memberId} and del_flag = '0' and status in ",
            "<foreach item='s' collection='statuses' open='(' separator=',' close=')'>#{s}</foreach> ",
            "order by order_id desc",
            "</script>"})
    List<WashSelfwashOrder> selectByMemberTab(@Param("memberId") Long memberId, @Param("statuses") List<String> statuses);

    @Update("update wash_selfwash_order set status = 'WAIT_USE', pay_channel = #{payChannel}, update_time = sysdate() " +
            "where order_no = #{orderNo} and status = 'WAIT_PAY'")
    int markPaid(@Param("orderNo") String orderNo, @Param("payChannel") String payChannel);

    @Update("update wash_selfwash_order set status = 'CANCELED', cancel_reason = #{reason}, update_time = sysdate() " +
            "where order_no = #{orderNo} and status in ('WAIT_PAY', 'WAIT_USE')")
    int markCanceled(@Param("orderNo") String orderNo, @Param("reason") String reason);

    /** 到店激活但前方有单：WAIT_USE → QUEUING，落排队序号 */
    @Update("update wash_selfwash_order set status = 'QUEUING', queue_no = #{queueNo}, update_time = sysdate() " +
            "where order_no = #{orderNo} and status = 'WAIT_USE'")
    int markQueued(@Param("orderNo") String orderNo, @Param("queueNo") int queueNo);

    /** 开始服务：QUEUING/WAIT_USE → SERVING，记开始与截止时间 */
    @Update("update wash_selfwash_order set status = 'SERVING', queue_no = null, started_at = #{startedAt}, expire_at = #{expireAt}, update_time = sysdate() " +
            "where order_no = #{orderNo} and status in ('QUEUING', 'WAIT_USE')")
    int markStarted(@Param("orderNo") String orderNo, @Param("startedAt") long startedAt, @Param("expireAt") long expireAt);

    /** 加时：SERVING 累加加时秒并延后截止 */
    @Update("update wash_selfwash_order set added_seconds = coalesce(added_seconds, 0) + #{addSeconds}, expire_at = #{expireAt}, update_time = sysdate() " +
            "where order_no = #{orderNo} and status = 'SERVING'")
    int addSeconds(@Param("orderNo") String orderNo, @Param("addSeconds") int addSeconds, @Param("expireAt") long expireAt);

    /** 到点/手动结束：SERVING → DONE */
    @Update("update wash_selfwash_order set status = 'DONE', update_time = sysdate() where order_no = #{orderNo} and status = 'SERVING'")
    int markDone(@Param("orderNo") String orderNo);

    /** 定时任务用：到点仍在服务的订单 */
    @Select("select order_id, order_no, plate_no, member_id, site_id, workstation_id, service_id, service_name, vehicle_type, service_minutes, " +
            "pay_amount, pay_channel, coupon_user_id, coupon_discount, status, queue_no, started_at, expire_at, added_seconds, pay_expire_at, cancel_reason, create_time, update_time " +
            "from wash_selfwash_order where status = 'SERVING' and expire_at is not null and expire_at <= #{now} and del_flag = '0' limit #{limit}")
    List<WashSelfwashOrder> selectExpiredServing(@Param("now") long now, @Param("limit") int limit);

    /** 某工位当前占用中的订单（SERVING；QUEUING 由上层另查） */
    @Select("select order_id, order_no, plate_no, member_id, site_id, workstation_id, service_id, service_name, vehicle_type, service_minutes, " +
            "pay_amount, pay_channel, coupon_user_id, coupon_discount, status, queue_no, started_at, expire_at, added_seconds, pay_expire_at, cancel_reason, create_time, update_time " +
            "from wash_selfwash_order where workstation_id = #{workstationId} and status in ('QUEUING', 'SERVING') and del_flag = '0' order by queue_no asc, order_id asc")
    List<WashSelfwashOrder> selectActiveByWorkstation(@Param("workstationId") Long workstationId);

    @Select({
        "<script>",
        "select order_id, order_no, plate_no, member_id, site_id, workstation_id, service_id, service_name, vehicle_type, service_minutes, ",
        "pay_amount, pay_channel, coupon_user_id, coupon_discount, status, queue_no, started_at, expire_at, added_seconds, pay_expire_at, cancel_reason, create_time, update_time ",
        "from wash_selfwash_order where del_flag = '0' ",
        "<if test='status != null and status != \"\"'> and status = #{status} </if>",
        "<if test='siteId != null'> and site_id = #{siteId} </if>",
        "<if test='workstationId != null'> and workstation_id = #{workstationId} </if>",
        "<if test='memberId != null'> and member_id = #{memberId} </if>",
        "<if test='orderNo != null and orderNo != \"\"'> and order_no like concat('%', #{orderNo}, '%') </if>",
        "order by order_id desc limit #{offset}, #{limit}",
        "</script>"
    })
    List<WashSelfwashOrder> selectAdminPage(Map<String, Object> params);

    @Select({
        "<script>",
        "select count(*) from wash_selfwash_order where del_flag = '0' ",
        "<if test='status != null and status != \"\"'> and status = #{status} </if>",
        "<if test='siteId != null'> and site_id = #{siteId} </if>",
        "<if test='workstationId != null'> and workstation_id = #{workstationId} </if>",
        "<if test='memberId != null'> and member_id = #{memberId} </if>",
        "<if test='orderNo != null and orderNo != \"\"'> and order_no like concat('%', #{orderNo}, '%') </if>",
        "</script>"
    })
    long countAdmin(Map<String, Object> params);
}
