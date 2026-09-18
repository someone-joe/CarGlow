package com.ruoyi.wash.network.mapper;

import com.ruoyi.wash.network.domain.WashSlot;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 格口读写。跨模块请走 WashSlotService，禁止别处直接注入本 Mapper。 */
public interface WashSlotMapper {

    @Select("select slot_id, cabinet_id, slot_no, status, order_no, member_id, create_time, update_time " +
            "from wash_slot where slot_id = #{slotId} limit 1")
    WashSlot selectById(@Param("slotId") Long slotId);

    @Select("select slot_id, cabinet_id, slot_no, status, order_no, member_id, create_time, update_time " +
            "from wash_slot where order_no = #{orderNo} and del_flag = '0' limit 1")
    WashSlot selectByOrderNo(@Param("orderNo") String orderNo);

    @Select("select slot_id, cabinet_id, slot_no, status, order_no, member_id, create_time, update_time " +
            "from wash_slot where cabinet_id = #{cabinetId} and status = 'FREE' and del_flag = '0' " +
            "order by slot_no asc limit #{limit}")
    List<WashSlot> selectFree(@Param("cabinetId") Long cabinetId, @Param("limit") int limit);

    /** 乐观占用：只有当前状态符合预期才更新成功，返回影响行数 */
    @Update("update wash_slot set status = #{toStatus}, order_no = #{orderNo}, member_id = #{memberId}, update_time = sysdate() " +
            "where slot_id = #{slotId} and status = #{fromStatus} and del_flag = '0'")
    int updateStatus(@Param("slotId") Long slotId,
                     @Param("fromStatus") String fromStatus,
                     @Param("toStatus") String toStatus,
                     @Param("orderNo") String orderNo,
                     @Param("memberId") Long memberId);

    /** 按订单释放：预占或占用中的格子都释放 */
    @Update("update wash_slot set status = 'FREE', order_no = null, member_id = null, update_time = sysdate() " +
            "where order_no = #{orderNo} and status in ('RESERVED', 'OCCUPIED') and del_flag = '0'")
    int releaseByOrderNo(@Param("orderNo") String orderNo);

    @Insert("insert into wash_slot_open_log (cabinet_id, slot_id, slot_no, order_no, member_id, action, open_code, create_time) " +
            "values (#{cabinetId}, #{slotId}, #{slotNo}, #{orderNo}, #{memberId}, #{action}, #{openCode}, sysdate())")
    int insertOpenLog(@Param("cabinetId") Long cabinetId,
                      @Param("slotId") Long slotId,
                      @Param("slotNo") String slotNo,
                      @Param("orderNo") String orderNo,
                      @Param("memberId") Long memberId,
                      @Param("action") String action,
                      @Param("openCode") String openCode);
}
