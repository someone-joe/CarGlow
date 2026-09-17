package com.ruoyi.wash.goods.mapper;

import com.ruoyi.wash.goods.domain.WashService;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 服务项读写。下单时按 serviceId 取名称与价格做快照。 */
public interface WashServiceMapper {

    @Select("select service_id, service_name, price_amount, work_minutes, enabled " +
            "from wash_service where service_id = #{serviceId} and enabled = 'Y' and del_flag = '0' limit 1")
    WashService selectEnabledById(@Param("serviceId") Long serviceId);
}
