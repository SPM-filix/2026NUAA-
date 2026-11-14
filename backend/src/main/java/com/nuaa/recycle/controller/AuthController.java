package com.nuaa.recycle.controller;

import com.nuaa.recycle.common.*;
import com.nuaa.recycle.entity.AdminUser;
import com.nuaa.recycle.entity.Recycler;
import com.nuaa.recycle.entity.User;
import com.nuaa.recycle.mapper.AdminUserMapper;
import com.nuaa.recycle.mapper.RecyclerMapper;
import com.nuaa.recycle.mapper.UserMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Validated
@RestController
public class AuthController {

    @Autowired private UserMapper userMapper;
    @Autowired private RecyclerMapper recyclerMapper;
    @Autowired private AdminUserMapper adminUserMapper;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/api/auth/users/register")
    public R<Map<String, Object>> userRegister(@Valid @RequestBody UserRegisterReq req) {
        User u = new User();
        u.setPhone(req.phone);
        u.setPasswordHash(BCrypt.hashpw(req.password, BCrypt.gensalt()));
        u.setNickname(req.nickname);
        u.setAddress(req.address);
        u.setUserCode(generateUserCode());
        u.setPointsBalance(0);
        u.setTokenVersion(0);
        u.setMustChangePassword(false);
        u.setStatus("ACTIVE");
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(u);
        String token = jwtUtil.generateToken(u.getId(), "USER", 0);
        return R.ok(Map.of("userId", u.getId(), "userCode", u.getUserCode(), "token", token, "tokenType", "Bearer", "expiresIn", 7200));
    }

