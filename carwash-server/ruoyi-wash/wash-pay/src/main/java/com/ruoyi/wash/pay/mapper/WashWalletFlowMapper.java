package com.ruoyi.wash.pay.mapper;

import com.ruoyi.wash.pay.domain.WashWalletFlow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 钱包流水。只增不删。 */
public interface WashWalletFlowMapper {

    @Insert("insert into wash_wallet_flow (flow_no, member_id, biz_type, change_amount, gift_change, " +
            "balance_after, gift_after, biz_no, remark, create_time) " +
            "values (#{flowNo}, #{memberId}, #{bizType}, #{changeAmount}, #{giftChange}, " +
            "#{balanceAfter}, #{giftAfter}, #{bizNo}, #{remark}, sysdate())")
    int insert(WashWalletFlow flow);

    @Select("select flow_id, flow_no, member_id, biz_type, change_amount, gift_change, balance_after, gift_after, biz_no, remark, create_time " +
            "from wash_wallet_flow where member_id = #{memberId} order by flow_id desc limit #{offset}, #{limit}")
    List<WashWalletFlow> selectByMember(@Param("memberId") Long memberId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("select count(*) from wash_wallet_flow where member_id = #{memberId}")
    long countByMember(@Param("memberId") Long memberId);
}
