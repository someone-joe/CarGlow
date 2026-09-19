package com.ruoyi.wash.promo.mapper;

import com.ruoyi.wash.promo.domain.WashInsurancePolicy;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 保单读写。 */
public interface WashInsurancePolicyMapper {

    @Insert("insert into wash_insurance_policy(policy_no, product_id, member_id, vehicle_id, order_no, " +
            "status, start_time, end_time, paid_amount, create_time) " +
            "values(#{policyNo}, #{productId}, #{memberId}, #{vehicleId}, #{orderNo}, " +
            "#{status}, #{startTime}, #{endTime}, #{paidAmount}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "policyId")
    int insert(WashInsurancePolicy policy);

    @Select("select policy_id, policy_no, product_id, member_id, vehicle_id, order_no, status, " +
            "start_time, end_time, paid_amount " +
            "from wash_insurance_policy where member_id = #{memberId} and del_flag = '0' " +
            "order by create_time desc")
    List<WashInsurancePolicy> selectMy(@Param("memberId") long memberId);

    @Select("select policy_id, policy_no, product_id, member_id, vehicle_id, order_no, status, " +
            "start_time, end_time, paid_amount " +
            "from wash_insurance_policy where policy_no = #{policyNo} and del_flag = '0' limit 1")
    WashInsurancePolicy selectByPolicyNo(@Param("policyNo") String policyNo);
}
