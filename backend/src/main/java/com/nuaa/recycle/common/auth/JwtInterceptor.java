package com.nuaa.recycle.common.auth;

import com.nuaa.recycle.auth.entity.AdminUser;
import com.nuaa.recycle.auth.entity.Recycler;
import com.nuaa.recycle.auth.entity.User;
import com.nuaa.recycle.auth.mapper.AdminUserMapper;
import com.nuaa.recycle.auth.mapper.RecyclerMapper;
import com.nuaa.recycle.auth.mapper.UserMapper;
import com.nuaa.recycle.common.error.BizException;
import com.nuaa.recycle.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final List<String> WHITELIST = List.of(
            "/api/auth/users/register",
            "/api/auth/users/login",
            "/api/auth/recyclers/register",
            "/api/auth/recyclers/login",
            "/api/auth/admins/login",
            "/api/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error"
    );

    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final RecyclerMapper recyclerMapper;
    private final AdminUserMapper adminUserMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)
                || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (WHITELIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, path))) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        JwtService.JwtPayload payload = jwtService.parse(header.substring(7));
        if (!"ACCESS".equals(payload.type())) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        boolean mustChangePassword = false;
        switch (payload.role()) {
            case USER -> {
                User user = userMapper.selectById(payload.id());
                if (user == null) {
                    throw new BizException(ErrorCode.UNAUTHORIZED);
                }
                if (!"ACTIVE".equals(user.getStatus())) {
                    throw new BizException(ErrorCode.ACCOUNT_DISABLED);
                }
                checkVersion(payload, user.getTokenVersion());
                mustChangePassword = Boolean.TRUE.equals(user.getMustChangePassword());
            }
            case RECYCLER -> {
                Recycler recycler = recyclerMapper.selectById(payload.id());
                if (recycler == null) {
                    throw new BizException(ErrorCode.UNAUTHORIZED);
                }
                if (!"APPROVED".equals(recycler.getStatus())) {
                    throw new BizException(ErrorCode.ACCOUNT_DISABLED);
                }
                checkVersion(payload, recycler.getTokenVersion());
                mustChangePassword = Boolean.TRUE.equals(recycler.getMustChangePassword());
            }
            case ADMIN -> {
                AdminUser admin = adminUserMapper.selectById(payload.id());
                if (admin == null) {
                    throw new BizException(ErrorCode.UNAUTHORIZED);
                }
                if (!"ACTIVE".equals(admin.getStatus())) {
                    throw new BizException(ErrorCode.ACCOUNT_DISABLED);
                }
                checkVersion(payload, admin.getTokenVersion());
            }
        }

        if (mustChangePassword
                && !"/api/auth/password".equals(path)
                && !"/api/auth/logout".equals(path)) {
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "请先修改初始密码");
        }

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = org.springframework.core.annotation.AnnotatedElementUtils
                    .findMergedAnnotation(handlerMethod.getBeanType(), RequireRole.class);
        }
        if (requireRole != null
                && Arrays.stream(requireRole.value()).noneMatch(role -> role == payload.role())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        com.nuaa.recycle.common.LoginUser loginUser = new com.nuaa.recycle.common.LoginUser();
        loginUser.setUserId(payload.id());
        loginUser.setRole(payload.role().name());
        loginUser.setTokenVersion(payload.version());
        loginUser.setToken(header.substring(7));
        com.nuaa.recycle.common.UserContext.set(loginUser);

        UserContext.set(new CurrentUser(payload.id(), payload.role()));
        return true;
    }

    private void checkVersion(JwtService.JwtPayload payload, Integer currentVersion) {
        if (!Objects.equals(payload.version(), currentVersion)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
        com.nuaa.recycle.common.UserContext.clear();
    }
}
