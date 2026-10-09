package com.ruoyi.wash.network.mapper;

import com.ruoyi.wash.network.domain.WashSlot;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 格口读写。跨模块请走 WashSlotService；后台管理 CRUD 走 WashSlotAdminService。 */
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

    /** 机柜格口总数（C 端展示"共几个格口"） */
    @Select("select count(*) from wash_slot where cabinet_id = #{cabinetId} and del_flag = '0'")
    long countByCabinet(@Param("cabinetId") Long cabinetId);

    /** 机柜空闲格口数：C 端据此判断柜子是否已满，避免用户选了才发现没格子 */
    @Select("select count(*) from wash_slot where cabinet_id = #{cabinetId} and status = 'FREE' and del_flag = '0'")
    long countFreeByCabinet(@Param("cabinetId") Long cabinetId);

    @Select("select count(*) from wash_slot where cabinet_id = #{cabinetId} and slot_no = #{slotNo} and del_flag = '0'")
    long countByCabinetAndSlotNo(@Param("cabinetId") Long cabinetId, @Param("slotNo") String slotNo);

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

    /**
     * 带操作者的开箱留痕（角色 / 人员 / 来源端 / 结果）。
     */
    @Insert("insert into wash_slot_open_log (cabinet_id, slot_id, slot_no, order_no, member_id, action, open_code, " +
            "role, user_id, source, result, create_time) " +
            "values (#{cabinetId}, #{slotId}, #{slotNo}, #{orderNo}, #{memberId}, #{action}, #{openCode}, " +
            "#{role}, #{userId}, #{source}, #{result}, sysdate())")
    int insertOpenLogWithActor(@Param("cabinetId") Long cabinetId,
                               @Param("slotId") Long slotId,
                               @Param("slotNo") String slotNo,
                               @Param("orderNo") String orderNo,
                               @Param("memberId") Long memberId,
                               @Param("action") String action,
                               @Param("openCode") String openCode,
                               @Param("role") String role,
                               @Param("userId") Long userId,
                               @Param("source") String source,
                               @Param("result") String result);

    // ---------------- 后台管理 CRUD ----------------

    /** 后台格口列表：带机柜名 */
    @Select("<script>" +
            "select sl.slot_id, sl.cabinet_id, sl.slot_no, sl.status, sl.order_no, sl.member_id, " +
            "sl.create_time, sl.update_time, c.cabinet_name " +
            "from wash_slot sl left join wash_cabinet c on c.cabinet_id = sl.cabinet_id " +
            "where sl.del_flag = '0' " +
            "<if test='cabinetId != null'> and sl.cabinet_id = #{cabinetId} </if> " +
            "<if test='status != null and status != \"\"'> and sl.status = #{status} </if> " +
            "order by sl.cabinet_id asc, sl.slot_no asc limit #{offset}, #{limit}" +
            "</script>")
    List<WashSlot> selectList(@Param("cabinetId") Long cabinetId,
                              @Param("status") String status,
                              @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>select count(*) from wash_slot where del_flag = '0' " +
            "<if test='cabinetId != null'> and cabinet_id = #{cabinetId} </if> " +
            "<if test='status != null and status != \"\"'> and status = #{status} </if></script>")
    int count(@Param("cabinetId") Long cabinetId, @Param("status") String status);

    /** 机柜下拉用：某机柜下的格口（含停用，便于排查） */
    @Select("select slot_id, cabinet_id, slot_no, status, order_no, member_id, create_time, update_time " +
            "from wash_slot where cabinet_id = #{cabinetId} and del_flag = '0' order by slot_no asc")
    List<WashSlot> selectByCabinet(@Param("cabinetId") Long cabinetId);

    @Options(useGeneratedKeys = true, keyProperty = "slotId")
    @Insert("<script>insert into wash_slot " +
            "<trim prefix='(' suffix=')' suffixOverrides=','>" +
            "cabinet_id, slot_no, " +
            "<if test='status != null'>status,</if>" +
            "<if test='orderNo != null'>order_no,</if>" +
            "<if test='memberId != null'>member_id,</if>" +
            "del_flag, create_time" +
            "</trim> " +
            "<trim prefix='values (' suffix=')' suffixOverrides=','>" +
            "#{cabinetId}, #{slotNo}, " +
            "<if test='status != null'>#{status},</if>" +
            "<if test='orderNo != null'>#{orderNo},</if>" +
            "<if test='memberId != null'>#{memberId},</if>" +
            "'0', now()" +
            "</trim></script>")
    int insert(WashSlot slot);

    @Update("<script>update wash_slot " +
            "<set>" +
            "<if test='slotNo != null'>slot_no = #{slotNo},</if>" +
            "<if test='status != null'>status = #{status},</if>" +
            "<if test='orderNo != null'>order_no = #{orderNo},</if>" +
            "<if test='memberId != null'>member_id = #{memberId},</if>" +
            "update_time = now()" +
            "</set> where slot_id = #{slotId} and del_flag = '0'</script>")
    int update(WashSlot slot);

    @Update("update wash_slot set del_flag = '1', update_time = now() where slot_id = #{slotId} and del_flag = '0'")
    int logicDelete(@Param("slotId") Long slotId);

    // ---------------- 开箱留痕查询（后台审计） ----------------

    @Select("<script>" +
            "select id, cabinet_id, slot_id, slot_no, order_no, member_id, action, open_code, " +
            "role, user_id, source, result, create_time " +
            "from wash_slot_open_log where 1=1 " +
            "<if test='cabinetId != null'> and cabinet_id = #{cabinetId} </if> " +
            "<if test='slotId != null'> and slot_id = #{slotId} </if> " +
            "order by id desc limit #{offset}, #{limit}" +
            "</script>")
    List<java.util.Map<String, Object>> selectOpenLog(@Param("cabinetId") Long cabinetId,
                                                      @Param("slotId") Long slotId,
                                                      @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>select count(*) from wash_slot_open_log where 1=1 " +
            "<if test='cabinetId != null'> and cabinet_id = #{cabinetId} </if> " +
            "<if test='slotId != null'> and slot_id = #{slotId} </if></script>")
    int countOpenLog(@Param("cabinetId") Long cabinetId, @Param("slotId") Long slotId);
}
