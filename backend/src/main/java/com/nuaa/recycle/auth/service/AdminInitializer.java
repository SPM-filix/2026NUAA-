package com.nuaa.recycle.auth.service;

import com.nuaa.recycle.auth.entity.AdminUser;
import com.nuaa.recycle.auth.mapper.AdminUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.init-password:}")
    private String initPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(initPassword)) {
            log.warn("ADMIN_INIT_PASSWORD 未设置，跳过初始管理员创建");
            return;
        }
        if (adminUserMapper.selectCount(null) > 0) {
            return;
        }
        AdminUser admin = new AdminUser();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode(initPassword));
        admin.setStatus("ACTIVE");
        admin.setTokenVersion(0);
        adminUserMapper.insert(admin);
        log.info("已创建初始管理员账号 admin");
    }
}
