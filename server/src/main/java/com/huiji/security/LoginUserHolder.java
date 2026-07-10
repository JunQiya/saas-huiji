package com.huiji.security;

/**
 * 当前登录用户上下文(ThreadLocal), 在 JwtAuthFilter 中注入, 业务中直接取用。
 */
public final class LoginUserHolder {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private LoginUserHolder() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            throw new IllegalStateException("当前无登录用户上下文, 请确认接口已鉴权");
        }
        return u;
    }

    /** 当前登录用户可能为空(如公开接口) */
    public static LoginUser getOrNull() {
        return HOLDER.get();
    }

    public static Long currentTenantId() {
        return get().getTenantId();
    }

    /** 当前用户 id(可能为 null, 用于 JwtAuthFilter 注入前的兜底) */
    public static Long currentUserId() {
        LoginUser u = get();
        return u == null ? null : u.getUserId();
    }

    /** 等价于 get(), 命名以更贴合业务 */
    public static LoginUser current() {
        return get();
    }

    /** 当前用户绑定的门店(收银员/店长从 token 取, 可能为 null) */
    public static Long requireStoreId() {
        LoginUser u = get();
        return u == null ? null : u.getStoreId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
