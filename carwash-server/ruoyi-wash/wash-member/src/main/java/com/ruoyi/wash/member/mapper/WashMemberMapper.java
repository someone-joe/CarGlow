package com.ruoyi.wash.member.mapper;

import com.ruoyi.wash.member.domain.WashMember;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 会员表读写。禁止其他模块直接注入本 Mapper，跨域一律走 WashMemberService / WashMemberAdminService。
 */
public interface WashMemberMapper {

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
