package com.ruoyi.wash.goods.mapper;

import com.ruoyi.wash.goods.domain.WashService;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 服务项读写。下单时按 serviceId 取名称与价格做快照。 */
public interface WashServiceMapper {

    @Select("select service_id, service_name, price_amount, work_minutes, enabled " +
            "from wash_service where service_id = #{serviceId} and enabled = 'Y' and del_flag = '0' limit 1")
    WashService selectEnabledById(@Param("serviceId") Long serviceId);

    /** C 端服务项列表：只出上架且未删除的 */
    @Select("select service_id, service_name, price_amount, work_minutes, enabled " +
            "from wash_service where enabled = 'Y' and del_flag = '0' order by service_id asc")
    List<WashService> selectEnabledList();
}
