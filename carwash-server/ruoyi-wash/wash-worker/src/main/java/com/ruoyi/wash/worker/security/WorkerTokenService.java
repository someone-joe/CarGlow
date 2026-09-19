package com.ruoyi.wash.worker.security;

import com.ruoyi.common.core.redis.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 师傅端 token：UUID 存 Redis，不透明令牌（与 C 端 MemberTokenService 同思路，可主动吊销）。
 * key 规范：worker:token:{token}。与 C 端 wx:token: 前缀隔离，两套登录态互不干扰。
 */
@Component
public class WorkerTokenService {

    public static final String TOKEN_PREFIX = "worker:token:";
    public static final String HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private RedisCache redisCache;

    @Value("${wash.worker.token.ttl-days:30}")
    private int ttlDays;

    /** 登录成功后签发 token。 */
    public String createToken(Long workerId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisCache.setCacheObject(TOKEN_PREFIX + token, workerId, ttlDays, TimeUnit.DAYS);
        return token;
    }

    /** 校验并返回 workerId；无效返回 null。命中后顺延 TTL（滑动过期）。 */
    public Long getWorkerId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Long workerId = redisCache.getCacheObject(TOKEN_PREFIX + token);
        if (workerId != null) {
            redisCache.expire(TOKEN_PREFIX + token, ttlDays, TimeUnit.DAYS);
        }
        return workerId;
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
