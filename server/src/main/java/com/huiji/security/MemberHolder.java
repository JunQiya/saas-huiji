package com.huiji.security;

/**
 * H5 当前会员上下文(ThreadLocal)。
 */
public final class MemberHolder {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private MemberHolder() {
    }

    public static void set(Long memberId) {
        HOLDER.set(memberId);
    }

    public static Long get() {
        Long id = HOLDER.get();
        if (id == null) {
            throw new IllegalStateException("当前无登录会员上下文");
        }
        return id;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
