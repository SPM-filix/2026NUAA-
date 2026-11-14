package com.nuaa.recycle.controller;

import com.nuaa.recycle.common.*;
import com.nuaa.recycle.entity.Announcement;
import com.nuaa.recycle.service.AnnouncementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @GetMapping("/api/announcements")
    public R<PageResult<Announcement>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type) {
        return R.ok(announcementService.list(page, size, type));
    }

    @GetMapping("/api/announcements/{announcementId}")
    public R<Announcement> detail(@PathVariable Long announcementId) {
        return R.ok(announcementService.detail(announcementId));
    }

    @PostMapping("/api/admin/announcements")
    public R<Map<String, Long>> create(@Valid @RequestBody Announcement dto) {
        checkAdmin();
        Long id = announcementService.create(dto);
        return R.ok(Map.of("announcementId", id));
    }

    @PutMapping("/api/admin/announcements/{announcementId}")
    public R<Announcement> update(@PathVariable Long announcementId, @RequestBody Announcement dto) {
        checkAdmin();
        return R.ok(announcementService.update(announcementId, dto));
    }

    @PatchMapping("/api/admin/announcements/{announcementId}/status")
    public R<Announcement> updateStatus(@PathVariable Long announcementId, @Valid @RequestBody StatusReq req) {
        checkAdmin();
        return R.ok(announcementService.updateStatus(announcementId, req.status));
    }

    @DeleteMapping("/api/admin/announcements/{announcementId}")
    public R<Void> delete(@PathVariable Long announcementId) {
        checkAdmin();
        announcementService.delete(announcementId);
        return R.ok();
    }

    @GetMapping("/api/admin/announcements")
    public R<PageResult<Announcement>> adminList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        checkAdmin();
        return R.ok(announcementService.adminList(page, size, type, status, keyword));
    }

    private void checkAdmin() {
        if (!"ADMIN".equals(UserContext.getRole())) throw new ForbiddenException("当前角色无权访问该接口");
    }

    public record StatusReq(@NotBlank String status) {}
}
