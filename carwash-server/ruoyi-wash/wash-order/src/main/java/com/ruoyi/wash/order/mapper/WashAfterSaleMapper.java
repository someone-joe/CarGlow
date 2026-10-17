package com.ruoyi.wash.order.mapper;

import com.ruoyi.wash.order.domain.WashAfterSale;
import com.ruoyi.wash.order.dto.AdminAfterSaleVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 售后单读写。 */
public interface WashAfterSaleMapper {

    @Insert("insert into wash_after_sale (after_sale_no, order_no, member_id, type, reason, status, amount, del_flag, create_time) " +
            "values (#{afterSaleNo}, #{orderNo}, #{memberId}, #{type}, #{reason}, #{status}, #{amount}, '0', sysdate())")
    int insertAfterSale(WashAfterSale afterSale);

    /** 按工单号查（含处置留痕列） */
    @Select("select after_sale_id, after_sale_no, order_no, member_id, type, reason, status, amount, " +
            "handler_by, handle_remark, handle_time, UNIX_TIMESTAMP(create_time) * 1000 as create_time " +
            "from wash_after_sale where after_sale_no = #{afterSaleNo} and del_flag = '0' limit 1")
    WashAfterSale selectByNo(@Param("afterSaleNo") String afterSaleNo);

    /** C 端「我的售后」：只返回本人工单，按创建时间倒序 */
    @Select("<script>" +
            "select after_sale_id, after_sale_no, order_no, member_id, type, reason, status, amount, " +
            "handler_by, handle_remark, handle_time, UNIX_TIMESTAMP(create_time) * 1000 as create_time " +
            "from wash_after_sale where member_id = #{memberId} and del_flag = '0' " +
            "<if test='status != null'> and status = #{status} </if>" +
            "order by create_time desc" +
            "</script>")
    List<WashAfterSale> selectMyList(@Param("memberId") Long memberId, @Param("status") String status);

    /** 后台列表：带订单上下文（orderStatus / serviceName），运营处置时要能看到现在什么状态 */
    @Select("<script>" +
            "select s.after_sale_no, s.order_no, s.member_id, s.type, s.reason, s.status, s.amount, " +
            "s.handler_by, s.handle_remark, s.handle_time, UNIX_TIMESTAMP(s.create_time) * 1000 as create_time, " +
            "o.status as order_status, o.service_name " +
            "from wash_after_sale s left join wash_order o on o.order_no = s.order_no " +
            "where s.del_flag = '0' " +
            "<if test='afterSaleNo != null'> and s.after_sale_no = #{afterSaleNo} </if>" +
            "<if test='type != null'> and s.type = #{type} </if>" +
            "<if test='status != null'> and s.status = #{status} </if>" +
            "order by s.create_time desc limit #{offset}, #{pageSize}" +
            "</script>")
    List<AdminAfterSaleVO> selectAdminList(@Param("afterSaleNo") String afterSaleNo,
                                           @Param("type") String type,
                                           @Param("status") String status,
                                           @Param("offset") int offset,
                                           @Param("pageSize") int pageSize);

    @Select("<script>" +
            "select count(*) from wash_after_sale s where s.del_flag = '0' " +
            "<if test='afterSaleNo != null'> and s.after_sale_no = #{afterSaleNo} </if>" +
            "<if test='type != null'> and s.type = #{type} </if>" +
            "<if test='status != null'> and s.status = #{status} </if>" +
            "</script>")
    long countAdmin(@Param("afterSaleNo") String afterSaleNo,
                    @Param("type") String type,
                    @Param("status") String status);

    /**
     * 处置（同意 / 驳回）。SQL 带 status = 'INIT' 做并发幂等：
     * 两个运营同时点同一工单时只有一个能更新成功，后一个返回 0 行，不会覆盖前者的处置结果。
     */
    @Update("update wash_after_sale set status = #{status}, handler_by = #{handlerBy}, " +
            "handle_remark = #{handleRemark}, handle_time = #{handleTime}, update_time = sysdate() " +
            "where after_sale_no = #{afterSaleNo} and status = 'INIT' and del_flag = '0'")
    int updateHandle(@Param("afterSaleNo") String afterSaleNo,
                     @Param("status") String status,
                     @Param("handlerBy") String handlerBy,
                     @Param("handleRemark") String handleRemark,
                     @Param("handleTime") Long handleTime);

    /** 退款已经到账 → 工单收尾为 DONE（退款成功事件回调，幂等：只动 PROCESSING 的 REFUND 工单） */
    @Update("update wash_after_sale set status = 'DONE', handle_time = #{handleTime}, update_time = sysdate() " +
            "where order_no = #{orderNo} and type = 'REFUND' and status = 'PROCESSING' and del_flag = '0'")
    int finishByRefundSuccess(@Param("orderNo") String orderNo, @Param("handleTime") Long handleTime);
}
