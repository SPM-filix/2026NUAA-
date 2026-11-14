package com.nuaa.recycle.controller;

import com.nuaa.recycle.common.*;
import com.nuaa.recycle.entity.Appeal;
import com.nuaa.recycle.entity.Recycler;
import com.nuaa.recycle.entity.User;
import com.nuaa.recycle.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/api/admin/accounts/users")
    public R<PageResult<User>> userList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        checkAdmin();
        return R.ok(accountService.userList(page, size, keyword, status));
    }

    @PatchMapping("/api/admin/accounts/users/{userId}/status")
    public R<User> updateUserStatus(@PathVariable Long userId, @Valid @RequestBody UserStatusReq req) {
        checkAdmin();
        return R.ok(accountService.updateUserStatus(userId, req.status, req.reason));
    }

    @PostMapping("/api/admin/accounts/users/{userId}/password/reset")
    public R<Map<String, Object>> resetUserPassword(@PathVariable Long userId) {
        checkAdmin();
        return R.ok(accountService.resetUserPassword(userId));
    }

    @GetMapping("/api/admin/accounts/recyclers")
    public R<PageResult<Recycler>> recyclerList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        checkAdmin();
        return R.ok(accountService.recyclerList(page, size, status, keyword));
    }

    @PostMapping("/api/admin/accounts/recyclers/{recyclerId}/approve")
    public R<Recycler> approveRecycler(@PathVariable Long recyclerId, @Valid @RequestBody ApproveReq req) {
        checkAdmin();
        return R.ok(accountService.approveRecycler(recyclerId, req.siteId));
    }

    @PostMapping("/api/admin/accounts/recyclers/{recyclerId}/reject")
    public R<Recycler> rejectRecycler(@PathVariable Long recyclerId, @Valid @RequestBody RejectReq req) {
        checkAdmin();
        return R.ok(accountService.rejectRecycler(recyclerId, req.reason));
    }

    @PatchMapping("/api/admin/accounts/recyclers/{recyclerId}/status")
    public R<Recycler> updateRecyclerStatus(@PathVariable Long recyclerId, @Valid @RequestBody RecyclerStatusReq req) {
        checkAdmin();
        return R.ok(accountService.updateRecyclerStatus(recyclerId, req.status, req.reason));
    }

    @PostMapping("/api/admin/accounts/recyclers/{recyclerId}/password/reset")
    public R<Map<String, Object>> resetRecyclerPassword(@PathVariable Long recyclerId) {
        checkAdmin();
        return R.ok(accountService.resetRecyclerPassword(recyclerId));
    }

    @PostMapping("/api/admin/points/adjust")
    public R<Map<String, Object>> adjustPoints(@Valid @RequestBody AdjustReq req) {
        checkAdmin();
        return R.ok(accountService.adjustPoints(req.userId, req.change, req.reason));
    }

    @GetMapping("/api/admin/appeals")
    public R<PageResult<Appeal>> appealList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        checkAdmin();
        return R.ok(accountService.appealList(page, size, status));
    }

    @PostMapping("/api/admin/appeals/{appealId}/handle")
    public R<Appeal> handleAppeal(@PathVariable Long appealId, @Valid @RequestBody HandleReq req) {
        checkAdmin();
        return R.ok(accountService.handleAppeal(appealId, req.result, req.conclusion));
    }

    private void checkAdmin() {
        if (!"ADMIN".equals(UserContext.getRole())) throw new ForbiddenException("当前角色无权访问该接口");
    }

    public record UserStatusReq(@NotBlank String status, String reason) {}
    public record ApproveReq(@NotNull Long siteId) {}
    public record RejectReq(@NotBlank String reason) {}
    public record RecyclerStatusReq(@NotBlank String status, String reason) {}
    public record AdjustReq(@NotNull Long userId, @NotNull Integer change, @NotBlank String reason) {}
    public record HandleReq(@NotBlank String result, @NotBlank String conclusion) {}
}
