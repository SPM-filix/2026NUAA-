package com.nuaa.recycle.exchange.controller;

import com.nuaa.recycle.common.api.ApiResponse;
import com.nuaa.recycle.common.api.PageResult;
import com.nuaa.recycle.common.auth.RequireRole;
import com.nuaa.recycle.common.auth.Role;
import com.nuaa.recycle.exchange.dto.CancelRequest;
import com.nuaa.recycle.exchange.dto.CancelResponse;
import com.nuaa.recycle.exchange.dto.ExchangeCreateRequest;
import com.nuaa.recycle.exchange.dto.ExchangeOrderResponse;
import com.nuaa.recycle.exchange.dto.RedeemRequest;
import com.nuaa.recycle.exchange.dto.RedeemResponse;
import com.nuaa.recycle.exchange.service.ExchangeService;
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
public class ExchangeController {

    private final ExchangeService exchangeService;

    @PostMapping("/api/exchanges")
    @RequireRole(Role.USER)
    public ApiResponse<ExchangeOrderResponse> create(@Valid @RequestBody ExchangeCreateRequest request) {
        return ApiResponse.ok(exchangeService.create(request));
    }

    @GetMapping("/api/exchanges/{orderId}")
    @RequireRole({Role.USER, Role.ADMIN})
    public ApiResponse<ExchangeOrderResponse> detail(@PathVariable Long orderId) {
        return ApiResponse.ok(exchangeService.detail(orderId));
    }

    @PostMapping("/api/exchanges/redeem")
    @RequireRole(Role.RECYCLER)
    public ApiResponse<RedeemResponse> redeem(@Valid @RequestBody RedeemRequest request) {
        return ApiResponse.ok(exchangeService.redeem(request.redeemCode()));
    }

    @GetMapping("/api/exchanges/lookup")
    @RequireRole(Role.RECYCLER)
    public ApiResponse<ExchangeOrderResponse> lookup(@RequestParam String redeemCode) {
        return ApiResponse.ok(exchangeService.lookup(redeemCode));
    }

    @GetMapping("/api/recyclers/me/redemptions")
    @RequireRole(Role.RECYCLER)
    public ApiResponse<PageResult<ExchangeOrderResponse>> myRedemptions(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(exchangeService.myRedemptions(page, clamp(size), from, to));
    }

    @GetMapping("/api/admin/exchanges")
    @RequireRole(Role.ADMIN)
    public ApiResponse<PageResult<ExchangeOrderResponse>> adminList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String orderNo) {
        return ApiResponse.ok(exchangeService.adminList(page, clamp(size), status, userId, orderNo));
    }

    @PostMapping("/api/admin/exchanges/{orderId}/cancel")
    @RequireRole(Role.ADMIN)
    public ApiResponse<CancelResponse> cancel(@PathVariable Long orderId,
                                              @Valid @RequestBody CancelRequest request) {
        return ApiResponse.ok(exchangeService.cancel(orderId, request.reason()));
    }

    private long clamp(long size) {
        return Math.min(Math.max(size, 1), 50);
    }
}
