package com.ruoyi.wash.pay.mapper;

import com.ruoyi.wash.pay.domain.WashMemberWallet;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 会员钱包读写。扣款在事务内走 selectByMemberIdForUpdate 行锁，避免并发超扣。 */
public interface WashMemberWalletMapper {

    @Select("select wallet_id, member_id, balance, gift_balance, total_recharge, version, create_time, update_time " +
            "from wash_member_wallet where member_id = #{memberId} limit 1")
    WashMemberWallet selectByMemberId(@Param("memberId") Long memberId);

    @Select("select wallet_id, member_id, balance, gift_balance, total_recharge, version, create_time, update_time " +
            "from wash_member_wallet where member_id = #{memberId} limit 1 for update")
    WashMemberWallet selectByMemberIdForUpdate(@Param("memberId") Long memberId);

    @Insert("insert into wash_member_wallet (member_id, balance, gift_balance, total_recharge, version, create_time) " +
            "values (#{memberId}, 0, 0, 0, 0, sysdate())")
    int insertWallet(@Param("memberId") Long memberId);

    @Update("update wash_member_wallet set balance = #{balance}, gift_balance = #{giftBalance}, " +
            "total_recharge = #{totalRecharge}, version = version + 1, update_time = sysdate() " +
            "where member_id = #{memberId}")
    int updateBalance(WashMemberWallet wallet);
}
