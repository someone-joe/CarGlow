package com.ruoyi.wash.member.service;

import com.ruoyi.wash.member.domain.WashMember;
import com.ruoyi.wash.member.mapper.WashMemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/** 会员只读查询（跨模块用，如消息推送解析 openid），避免各处直接依赖 Mapper。 */
@Service
public class WashMemberQueryService {

    private static final SecureRandom RANDOM = new SecureRandom();

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

    /**
     * 取会员邀请码；没有则生成一个并落库（首次访问时惰性生成）。
     * 生成用「CG + 6 位随机大写」，冲突时重试；DB 唯一索引兜底。
     */
    public String ensureInviteCode(Long memberId) {
        WashMember m = memberMapper.selectById(memberId);
        if (m != null && m.getInviteCode() != null && !m.getInviteCode().isBlank()) {
            return m.getInviteCode();
        }
        for (int i = 0; i < 5; i++) {
            String code = generateCode();
            if (findMemberIdByInviteCode(code) != null) {
                continue;
            }
            if (memberMapper.initInviteCode(memberId, code) > 0) {
                return code;
            }
            // 并发下已被写入，回读
            WashMember again = memberMapper.selectById(memberId);
            if (again != null && again.getInviteCode() != null && !again.getInviteCode().isBlank()) {
                return again.getInviteCode();
            }
        }
        throw new IllegalStateException("邀请码生成失败");
    }

    /** 按邀请码反查会员ID；不存在返回 null */
    public Long findMemberIdByInviteCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return memberMapper.selectMemberIdByInviteCode(code.trim().toUpperCase());
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder("CG");
        for (int i = 0; i < 6; i++) {
            sb.append((char) ('A' + RANDOM.nextInt(26)));
        }
        return sb.toString();
    }
}
