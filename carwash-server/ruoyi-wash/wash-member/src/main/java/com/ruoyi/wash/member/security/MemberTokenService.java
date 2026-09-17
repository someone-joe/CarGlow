package com.ruoyi.wash.member.security;

import com.ruoyi.common.core.redis.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * C 端会员 token：UUID 存 Redis，不透明令牌（简单、可主动吊销，V1.0 不上 JWT）。
 * key 规范遵循技术方案 5.3：wx:token:{token}。
 */
@Component
public class MemberTokenService {

    public static final String TOKEN_PREFIX = "wx:token:";
    public static final String HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private RedisCache redisCache;

    @Value("${wash.token.ttl-days:30}")
    private int ttlDays;

    /** 登录成功后签发 token。 */
    public String createToken(Long memberId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisCache.setCacheObject(TOKEN_PREFIX + token, memberId, ttlDays, TimeUnit.DAYS);
        return token;
    }

    /** 校验并返回 memberId；无效返回 null。命中后顺延 TTL（滑动过期）。 */
    public Long getMemberId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Long memberId = redisCache.getCacheObject(TOKEN_PREFIX + token);
        if (memberId != null) {
            redisCache.expire(TOKEN_PREFIX + token, ttlDays, TimeUnit.DAYS);
        }
        return memberId;
    }

    /** 从请求头解析 token（形如「Bearer xxx」）。 */
    public String resolveToken(String headerValue) {
        if (headerValue == null || !headerValue.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return headerValue.substring(BEARER_PREFIX.length());
    }

    public long getExpireAt() {
        return System.currentTimeMillis() + ttlDays * 24L * 3600 * 1000;
    }
}
