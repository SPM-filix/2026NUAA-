package com.nuaa.recycle.common;

public class UserContext {
    private static final ThreadLocal<LoginUser> TL = new ThreadLocal<>();

    public static void set(LoginUser user) {
        TL.set(user);
    }

    public static LoginUser get() {
        return TL.get();
    }

    public static void clear() {
        TL.remove();
    }

    public static Long getUserId() {
        LoginUser u = get();
        return u != null ? u.getUserId() : null;
    }

    public static String getRole() {
        LoginUser u = get();
        return u != null ? u.getRole() : null;
    }
}
