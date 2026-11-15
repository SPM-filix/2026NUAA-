package com.nuaa.recycle.auth.controller;

import com.nuaa.recycle.auth.dto.AdminLoginRequest;
import com.nuaa.recycle.auth.dto.ChangePasswordRequest;
import com.nuaa.recycle.auth.dto.LoginRequest;
import com.nuaa.recycle.auth.dto.LoginResponse;
import com.nuaa.recycle.auth.dto.RecyclerRegisterRequest;
import com.nuaa.recycle.auth.dto.RecyclerRegisterResponse;
import com.nuaa.recycle.auth.dto.RegisterRequest;
import com.nuaa.recycle.auth.service.AuthService;
import com.nuaa.recycle.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/users/register")
    public ApiResponse<LoginResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.registerUser(request));
    }

    @PostMapping("/users/login")
    public ApiResponse<LoginResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.loginUser(request));
    }

    @PostMapping("/recyclers/register")
    public ApiResponse<RecyclerRegisterResponse> registerRecycler(
            @Valid @RequestBody RecyclerRegisterRequest request) {
        return ApiResponse.ok(authService.registerRecycler(request));
    }

    @PostMapping("/recyclers/login")
    public ApiResponse<LoginResponse> loginRecycler(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.loginRecycler(request));
    }

    @PostMapping("/admins/login")
    public ApiResponse<LoginResponse> loginAdmin(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok(authService.loginAdmin(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.ok();
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.ok();
    }
}
