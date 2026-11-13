package com.nuaa.recycle.common.auth;

import com.nuaa.recycle.common.error.BizException;
import com.nuaa.recycle.common.error.ErrorCode;

public final class UserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Long id() {
        return get().id();
    }

    public static Role role() {
        return get().role();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
