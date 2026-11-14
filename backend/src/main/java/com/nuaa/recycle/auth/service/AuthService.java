package com.nuaa.recycle.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nuaa.recycle.auth.dto.AdminLoginRequest;
import com.nuaa.recycle.auth.dto.AdminMeResponse;
import com.nuaa.recycle.auth.dto.ChangePasswordRequest;
import com.nuaa.recycle.auth.dto.LoginRequest;
import com.nuaa.recycle.auth.dto.LoginResponse;
import com.nuaa.recycle.auth.dto.RecyclerMeResponse;
import com.nuaa.recycle.auth.dto.RecyclerRegisterRequest;
import com.nuaa.recycle.auth.dto.RecyclerRegisterResponse;
import com.nuaa.recycle.auth.dto.RegisterRequest;
import com.nuaa.recycle.auth.entity.AdminUser;
import com.nuaa.recycle.auth.entity.Recycler;
import com.nuaa.recycle.auth.entity.User;
import com.nuaa.recycle.auth.mapper.AdminUserMapper;
import com.nuaa.recycle.auth.mapper.RecyclerMapper;
import com.nuaa.recycle.auth.mapper.UserMapper;
import com.nuaa.recycle.catalog.entity.Site;
import com.nuaa.recycle.catalog.mapper.SiteMapper;
import com.nuaa.recycle.common.auth.JwtService;
import com.nuaa.recycle.common.auth.Role;
import com.nuaa.recycle.common.auth.UserContext;
import com.nuaa.recycle.common.error.BizException;
import com.nuaa.recycle.common.error.ErrorCode;
import com.nuaa.recycle.common.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final RecyclerMapper recyclerMapper;
    private final AdminUserMapper adminUserMapper;
    private final SiteMapper siteMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CodeGenerator codeGenerator;

    @Transactional
    public LoginResponse registerUser(RegisterRequest request) {
        if (countUserByPhone(request.phone()) > 0) {
            throw new BizException(ErrorCode.PHONE_EXISTS);
        }
        User user = new User();
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setAddress(request.address());
        user.setPointsBalance(0);
        user.setTokenVersion(0);
        user.setMustChangePassword(false);
        user.setStatus("ACTIVE");
        user.setUserCode(nextUserCode());
        userMapper.insert(user);
        return userLoginResponse(user);
    }

    public LoginResponse loginUser(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, request.phone()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        return userLoginResponse(user);
    }

    public LoginResponse loginRecycler(LoginRequest request) {
        Recycler recycler = recyclerMapper.selectOne(
                new LambdaQueryWrapper<Recycler>().eq(Recycler::getPhone, request.phone()));
        if (recycler == null || !passwordEncoder.matches(request.password(), recycler.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if ("DISABLED".equals(recycler.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!"APPROVED".equals(recycler.getStatus())) {
            String reason = "REJECTED".equals(recycler.getStatus()) && StringUtils.hasText(recycler.getRejectReason())
                    ? "：" + recycler.getRejectReason() : "";
            throw new BizException(ErrorCode.RECYCLER_NOT_APPROVED,
                    "回收员申请未通过审核" + reason);
        }
        String siteName = null;
        if (recycler.getSiteId() != null) {
            Site site = siteMapper.selectById(recycler.getSiteId());
            siteName = site == null ? null : site.getName();
        }
        String token = jwtService.generate(recycler.getId(), Role.RECYCLER, recycler.getTokenVersion());
        return new LoginResponse(token, "Bearer", jwtService.expiresInSeconds(),
                Boolean.TRUE.equals(recycler.getMustChangePassword()), null,
                new LoginResponse.RecyclerInfo(recycler.getId(), recycler.getName(),
                        recycler.getSiteId(), siteName, recycler.getStatus()),
                null);
    }

    public LoginResponse loginAdmin(AdminLoginRequest request) {
        AdminUser admin = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, request.username()));
        if (admin == null || !passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (!"ACTIVE".equals(admin.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        String token = jwtService.generate(admin.getId(), Role.ADMIN, admin.getTokenVersion());
        return new LoginResponse(token, "Bearer", jwtService.expiresInSeconds(), false, null, null,
                new LoginResponse.AdminInfo(admin.getId(), admin.getUsername()));
    }

    @Transactional
    public RecyclerRegisterResponse registerRecycler(RecyclerRegisterRequest request) {
        if (countUserByPhone(request.phone()) > 0
                || recyclerMapper.selectCount(
                new LambdaQueryWrapper<Recycler>().eq(Recycler::getPhone, request.phone())) > 0) {
            throw new BizException(ErrorCode.PHONE_EXISTS);
        }
        Recycler recycler = new Recycler();
        recycler.setName(request.name());
        recycler.setPhone(request.phone());
        recycler.setPasswordHash(passwordEncoder.encode(request.password()));
        recycler.setEmployeeNo(request.employeeNo());
        recycler.setSiteId(request.siteId());
        recycler.setStatus("PENDING");
        recycler.setTokenVersion(0);
        recycler.setMustChangePassword(false);
        recycler.setApplyReason(request.reason());
        recyclerMapper.insert(recycler);
        return new RecyclerRegisterResponse(recycler.getId(), recycler.getStatus());
    }

    @Transactional
    public void logout() {
        switch (UserContext.role()) {
            case USER -> userMapper.bumpTokenVersion(UserContext.id());
            case RECYCLER -> recyclerMapper.bumpTokenVersion(UserContext.id());
            case ADMIN -> adminUserMapper.bumpTokenVersion(UserContext.id());
        }
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        switch (UserContext.role()) {
            case USER -> {
                User user = requireUser(UserContext.id());
                if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
                    throw new BizException(ErrorCode.OLD_PASSWORD_WRONG);
                }
                if (request.oldPassword().equals(request.newPassword())) {
                    throw new BizException(ErrorCode.NEW_PASSWORD_INVALID, "新密码不能与原密码相同");
                }
                userMapper.updatePassword(user.getId(), passwordEncoder.encode(request.newPassword()));
            }
            case RECYCLER -> {
                Recycler recycler = requireRecycler(UserContext.id());
                if (!passwordEncoder.matches(request.oldPassword(), recycler.getPasswordHash())) {
                    throw new BizException(ErrorCode.OLD_PASSWORD_WRONG);
                }
                if (request.oldPassword().equals(request.newPassword())) {
                    throw new BizException(ErrorCode.NEW_PASSWORD_INVALID, "新密码不能与原密码相同");
                }
                recyclerMapper.updatePassword(recycler.getId(), passwordEncoder.encode(request.newPassword()));
            }
            case ADMIN -> {
                AdminUser admin = requireAdmin(UserContext.id());
                if (!passwordEncoder.matches(request.oldPassword(), admin.getPasswordHash())) {
                    throw new BizException(ErrorCode.OLD_PASSWORD_WRONG);
                }
                if (request.oldPassword().equals(request.newPassword())) {
                    throw new BizException(ErrorCode.NEW_PASSWORD_INVALID, "新密码不能与原密码相同");
                }
                adminUserMapper.updatePassword(admin.getId(), passwordEncoder.encode(request.newPassword()));
            }
        }
    }

    public RecyclerMeResponse recyclerMe() {
        Recycler recycler = requireRecycler(UserContext.id());
        String siteName = null;
        if (recycler.getSiteId() != null) {
            Site site = siteMapper.selectById(recycler.getSiteId());
            siteName = site == null ? null : site.getName();
        }
        return new RecyclerMeResponse(recycler.getId(), recycler.getName(), recycler.getPhone(),
                recycler.getEmployeeNo(), recycler.getSiteId(), siteName, recycler.getStatus(),
                recycler.getCreatedAt());
    }

    public AdminMeResponse adminMe() {
        AdminUser admin = requireAdmin(UserContext.id());
        return new AdminMeResponse(admin.getId(), admin.getUsername(), admin.getCreatedAt());
    }

    public User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    public Recycler requireRecycler(Long id) {
        Recycler recycler = recyclerMapper.selectById(id);
        if (recycler == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "回收员不存在");
        }
        return recycler;
    }

    public AdminUser requireAdmin(Long id) {
        AdminUser admin = adminUserMapper.selectById(id);
        if (admin == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "管理员不存在");
        }
        return admin;
    }

    private LoginResponse userLoginResponse(User user) {
        String token = jwtService.generate(user.getId(), Role.USER, user.getTokenVersion());
        return new LoginResponse(token, "Bearer", jwtService.expiresInSeconds(),
                Boolean.TRUE.equals(user.getMustChangePassword()),
                new LoginResponse.UserInfo(user.getId(), user.getUserCode(), user.getNickname(),
                        user.getAvatarUrl(), user.getPointsBalance()), null, null);
    }

    private long countUserByPhone(String phone) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    private String nextUserCode() {
        for (int i = 0; i < 5; i++) {
            String code = codeGenerator.userCode();
            if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUserCode, code)) == 0) {
                return code;
            }
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "用户编号生成失败");
    }
}
