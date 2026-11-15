package com.nuaa.recycle.controller;

import com.nuaa.recycle.common.*;
import com.nuaa.recycle.entity.Product;
import com.nuaa.recycle.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/api/products")
    public R<PageResult<Product>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "created_desc") String sort,
            @RequestParam(defaultValue = "false") boolean onlyAvailable) {
        return R.ok(productService.list(page, size, keyword, sort, onlyAvailable));
    }

    @GetMapping("/api/products/{productId}")
    public R<Product> detail(@PathVariable Long productId) {
        return R.ok(productService.detail(productId));
    }

    @PostMapping("/api/admin/products")
    public R<Map<String, Long>> create(@Valid @RequestBody Product dto) {
        checkAdmin();
        Long id = productService.create(dto);
        return R.ok(Map.of("productId", id));
    }

    @PutMapping("/api/admin/products/{productId}")
    public R<Product> update(@PathVariable Long productId, @RequestBody Product dto) {
        checkAdmin();
        return R.ok(productService.update(productId, dto));
    }

    @PatchMapping("/api/admin/products/{productId}/status")
    public R<Product> updateStatus(@PathVariable Long productId, @RequestBody @Valid StatusReq req) {
        checkAdmin();
        return R.ok(productService.updateStatus(productId, req.status));
    }

    @PatchMapping("/api/admin/products/{productId}/stock")
    public R<Map<String, Object>> adjustStock(@PathVariable Long productId, @Valid @RequestBody StockReq req) {
        checkAdmin();
        return R.ok(productService.adjustStock(productId, req.mode, req.value, req.reason));
    }

    @DeleteMapping("/api/admin/products/{productId}")
    public R<Void> delete(@PathVariable Long productId) {
        checkAdmin();
        productService.delete(productId);
        return R.ok();
    }

    private void checkAdmin() {
        if (!"ADMIN".equals(UserContext.getRole())) throw new ForbiddenException("当前角色无权访问该接口");
    }

    public record StatusReq(@NotBlank String status) {}
    public record StockReq(@NotBlank String mode, @NotNull Integer value, @NotBlank String reason) {}
}
