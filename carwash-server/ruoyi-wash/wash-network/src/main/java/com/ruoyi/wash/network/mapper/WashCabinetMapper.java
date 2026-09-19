package com.ruoyi.wash.network.mapper;

import com.ruoyi.wash.network.domain.WashCabinet;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 机柜读写。下单需校验机柜存在且启用，并据此确定站点（产能按站点计算）。 */
public interface WashCabinetMapper {

    @Select("select cabinet_id, site_id, community_id, cabinet_name, enabled " +
            "from wash_cabinet where cabinet_id = #{cabinetId} and enabled = 'Y' and del_flag = '0' limit 1")
    WashCabinet selectEnabledById(@Param("cabinetId") Long cabinetId);

    /** 按主键查（不问启用状态）：订单详情补全柜名时，柜子可能已停用，仍要显示历史名称 */
    @Select("select cabinet_id, site_id, community_id, cabinet_name, enabled " +
            "from wash_cabinet where cabinet_id = #{cabinetId} and del_flag = '0' limit 1")
    WashCabinet selectById(@Param("cabinetId") Long cabinetId);

    /** C 端机柜列表：按小区过滤 + 按名称模糊搜（楼栋/单元） */
    @Select("<script>" +
            "select cabinet_id, site_id, community_id, cabinet_name, enabled " +
            "from wash_cabinet where enabled = 'Y' and del_flag = '0' " +
            "<if test='communityId != null'> and community_id = #{communityId} </if> " +
            "<if test='keyword != null and keyword != \"\"'> and cabinet_name like concat('%', #{keyword}, '%') </if> " +
            "order by cabinet_id asc" +
            "</script>")
    List<WashCabinet> selectEnabledList(@Param("communityId") Long communityId, @Param("keyword") String keyword);
}
