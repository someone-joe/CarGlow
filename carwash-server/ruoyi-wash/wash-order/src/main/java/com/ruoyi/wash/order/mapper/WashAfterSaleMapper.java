package com.ruoyi.wash.order.mapper;

import com.ruoyi.wash.order.domain.WashAfterSale;
import org.apache.ibatis.annotations.Insert;

/** 售后单读写。 */
public interface WashAfterSaleMapper {

    @Insert("insert into wash_after_sale (after_sale_no, order_no, member_id, type, reason, status, amount, del_flag, create_time) " +
            "values (#{afterSaleNo}, #{orderNo}, #{memberId}, #{type}, #{reason}, #{status}, #{amount}, '0', sysdate())")
    int insertAfterSale(WashAfterSale afterSale);
}
