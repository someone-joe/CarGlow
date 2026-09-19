package com.ruoyi.wash.promo.mapper;

import com.ruoyi.wash.promo.domain.WashInsuranceProduct;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 保险产品读写。 */
public interface WashInsuranceProductMapper {

    @Select("select insurance_product_id, name, price_amount, coverage_desc, status " +
            "from wash_insurance_product where status = 'Y' and del_flag = '0' order by insurance_product_id asc")
    List<WashInsuranceProduct> selectEnabled();

    @Select("select insurance_product_id, name, price_amount, coverage_desc, status " +
            "from wash_insurance_product where insurance_product_id = #{id} and del_flag = '0'")
    WashInsuranceProduct selectById(@Param("id") long id);

    @Insert("insert into wash_insurance_product(name, price_amount, coverage_desc, status, create_time) " +
            "values(#{name}, #{priceAmount}, #{coverageDesc}, #{status}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "insuranceProductId")
    int insert(WashInsuranceProduct product);
}
