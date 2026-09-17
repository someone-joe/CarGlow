package com.ruoyi.wash.common.security;

/**
 * 当前登录的 C 端会员上下文（ThreadLocal）。
 *
 * <p>由 wash-member 的 MemberAuthInterceptor 在请求进入时写入，
 * 业务 Controller / Service 通过本类取 memberId，禁止自己再解析 token。
 * 请求结束必须 clear（拦截器 afterCompletion 负责），防止线程复用串号。
 */
public final class MemberContext {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private MemberContext() {
    }

    public static void set(Long memberId) {
        HOLDER.set(memberId);
    }

    /** 未登录时返回 null；仅在明确允许匿名的场景使用。 */
    public static Long get() {
        return HOLDER.get();
    }

    /** 业务接口内取当前会员，取不到说明拦截器漏配，直接抛错暴露问题。 */
    public static Long require() {
        Long memberId = HOLDER.get();
        if (memberId == null) {
            throw new IllegalStateException("当前线程没有会员上下文（拦截器未生效？）");
        }
        return memberId;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
