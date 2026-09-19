package com.ruoyi.wash.common.security;

/**
 * 当前登录的师傅（工作人员）上下文（ThreadLocal）。
 *
 * <p>由 wash-worker 的 WorkerAuthInterceptor 在请求进入时写入，
 * 师傅端 Controller / Service 通过本类取 workerId，禁止自己再解析 token。
 * 请求结束必须 clear（拦截器 afterCompletion 负责），防止线程复用串号。
 */
public final class WorkerContext {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private WorkerContext() {
    }

    public static void set(Long workerId) {
        HOLDER.set(workerId);
    }

    /** 未登录时返回 null；仅在明确允许匿名的场景使用。 */
    public static Long get() {
        return HOLDER.get();
    }

    /** 师傅端接口内取当前师傅，取不到说明拦截器漏配，直接抛错暴露问题。 */
    public static Long require() {
        Long workerId = HOLDER.get();
        if (workerId == null) {
            throw new IllegalStateException("当前线程没有师傅上下文（拦截器未生效？）");
        }
        return workerId;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
