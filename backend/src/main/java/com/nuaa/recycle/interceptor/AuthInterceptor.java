package com.nuaa.recycle.interceptor;

import com.nuaa.recycle.common.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (path.equals("/api/health") ||
            path.startsWith("/api/auth/users/register") ||
            path.startsWith("/api/auth/users/login") ||
            path.startsWith("/api/auth/recyclers/login") ||
            path.startsWith("/api/auth/recyclers/register") ||
            path.startsWith("/api/auth/admins/login")) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new UnauthorizedException("登录已过期，请重新登录");
        }

        String token = header.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new UnauthorizedException("登录已过期，请重新登录");
        }

        LoginUser lu = new LoginUser();
        lu.setUserId(jwtUtil.getUserId(token));
        lu.setRole(jwtUtil.getRole(token));
        lu.setTokenVersion(jwtUtil.getVersion(token));
        lu.setToken(token);
        UserContext.set(lu);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
