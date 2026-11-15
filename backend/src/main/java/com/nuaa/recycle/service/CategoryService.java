package com.nuaa.recycle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nuaa.recycle.common.BizException;
import com.nuaa.recycle.dto.CategoryResponse;
import com.nuaa.recycle.entity.PointsRule;
import com.nuaa.recycle.entity.RecycleCategory;
import com.nuaa.recycle.mapper.ModuleDeliveryRecordMapper;
import com.nuaa.recycle.mapper.ModulePointsRuleMapper;
import com.nuaa.recycle.mapper.ModuleRecycleCategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private ModuleRecycleCategoryMapper categoryMapper;
    @Autowired
    private ModulePointsRuleMapper pointsRuleMapper;
    @Autowired
    private ModuleDeliveryRecordMapper deliveryRecordMapper;
    @Autowired
    private com.nuaa.recycle.catalog.mapper.PointsRuleMapper currentRuleMapper;

    /**
     * API-CAT-01：返回 categoryId 与当前生效系数。
     * 自定义 @Select 走 MyBatis-Plus 实体映射时会忽略 exist=false 字段，
     * 因此这里按类别逐个取当前规则，类别数量很小（个位数）。
     */
    public List<CategoryResponse> listWithRules(boolean enabledOnly) {
        List<RecycleCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<RecycleCategory>()
                        .eq(enabledOnly, RecycleCategory::getStatus, "ENABLED")
                        .orderByAsc(RecycleCategory::getSort)
                        .orderByAsc(RecycleCategory::getId));
        return categories.stream().map(category -> {
            com.nuaa.recycle.catalog.entity.PointsRule rule = currentRuleMapper.findCurrent(category.getId());
            return new CategoryResponse(category.getId(), category.getName(), category.getCode(),
                    category.getUnit(), category.getIconUrl(), category.getSort(), category.getStatus(),
                    rule == null ? null : rule.getPointsPerKg(),
                    rule == null ? null : rule.getEffectiveFrom());
        }).toList();
    }

    @Transactional
    public Long create(RecycleCategory dto) {
        Long exists = categoryMapper.selectCount(
                new LambdaQueryWrapper<RecycleCategory>().eq(RecycleCategory::getCode, dto.getCode()));
        if (exists > 0) throw new BizException(1001, "编码已存在");
        dto.setStatus("ENABLED");
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insert(dto);
        // 创建初始规则
        PointsRule rule = new PointsRule();
        rule.setCategoryId(dto.getId());
        rule.setPointsPerKg(dto.getPointsPerKg() != null ? dto.getPointsPerKg() : 1);
        rule.setEffectiveFrom(LocalDateTime.now());
        rule.setCreatedBy(com.nuaa.recycle.common.UserContext.getUserId());
        rule.setCreatedAt(LocalDateTime.now());
        pointsRuleMapper.insert(rule);
        return dto.getId();
    }

    @Transactional
    public RecycleCategory update(Long id, RecycleCategory dto) {
        RecycleCategory c = categoryMapper.selectById(id);
        if (c == null) throw new BizException(1005, "类别不存在");
        if (dto.getName() != null) c.setName(dto.getName());
        if (dto.getUnit() != null) c.setUnit(dto.getUnit());
        if (dto.getIconUrl() != null) c.setIconUrl(dto.getIconUrl());
        if (dto.getSort() != null) c.setSort(dto.getSort());
        c.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(c);
        return c;
    }

    @Transactional
    public void delete(Long id) {
        RecycleCategory c = categoryMapper.selectById(id);
        if (c == null) throw new BizException(1005, "类别不存在");
        Long count = deliveryRecordMapper.selectCount(
                new LambdaQueryWrapper<com.nuaa.recycle.entity.DeliveryRecord>().eq(com.nuaa.recycle.entity.DeliveryRecord::getCategoryId, id));
        if (count > 0) throw new BizException(1006, "该类别已有流水引用，请改为停用");
        categoryMapper.deleteById(id);
    }

    @Transactional
    public RecycleCategory updateStatus(Long id, String status) {
        RecycleCategory c = categoryMapper.selectById(id);
        if (c == null) throw new BizException(1005, "类别不存在");
        c.setStatus(status);
        c.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(c);
        return c;
    }

    @Transactional
    public PointsRule setRule(Long categoryId, Integer pointsPerKg, LocalDateTime effectiveFrom) {
        RecycleCategory c = categoryMapper.selectById(categoryId);
        if (c == null) throw new BizException(1005, "类别不存在");
        PointsRule rule = new PointsRule();
        rule.setCategoryId(categoryId);
        rule.setPointsPerKg(pointsPerKg);
        rule.setEffectiveFrom(effectiveFrom != null ? effectiveFrom : LocalDateTime.now());
        rule.setCreatedBy(com.nuaa.recycle.common.UserContext.getUserId());
        rule.setCreatedAt(LocalDateTime.now());
        pointsRuleMapper.insert(rule);
        return rule;
    }

    public Page<PointsRule> ruleHistory(Long categoryId, int page, int size) {
        Page<PointsRule> p = new Page<>(page, size);
        return pointsRuleMapper.selectPage(p,
                new LambdaQueryWrapper<PointsRule>().eq(PointsRule::getCategoryId, categoryId)
                        .orderByDesc(PointsRule::getEffectiveFrom));
    }
}
