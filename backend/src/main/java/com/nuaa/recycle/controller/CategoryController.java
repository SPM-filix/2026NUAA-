package com.nuaa.recycle.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nuaa.recycle.common.*;
import com.nuaa.recycle.entity.PointsRule;
import com.nuaa.recycle.entity.RecycleCategory;
import com.nuaa.recycle.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Validated
@RestController
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/api/categories")
    public R<List<RecycleCategory>> list(@RequestParam(required = false, defaultValue = "true") boolean enabledOnly) {
        if (enabledOnly) {
            return R.ok(categoryService.listEnabled());
        }
        checkAdmin();
        return R.ok(categoryService.listWithRules());
    }

    @PostMapping("/api/admin/categories")
    public R<Map<String, Long>> create(@Valid @RequestBody RecycleCategory dto) {
        checkAdmin();
        Long id = categoryService.create(dto);
        return R.ok(Map.of("categoryId", id));
    }

    @PutMapping("/api/admin/categories/{categoryId}")
    public R<RecycleCategory> update(@PathVariable Long categoryId, @RequestBody RecycleCategory dto) {
        checkAdmin();
        return R.ok(categoryService.update(categoryId, dto));
    }

    @DeleteMapping("/api/admin/categories/{categoryId}")
    public R<Void> delete(@PathVariable Long categoryId) {
        checkAdmin();
        categoryService.delete(categoryId);
        return R.ok();
    }

    @PatchMapping("/api/admin/categories/{categoryId}/status")
    public R<RecycleCategory> updateStatus(@PathVariable Long categoryId, @Valid @RequestBody StatusReq req) {
        checkAdmin();
        return R.ok(categoryService.updateStatus(categoryId, req.status));
    }

    @PutMapping("/api/admin/categories/{categoryId}/rule")
    public R<PointsRule> setRule(@PathVariable Long categoryId, @Valid @RequestBody RuleReq req) {
        checkAdmin();
        return R.ok(categoryService.setRule(categoryId, req.pointsPerKg, req.effectiveFrom));
    }

    @GetMapping("/api/admin/categories/{categoryId}/rules")
    public R<PageResult<PointsRule>> ruleHistory(@PathVariable Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        checkAdmin();
        Page<PointsRule> p = categoryService.ruleHistory(categoryId, page, size);
        return R.ok(PageResult.of(p.getTotal(), page, size, p.getRecords()));
    }

    private void checkAdmin() {
        if (!"ADMIN".equals(UserContext.getRole())) throw new ForbiddenException("当前角色无权访问该接口");
    }

    public record StatusReq(@NotBlank String status) {}
    public record RuleReq(@NotNull Integer pointsPerKg, LocalDateTime effectiveFrom) {}
}
