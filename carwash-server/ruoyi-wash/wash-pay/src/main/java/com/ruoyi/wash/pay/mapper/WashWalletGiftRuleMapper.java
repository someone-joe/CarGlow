package com.ruoyi.wash.pay.mapper;

import com.ruoyi.wash.pay.domain.WashWalletGiftRule;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 充值赠送规则。后台「钱包配置」整体覆盖：deleteAll + 逐条 insert（充值单已存金额快照，删规则不影响历史）。 */
public interface WashWalletGiftRuleMapper {

    @Select("select rule_id, recharge_fen, gift_fen, enabled, sort_order, create_time, update_time " +
            "from wash_wallet_gift_rule where enabled = 'Y' order by sort_order asc, recharge_fen asc")
    List<WashWalletGiftRule> selectActive();

    @Select("select rule_id, recharge_fen, gift_fen, enabled, sort_order, create_time, update_time " +
            "from wash_wallet_gift_rule order by sort_order asc, recharge_fen asc")
    List<WashWalletGiftRule> selectAll();

    @Select("select rule_id, recharge_fen, gift_fen, enabled, sort_order, create_time, update_time " +
            "from wash_wallet_gift_rule where rule_id = #{ruleId} limit 1")
    WashWalletGiftRule selectById(@Param("ruleId") Long ruleId);

    @Delete("delete from wash_wallet_gift_rule")
    int deleteAll();

    @Insert("insert into wash_wallet_gift_rule (recharge_fen, gift_fen, enabled, sort_order, create_time) " +
            "values (#{rechargeFen}, #{giftFen}, #{enabled}, #{sortOrder}, sysdate())")
    int insert(WashWalletGiftRule rule);
}