    @PostMapping("/api/auth/users/login")
    public R<Map<String, Object>> userLogin(@Valid @RequestBody LoginReq req) {
        User u = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>().eq(User::getPhone, req.phone));
        if (u == null || !BCrypt.checkpw(req.password, u.getPasswordHash())) {
            throw new BizException(2001, "账号或密码错误");
        }
        if ("DISABLED".equals(u.getStatus())) throw new BizException(2002, "账号已被禁用");
        String token = jwtUtil.generateToken(u.getId(), "USER", u.getTokenVersion());
        Map<String, Object> userMap = Map.of("userId", u.getId(), "nickname", u.getNickname(), "avatarUrl", u.getAvatarUrl(), "pointsBalance", u.getPointsBalance());
        return R.ok(Map.of("token", token, "tokenType", "Bearer", "expiresIn", 7200, "mustChangePassword", u.getMustChangePassword(), "user", userMap));
    }

    @PostMapping("/api/auth/recyclers/login")
    public R<Map<String, Object>> recyclerLogin(@Valid @RequestBody LoginReq req) {
        Recycler r = recyclerMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Recycler>().eq(Recycler::getPhone, req.phone));
        if (r == null || !BCrypt.checkpw(req.password, r.getPasswordHash())) {
            throw new BizException(2001, "账号或密码错误");
        }
        if ("DISABLED".equals(r.getStatus())) throw new BizException(2002, "账号已被禁用");
        if ("PENDING".equals(r.getStatus()) || "REJECTED".equals(r.getStatus())) throw new BizException(2004, "回收员申请未通过审核");
        String token = jwtUtil.generateToken(r.getId(), "RECYCLER", r.getTokenVersion());
        return R.ok(Map.of("token", token, "tokenType", "Bearer", "expiresIn", 7200, "mustChangePassword", r.getMustChangePassword(),
                "recycler", Map.of("recyclerId", r.getId(), "name", r.getName(), "siteId", r.getSiteId(), "status", r.getStatus())));
    }

    @PostMapping("/api/auth/admins/login")
    public R<Map<String, Object>> adminLogin(@Valid @RequestBody AdminLoginReq req) {
        AdminUser a = adminUserMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, req.username));
        if (a == null || !BCrypt.checkpw(req.password, a.getPasswordHash())) {
            throw new BizException(2001, "账号或密码错误");
        }
        if ("DISABLED".equals(a.getStatus())) throw new BizException(2002, "账号已被禁用");
        String token = jwtUtil.generateToken(a.getId(), "ADMIN", a.getTokenVersion());
        return R.ok(Map.of("token", token, "tokenType", "Bearer", "expiresIn", 7200, "mustChangePassword", false,
                "admin", Map.of("adminId", a.getId(), "username", a.getUsername())));
    }

    @PostMapping("/api/auth/logout")
    public R<Void> logout() {
        String role = UserContext.getRole();
        Long uid = UserContext.getUserId();
        if ("USER".equals(role)) userMapper.incrementTokenVersion(uid);
        else if ("RECYCLER".equals(role)) recyclerMapper.incrementTokenVersion(uid);
        else if ("ADMIN".equals(role)) adminUserMapper.incrementTokenVersion(uid);
        return R.ok();
    }

    @PutMapping("/api/auth/password")
    public R<Void> changePassword(@Valid @RequestBody ChangePwdReq req) {
        String role = UserContext.getRole();
        Long uid = UserContext.getUserId();
        if ("USER".equals(role)) {
            User u = userMapper.selectById(uid);
            if (!BCrypt.checkpw(req.oldPassword, u.getPasswordHash())) throw new BizException(2005, "原密码错误");
            u.setPasswordHash(BCrypt.hashpw(req.newPassword, BCrypt.gensalt()));
            u.setMustChangePassword(false);
            u.setTokenVersion(u.getTokenVersion() + 1);
            userMapper.updateById(u);
        } else if ("RECYCLER".equals(role)) {
            Recycler r = recyclerMapper.selectById(uid);
            if (!BCrypt.checkpw(req.oldPassword, r.getPasswordHash())) throw new BizException(2005, "原密码错误");
            r.setPasswordHash(BCrypt.hashpw(req.newPassword, BCrypt.gensalt()));
            r.setMustChangePassword(false);
            r.setTokenVersion(r.getTokenVersion() + 1);
            recyclerMapper.updateById(r);
        } else if ("ADMIN".equals(role)) {
            AdminUser a = adminUserMapper.selectById(uid);
            if (!BCrypt.checkpw(req.oldPassword, a.getPasswordHash())) throw new BizException(2005, "原密码错误");
            a.setPasswordHash(BCrypt.hashpw(req.newPassword, BCrypt.gensalt()));
            a.setTokenVersion(a.getTokenVersion() + 1);
            adminUserMapper.updateById(a);
        }
        return R.ok();
    }

    @PostMapping("/api/auth/recyclers/register")
    public R<Map<String, Object>> recyclerRegister(@Valid @RequestBody RecyclerRegisterReq req) {
        Recycler r = new Recycler();
        r.setName(req.name);
        r.setPhone(req.phone);
        r.setPasswordHash(BCrypt.hashpw(req.password, BCrypt.gensalt()));
        r.setEmployeeNo(req.employeeNo);
        r.setSiteId(req.siteId);
        r.setApplyReason(req.reason);
        r.setStatus("PENDING");
        r.setTokenVersion(0);
        r.setMustChangePassword(false);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        recyclerMapper.insert(r);
        return R.ok(Map.of("recyclerId", r.getId(), "status", "PENDING"));
    }

    private String generateUserCode() {
        Random rand = new Random();
        return String.format("%06d", rand.nextInt(1000000));
    }

    public record UserRegisterReq(@NotBlank String phone, @NotBlank String password, @NotBlank String nickname, String address) {}
    public record LoginReq(@NotBlank String phone, @NotBlank String password) {}
    public record AdminLoginReq(@NotBlank String username, @NotBlank String password) {}
    public record ChangePwdReq(@NotBlank String oldPassword, @NotBlank String newPassword) {}
    public record RecyclerRegisterReq(@NotBlank String name, @NotBlank String phone, @NotBlank String password, @NotBlank String employeeNo, Long siteId, String reason) {}
}
