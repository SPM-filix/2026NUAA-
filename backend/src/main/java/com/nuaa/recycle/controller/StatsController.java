package com.nuaa.recycle.controller;

import com.nuaa.recycle.common.*;
import com.nuaa.recycle.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
public class StatsController {

    @Autowired
    private StatsService statsService;

    @GetMapping("/api/admin/stats/overview")
    public R<Map<String, Object>> overview() {
        checkAdmin();
        return R.ok(statsService.overview());
    }

    @GetMapping("/api/admin/stats/categories")
    public R<List<Map<String, Object>>> categories(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        checkAdmin();
        return R.ok(statsService.categories(from, to));
    }

    @GetMapping("/api/admin/stats/trend")
    public R<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "7") int days) {
        checkAdmin();
        if (days != 7 && days != 30) days = 7;
        return R.ok(statsService.trend(days));
    }

    @GetMapping("/api/admin/stats/top")
    public R<List<Map<String, Object>>> top(
            @RequestParam String type,
            @RequestParam(defaultValue = "5") int limit) {
        checkAdmin();
        return R.ok(statsService.top(type, limit));
    }

    @GetMapping("/api/recyclers/me/workbench")
    public R<Map<String, Object>> workbench() {
        String role = UserContext.getRole();
        if (!"RECYCLER".equals(role)) throw new ForbiddenException("当前角色无权访问该接口");
        return R.ok(statsService.workbench(UserContext.getUserId()));
    }

    private void checkAdmin() {
        if (!"ADMIN".equals(UserContext.getRole())) throw new ForbiddenException("当前角色无权访问该接口");
    }
}
