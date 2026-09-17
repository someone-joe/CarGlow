package com.ruoyi.wash.network.mapper;

import com.ruoyi.wash.network.domain.WashCabinet;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 机柜读写。下单需校验机柜存在且启用，并据此确定站点（产能按站点计算）。 */
public interface WashCabinetMapper {

    @Select("select cabinet_id, site_id, community_id, cabinet_name, enabled " +
            "from wash_cabinet where cabinet_id = #{cabinetId} and enabled = 'Y' and del_flag = '0' limit 1")
    WashCabinet selectEnabledById(@Param("cabinetId") Long cabinetId);
}
