package com.ruoyi.wash.member.mapper;

import com.ruoyi.wash.member.domain.WashMember;
import org.apache.ibatis.annotations.Param;

/**
 * 会员表读写。禁止其他模块直接注入本 Mapper，跨域一律走 WashMemberService。
 */
public interface WashMemberMapper {

    WashMember selectByOpenid(@Param("openid") String openid);

    int insertWashMember(WashMember member);
}
