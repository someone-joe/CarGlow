package com.ruoyi.wash.network.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 开箱码：生成 / 校验 / 清除（一物一码、一次有效）。
 *
 * <p>客户与师傅的码用不同前缀：同一订单任一时刻只有一个环节需要开箱，分前缀是为让留痕能区分
 * 「谁开的箱」，也避免师傅取码顶掉客户正在用的码。
 *
 * <p>契约：openapi.yaml /api/v1/orders/{orderNo}/open-code（客户）、
 * /api/v1/pick/orders/{orderNo}/open-code（师傅）。
 */
@Service
public class WashSlotCodeService {

    /** 客户存 / 取钥匙的开箱码（wash-order 在用） */
    public static final String CUSTOMER_PREFIX = "slot:code:";

    /** 师傅取钥匙的开箱码（wash-worker 在用） */
    public static final String WORKER_PREFIX = "worker:slot:code:";

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 开箱码有效期（分钟）：阈值走配置不硬编码 */
    @Value("${wash.slot.open-code-ttl-minutes:10}")
    private long ttlMinutes;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public String issue(String prefix, String orderNo) {
        String code = String.valueOf(100000 + RANDOM.nextInt(900000));
        stringRedisTemplate.opsForValue().set(prefix + orderNo, code, Duration.ofMinutes(ttlMinutes));
        return code;
    }

    /** 剩余有效毫秒；不存在或已过期返回 0 */
    public long expireMillis(String prefix, String orderNo) {
        Long sec = stringRedisTemplate.getExpire(prefix + orderNo);
        return sec != null && sec > 0 ? sec * 1000 : 0;
    }

    /** 校验失败直接抛 B2004；通过不清除（清除时机由调用方决定） */
    public void verify(String prefix, String orderNo, String code) {
        String expected = stringRedisTemplate.opsForValue().get(prefix + orderNo);
        if (expected == null || code == null || !expected.equals(code)) {
            throw new ApiException(ErrorCode.B2004);
        }
    }

    public void clear(String prefix, String orderNo) {
        stringRedisTemplate.delete(prefix + orderNo);
    }
}
