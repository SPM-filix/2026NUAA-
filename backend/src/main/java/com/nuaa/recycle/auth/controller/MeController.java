package com.nuaa.recycle.auth.controller;

import com.nuaa.recycle.auth.dto.AdminMeResponse;
import com.nuaa.recycle.auth.dto.RecyclerMeResponse;
import com.nuaa.recycle.auth.service.AuthService;
import com.nuaa.recycle.common.api.ApiResponse;
import com.nuaa.recycle.common.auth.RequireRole;
import com.nuaa.recycle.common.auth.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeController {

    private final AuthService authService;

    @GetMapping("/api/recyclers/me")
    @RequireRole(Role.RECYCLER)
    public ApiResponse<RecyclerMeResponse> recyclerMe() {
        return ApiResponse.ok(authService.recyclerMe());
    }

    @GetMapping("/api/admins/me")
    @RequireRole(Role.ADMIN)
    public ApiResponse<AdminMeResponse> adminMe() {
        return ApiResponse.ok(authService.adminMe());
    }
}
