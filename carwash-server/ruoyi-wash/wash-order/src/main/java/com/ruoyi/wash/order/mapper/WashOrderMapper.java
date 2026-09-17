package com.ruoyi.wash.order.mapper;

import com.ruoyi.wash.common.statemachine.OrderStatusLog;
import com.ruoyi.wash.order.domain.WashOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 订单查询。写入类操作（下单/状态流转）后续单独加方法，并必须走状态机。
 * 禁止其他模块直接注入本 Mapper。
 */
public interface WashOrderMapper {

    List<WashOrder> selectPageByMember(@Param("memberId") Long memberId,
                                       @Param("statuses") List<String> statuses,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    long countByMember(@Param("memberId") Long memberId,
                       @Param("statuses") List<String> statuses);

    WashOrder selectByOrderNo(@Param("orderNo") String orderNo);

    int insertWashOrder(WashOrder order);

    @Select("select status from wash_order where order_no = #{orderNo} and del_flag = '0' limit 1")
    String selectStatusByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 乐观更新：仅当库中状态仍为 from 时才更新为 to，并发改过时返回 0。
     * 返回值即受影响行数，状态机据此判定 CAS 是否成功。
     */
    @Update("update wash_order set status = #{to}, update_time = sysdate() " +
            "where order_no = #{orderNo} and status = #{from} and del_flag = '0'")
    int updateStatus(@Param("orderNo") String orderNo, @Param("from") String from, @Param("to") String to);

    /** 后台全量分页：orderNo 模糊匹配，status 精确匹配（空表示不过滤） */
    List<WashOrder> selectAdminPage(@Param("orderNo") String orderNo,
                                    @Param("status") String status,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    long countAdmin(@Param("orderNo") String orderNo, @Param("status") String status);

    /** 流转日志按时间正序：时间轴与后台追溯都读它，只增不删 */
    @Select("select id, order_id, from_status, to_status, event, operator_type, operator_id, source, reason, create_time " +
            "from wash_order_status_log where order_id = #{orderId} order by create_time asc, id asc")
    List<OrderStatusLog> selectLogsByOrderId(@Param("orderId") Long orderId);

    @Insert("insert into wash_order_status_log " +
            "(order_id, from_status, to_status, event, operator_type, operator_id, source, reason, create_time) " +
            "values (#{orderId}, #{fromStatus}, #{toStatus}, #{event}, #{operatorType}, #{operatorId}, #{source}, #{reason}, sysdate())")
    int insertStatusLog(OrderStatusLog log);
}
