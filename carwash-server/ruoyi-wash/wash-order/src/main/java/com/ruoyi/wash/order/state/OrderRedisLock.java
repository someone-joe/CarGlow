package com.ruoyi.wash.order.state;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.common.statemachine.OrderStateMachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 状态机用的分布式锁：Redis SET NX PX，粒度为订单号。
 *
 * <p>key 规范遵循技术方案 5.3：lock:order:{orderNo}，TTL 10 秒，防止并发双流转。
 * 解锁只删自己加的锁（校验 value），避免误删别人的锁。
 */
@Component
public class OrderRedisLock implements OrderStateMachine.Lock {

    private static final String LOCK_PREFIX = "lock:order:";
    private static final Duration TTL = Duration.ofSeconds(10);

    private final ThreadLocal<String> held = new ThreadLocal<>();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void lock(String orderNo) {
        String key = LOCK_PREFIX + orderNo;
        String value = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, value, TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            // 拿不到锁就快速失败，让调用方重试，避免状态流转排队积压
            throw new ApiException(ErrorCode.A0005, "订单正在处理中，请稍后重试");
        }
        held.set(value);
    }

    @Override
    public void unlock(String orderNo) {
        try {
            String key = LOCK_PREFIX + orderNo;
            String value = held.get();
            if (value != null && value.equals(stringRedisTemplate.opsForValue().get(key))) {
                stringRedisTemplate.delete(key);
            }
        } finally {
            held.remove();
        }
    }
}
