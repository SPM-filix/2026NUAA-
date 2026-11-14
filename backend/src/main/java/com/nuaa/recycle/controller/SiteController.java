package com.nuaa.recycle.controller;

import com.nuaa.recycle.common.*;
import com.nuaa.recycle.entity.Site;
import com.nuaa.recycle.service.SiteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
public class SiteController {

    @Autowired
    private SiteService siteService;

    @GetMapping("/api/sites")
    public R<List<Site>> list(@RequestParam(required = false, defaultValue = "true") boolean enabledOnly) {
        return R.ok(siteService.list(enabledOnly));
    }

    @PostMapping("/api/admin/sites")
    public R<java.util.Map<String, Long>> create(@Valid @RequestBody Site dto) {
        checkAdmin();
        Long id = siteService.create(dto);
        return R.ok(java.util.Map.of("siteId", id));
    }

    @PutMapping("/api/admin/sites/{siteId}")
    public R<Site> update(@PathVariable Long siteId, @RequestBody Site dto) {
        checkAdmin();
        return R.ok(siteService.update(siteId, dto));
    }

    @PatchMapping("/api/admin/sites/{siteId}/status")
    public R<Site> updateStatus(@PathVariable Long siteId, @Valid @RequestBody StatusReq req) {
        checkAdmin();
        return R.ok(siteService.updateStatus(siteId, req.status));
    }

    private void checkAdmin() {
        if (!"ADMIN".equals(UserContext.getRole())) throw new ForbiddenException("当前角色无权访问该接口");
    }

    public record StatusReq(@NotBlank String status) {}
}
