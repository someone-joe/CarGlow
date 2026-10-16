package com.ruoyi.wash.member.service;

import com.ruoyi.wash.member.domain.WashMember;
import com.ruoyi.wash.member.mapper.WashMemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 会员只读查询（跨模块用，如消息推送解析 openid），避免各处直接依赖 Mapper。 */
@Service
public class WashMemberQueryService {

    @Autowired
    private WashMemberMapper memberMapper;

    /** 取会员 openid；未找到返回 null（调用方决定是否跳过推送） */
    public String findOpenid(Long memberId) {
        if (memberId == null) {
            return null;
        }
        WashMember m = memberMapper.selectById(memberId);
        return m == null ? null : m.getOpenid();
    }
}
