package com.ruoyi.wash.pay.mapper;

import com.ruoyi.wash.pay.domain.WashWalletRecharge;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/** 充值单读写。状态 INIT→SUCCESS 走 markSuccess（条件更新保幂等）。 */
public interface WashWalletRechargeMapper {

    @Select("select recharge_id, recharge_no, member_id, rule_id, pay_amount, gift_amount, channel, status, " +
            "provider_trade_no, pay_expire_at, create_time, update_time " +
            "from wash_wallet_recharge where recharge_no = #{rechargeNo} limit 1")
    WashWalletRecharge selectByRechargeNo(@Param("rechargeNo") String rechargeNo);

    @Insert("insert into wash_wallet_recharge (recharge_no, member_id, rule_id, pay_amount, gift_amount, channel, status, pay_expire_at, create_time) " +
            "values (#{rechargeNo}, #{memberId}, #{ruleId}, #{payAmount}, #{giftAmount}, #{channel}, 'INIT', #{payExpireAt}, sysdate())")
    int insert(WashWalletRecharge recharge);

    /** 仅当仍为 INIT 时置 SUCCESS，返回受影响行数（0 表示已处理过，保证幂等） */
    @Update("update wash_wallet_recharge set status = 'SUCCESS', provider_trade_no = #{tradeNo}, update_time = sysdate() " +
            "where recharge_no = #{rechargeNo} and status = 'INIT'")
    int markSuccess(@Param("rechargeNo") String rechargeNo, @Param("tradeNo") String tradeNo);

    @Select({
        "<script>",
        "select recharge_id, recharge_no, member_id, rule_id, pay_amount, gift_amount, channel, status, ",
        "provider_trade_no, pay_expire_at, create_time, update_time",
        "from wash_wallet_recharge",
        "<where>",
        "  <if test='hasMember'>member_id = #{memberId}</if>",
        "  <if test='hasStatus'>and status = #{status}</if>",
        "</where>",
        "order by create_time desc, recharge_id desc",
        "limit #{offset}, #{limit}",
        "</script>"
    })
    List<WashWalletRecharge> selectPage(Map<String, Object> params);

    @Select({
        "<script>",
        "select count(*) from wash_wallet_recharge",
        "<where>",
        "  <if test='hasMember'>member_id = #{memberId}</if>",
        "  <if test='hasStatus'>and status = #{status}</if>",
        "</where>",
        "</script>"
    })
    long selectCount(Map<String, Object> params);
}
