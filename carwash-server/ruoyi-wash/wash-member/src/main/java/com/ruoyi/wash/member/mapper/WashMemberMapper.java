package com.ruoyi.wash.member.mapper;

import com.ruoyi.wash.member.domain.WashMember;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 会员表读写。禁止其他模块直接注入本 Mapper，跨域一律走 WashMemberService / WashMemberAdminService。
 */
public interface WashMemberMapper {

    /** 按会员ID查（含 openid，供消息推送解析接收人；含 invite_code，供邀请好友） */
    @Select("select member_id, openid, unionid, nickname, phone, invite_code, create_time from wash_member " +
            "where member_id = #{memberId} and del_flag = '0' limit 1")
    WashMember selectById(@Param("memberId") Long memberId);

    /** 首次设置邀请码（幂等：已有则不覆盖） */
    @Update("update wash_member set invite_code = #{code}, update_time = sysdate() " +
            "where member_id = #{memberId} and (invite_code is null or invite_code = '') and del_flag = '0'")
    int initInviteCode(@Param("memberId") Long memberId, @Param("code") String code);

    @Select("select member_id from wash_member where invite_code = #{code} and del_flag = '0' limit 1")
    Long selectMemberIdByInviteCode(@Param("code") String code);

    WashMember selectByOpenid(@Param("openid") String openid);

    int insertWashMember(WashMember member);

    /** 后台会员分页：按昵称或手机号模糊查（手机号仅用于匹配，返回时脱敏） */
    @Select("<script>" +
            "select member_id, openid, unionid, nickname, phone, create_time from wash_member " +
            "where del_flag = '0' " +
            "<if test='keyword != null and keyword != \"\"'> " +
            "  and (nickname like concat('%', #{keyword}, '%') or phone like concat('%', #{keyword}, '%')) " +
            "</if> " +
            "order by create_time desc limit #{offset}, #{pageSize}" +
            "</script>")
    List<WashMember> selectAdminPage(@Param("keyword") String keyword,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    @Select("<script>" +
            "select count(*) from wash_member where del_flag = '0' " +
            "<if test='keyword != null and keyword != \"\"'> " +
            "  and (nickname like concat('%', #{keyword}, '%') or phone like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "</script>")
    long countAdmin(@Param("keyword") String keyword);
}
