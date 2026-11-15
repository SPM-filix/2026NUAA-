package com.nuaa.recycle.delivery.controller;

import com.nuaa.recycle.common.api.ApiResponse;
import com.nuaa.recycle.common.api.PageResult;
import com.nuaa.recycle.common.auth.RequireRole;
import com.nuaa.recycle.common.auth.Role;
import com.nuaa.recycle.delivery.dto.DeliveryCreateRequest;
import com.nuaa.recycle.delivery.dto.DeliveryCreateResponse;
import com.nuaa.recycle.delivery.dto.DeliveryRecordResponse;
import com.nuaa.recycle.delivery.dto.RevokeRequest;
import com.nuaa.recycle.delivery.dto.RevokeResponse;
import com.nuaa.recycle.delivery.service.DeliveryService;
import com.nuaa.recycle.user.dto.UserLookupResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping("/api/deliveries")
    @RequireRole(Role.RECYCLER)
    public ApiResponse<DeliveryCreateResponse> create(@Valid @RequestBody DeliveryCreateRequest request) {
        return ApiResponse.ok(deliveryService.create(request));
    }

    @GetMapping("/api/users/lookup")
    @RequireRole(Role.RECYCLER)
    public ApiResponse<UserLookupResponse> lookup(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String qrContent) {
        return ApiResponse.ok(deliveryService.lookup(keyword, qrContent));
    }

    @GetMapping("/api/recyclers/me/deliveries")
    @RequireRole(Role.RECYCLER)
    public ApiResponse<PageResult<DeliveryRecordResponse>> myRecords(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(deliveryService.myRecords(page, clamp(size), status, categoryId, from, to));
    }

    @PostMapping("/api/deliveries/{recordId}/revoke")
    @RequireRole(Role.RECYCLER)
    public ApiResponse<RevokeResponse> revoke(@PathVariable Long recordId,
                                              @Valid @RequestBody RevokeRequest request) {
        return ApiResponse.ok(deliveryService.revoke(recordId, request.reason()));
    }

    @GetMapping("/api/admin/deliveries")
    @RequireRole(Role.ADMIN)
    public ApiResponse<PageResult<DeliveryRecordResponse>> adminList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long recyclerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(deliveryService.adminList(page, clamp(size), recyclerId,
                status, categoryId, from, to));
    }

    @PostMapping("/api/admin/deliveries/{recordId}/revoke")
    @RequireRole(Role.ADMIN)
    public ApiResponse<RevokeResponse> adminRevoke(@PathVariable Long recordId,
                                                   @Valid @RequestBody RevokeRequest request) {
        return ApiResponse.ok(deliveryService.adminRevoke(recordId, request.reason()));
    }

    @GetMapping("/api/deliveries/{recordId}")
    @RequireRole({Role.RECYCLER, Role.ADMIN})
    public ApiResponse<DeliveryRecordResponse> detail(@PathVariable Long recordId) {
        return ApiResponse.ok(deliveryService.detail(recordId));
    }

    private long clamp(long size) {
        return Math.min(Math.max(size, 1), 50);
    }
}
