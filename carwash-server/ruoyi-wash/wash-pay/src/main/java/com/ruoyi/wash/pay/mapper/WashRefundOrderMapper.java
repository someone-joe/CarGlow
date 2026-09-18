package com.ruoyi.wash.pay.mapper;

import com.ruoyi.wash.pay.domain.WashRefundOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 退款单读写。只增不删（财务留痕），状态更新走 updateStatus。 */
public interface WashRefundOrderMapper {

    @Select("select refund_id, refund_no, order_no, member_id, amount, reason, status, provider, " +
            "provider_trade_no, fail_reason, retry_count, create_time, update_time " +
            "from wash_order_refund where refund_no = #{refundNo} limit 1")
    WashRefundOrder selectByRefundNo(@Param("refundNo") String refundNo);

    @Select("select refund_id, refund_no, order_no, member_id, amount, reason, status, provider, " +
            "provider_trade_no, fail_reason, retry_count, create_time, update_time " +
            "from wash_order_refund where order_no = #{orderNo} order by create_time desc")
    List<WashRefundOrder> selectByOrderNo(@Param("orderNo") String orderNo);

    /** 待重试的退款单：INIT 或 FAILED，且重试次数未超限 */
    @Select("select refund_id, refund_no, order_no, member_id, amount, reason, status, provider, " +
            "provider_trade_no, fail_reason, retry_count, create_time, update_time " +
            "from wash_order_refund where status in ('INIT', 'FAILED') and retry_count < #{maxRetry} " +
            "and del_flag = '0' order by create_time asc limit #{limit}")
    List<WashRefundOrder> selectPending(@Param("maxRetry") int maxRetry, @Param("limit") int limit);

    @Insert("insert into wash_order_refund (refund_no, order_no, member_id, amount, reason, status, provider, retry_count, create_time) " +
            "values (#{refundNo}, #{orderNo}, #{memberId}, #{amount}, #{reason}, 'INIT', #{provider}, 0, sysdate())")
    int insertRefund(WashRefundOrder refund);

    @Update("update wash_order_refund set status = #{status}, provider_trade_no = #{providerTradeNo}, " +
            "fail_reason = #{failReason}, retry_count = #{retryCount}, update_time = sysdate() " +
            "where refund_no = #{refundNo}")
    int updateResult(@Param("refundNo") String refundNo,
                     @Param("status") String status,
                     @Param("providerTradeNo") String providerTradeNo,
                     @Param("failReason") String failReason,
                     @Param("retryCount") Integer retryCount);
}
