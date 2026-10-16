package com.ruoyi.wash.promo.mapper;

import com.ruoyi.wash.promo.domain.WashInvite;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 邀请关系读写。状态流转用条件更新（where 带 BOUND）保证不重复发奖。 */
public interface WashInviteMapper {

    @Insert("insert into wash_invite (inviter_member_id, invitee_member_id, status, del_flag, create_time) " +
            "values (#{inviterMemberId}, #{inviteeMemberId}, #{status}, '0', sysdate())")
    @Options(useGeneratedKeys = true, keyProperty = "inviteId")
    int insert(WashInvite invite);

    /** 该会员作为「被邀请人」的关系（唯一） */
    @Select("select invite_id, inviter_member_id, invitee_member_id, first_order_no, reward_coupon_user_id, status, create_time, update_time " +
            "from wash_invite where invitee_member_id = #{memberId} and del_flag = '0' limit 1")
    WashInvite selectByInvitee(@Param("memberId") Long memberId);

    @Select("select invite_id, inviter_member_id, invitee_member_id, first_order_no, reward_coupon_user_id, status, create_time, update_time " +
            "from wash_invite where inviter_member_id = #{memberId} and del_flag = '0' order by invite_id desc")
    List<WashInvite> selectByInviter(@Param("memberId") Long memberId);

    /** 首单发奖：BOUND → REWARDED（条件更新，幂等） */
    @Update("update wash_invite set status = 'REWARDED', first_order_no = #{orderNo}, reward_coupon_user_id = #{couponUserId}, update_time = sysdate() " +
            "where invite_id = #{inviteId} and status = 'BOUND' and del_flag = '0'")
    int markRewarded(@Param("inviteId") Long inviteId, @Param("orderNo") String orderNo, @Param("couponUserId") Long couponUserId);

    @Select("select count(*) from wash_invite where inviter_member_id = #{memberId} and status = 'REWARDED' and del_flag = '0'")
    long countRewarded(@Param("memberId") Long memberId);
}
