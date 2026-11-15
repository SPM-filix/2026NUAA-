package com.nuaa.recycle.user.controller;

import com.nuaa.recycle.common.api.ApiResponse;
import com.nuaa.recycle.common.api.PageResult;
import com.nuaa.recycle.common.auth.RequireRole;
import com.nuaa.recycle.common.auth.Role;
import com.nuaa.recycle.user.dto.AppealCreateRequest;
import com.nuaa.recycle.user.dto.AppealCreateResponse;
import com.nuaa.recycle.user.dto.AppealResponse;
import com.nuaa.recycle.user.dto.PointsLogResponse;
import com.nuaa.recycle.user.dto.PointsSummaryResponse;
import com.nuaa.recycle.user.dto.QrCodeResponse;
import com.nuaa.recycle.user.dto.UserDeliveryResponse;
import com.nuaa.recycle.user.dto.UserExchangeResponse;
import com.nuaa.recycle.user.dto.UserProfileResponse;
import com.nuaa.recycle.user.dto.UserUpdateRequest;
import com.nuaa.recycle.user.service.AppealService;
import com.nuaa.recycle.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@RequireRole(Role.USER)
public class UserController {

    private final UserService userService;
    private final AppealService appealService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> profile() {
        return ApiResponse.ok(userService.profile());
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok(userService.updateProfile(request));
    }

    @GetMapping("/me/qrcode")
    public ApiResponse<QrCodeResponse> qrCode() {
        return ApiResponse.ok(userService.qrCode());
    }

    @GetMapping("/me/points")
    public ApiResponse<PointsSummaryResponse> points() {
        return ApiResponse.ok(userService.pointsSummary());
    }

    @GetMapping("/me/points/logs")
    public ApiResponse<PageResult<PointsLogResponse>> pointsLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(userService.pointsLogs(page, clamp(size), type, from, to));
    }

    @GetMapping("/me/deliveries")
    public ApiResponse<PageResult<UserDeliveryResponse>> deliveries(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(userService.deliveries(page, clamp(size), categoryId, from, to));
    }

    @GetMapping("/me/exchanges")
    public ApiResponse<PageResult<UserExchangeResponse>> exchanges(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(userService.exchanges(page, clamp(size), status));
    }

    @GetMapping("/me/exchanges/{orderId}")
    public ApiResponse<UserExchangeResponse> exchangeDetail(@PathVariable Long orderId) {
        return ApiResponse.ok(userService.exchangeDetail(orderId));
    }

    @PostMapping("/me/appeals")
    public ApiResponse<AppealCreateResponse> createAppeal(@Valid @RequestBody AppealCreateRequest request) {
        return ApiResponse.ok(appealService.create(request));
    }

    @GetMapping("/me/appeals")
    public ApiResponse<PageResult<AppealResponse>> appeals(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(appealService.myAppeals(page, clamp(size)));
    }

    private long clamp(long size) {
        return Math.min(Math.max(size, 1), 50);
    }
}
