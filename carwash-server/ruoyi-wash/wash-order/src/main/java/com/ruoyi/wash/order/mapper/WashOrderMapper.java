package com.ruoyi.wash.order.mapper;

import com.ruoyi.wash.order.domain.WashOrder;
import org.apache.ibatis.annotations.Param;

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
}
