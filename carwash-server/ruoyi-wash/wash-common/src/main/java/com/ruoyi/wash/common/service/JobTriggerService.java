package com.ruoyi.wash.common.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 手动触发定时任务（运维 / 接口自动化用）。
 *
 * <p>仅白名单内的 job 可被触发，避免任意 bean 被反射调用造成越权执行。
 * 各 job 必须实现 {@code public void runNow(Map<String,Object> params)}，由本服务按 bean 名反射调用，
 * 从而不引入跨模块的编译期依赖（wash-common 不依赖各业务模块的 job 类）。
 *
 * <p>正常生产由 {@code @Scheduled} 周期驱动；此处提供手动入口，主要解锁接口测试的 P 段用例
 * （P03 顺延 / P04 T+3 自动完成 / P07 退款重试）。
 */
@Service
public class JobTriggerService {

    private static final Logger log = LoggerFactory.getLogger(JobTriggerService.class);

    private static final Set<String> ALLOWED = Set.of(
            "pay-timeout", "capacity-init", "refund-retry", "order-postpone", "order-auto-finish");

    private static final Map<String, String> BEAN_NAMES = Map.of(
            "pay-timeout", "orderPayTimeoutJob",
            "capacity-init", "capacityInitJob",
            "refund-retry", "refundRetryJob",
            "order-postpone", "orderPostponeJob",
            "order-auto-finish", "orderAutoFinishJob");

    @Autowired
    private ApplicationContext applicationContext;

    public Map<String, Object> trigger(String job, Map<String, Object> params) {
        if (job == null || !ALLOWED.contains(job)) {
            throw new ApiException(ErrorCode.A0001, "不支持的 job：" + job);
        }
        String beanName = BEAN_NAMES.get(job);
        Object bean = applicationContext.getBean(beanName);
        try {
            Method m = bean.getClass().getMethod("runNow", Map.class);
            m.invoke(bean, params == null ? new HashMap<>() : params);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("[手动触发任务] 执行失败 job={}", job, e);
            throw new ApiException(ErrorCode.A0001, "任务执行失败：" + e.getMessage());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("job", job);
        result.put("triggered", true);
        return result;
    }
}
