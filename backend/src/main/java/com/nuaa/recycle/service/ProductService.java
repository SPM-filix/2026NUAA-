package com.nuaa.recycle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nuaa.recycle.common.*;
import com.nuaa.recycle.entity.Product;
import com.nuaa.recycle.mapper.ExchangeOrderMapper;
import com.nuaa.recycle.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ExchangeOrderMapper exchangeOrderMapper;

    public PageResult<Product> list(Integer page, Integer size, String keyword, String sort, Boolean onlyAvailable) {
        Page<Product> p = new Page<>(page, size);
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<Product>();
        if (StringUtils.hasText(keyword)) {
            qw.like(Product::getName, keyword);
        }
        if (Boolean.TRUE.equals(onlyAvailable)) {
            qw.gt(Product::getStock, 0);
        }
        // USER 侧只返回 ON
        if (!"ADMIN".equals(UserContext.getRole())) {
            qw.eq(Product::getStatus, "ON");
        }
        // sort
        if ("points_asc".equals(sort)) {
            qw.orderByAsc(Product::getPointsPrice);
        } else if ("points_desc".equals(sort)) {
            qw.orderByDesc(Product::getPointsPrice);
        } else {
            qw.orderByDesc(Product::getCreatedAt);
        }
        Page<Product> result = productMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    public Product detail(Long productId) {
        Product p = productMapper.selectById(productId);
        if (p == null) throw new BizException(1005, "商品不存在");
        return p;
    }

    @Transactional
    public Long create(Product dto) {
        dto.setSoldCount(0);
        dto.setStatus("ON");
        dto.setVersion(0);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        productMapper.insert(dto);
        return dto.getId();
    }

    @Transactional
    public Product update(Long productId, Product dto) {
        Product existing = productMapper.selectById(productId);
        if (existing == null) throw new BizException(1005, "商品不存在");
        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getCoverUrl() != null) existing.setCoverUrl(dto.getCoverUrl());
        if (dto.getPointsPrice() != null) existing.setPointsPrice(dto.getPointsPrice());
        if (dto.getSpec() != null) existing.setSpec(dto.getSpec());
        if (dto.getExchangeNotice() != null) existing.setExchangeNotice(dto.getExchangeNotice());
        existing.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(existing);
        return existing;
    }

    @Transactional
    public Product updateStatus(Long productId, String status) {
        Product p = productMapper.selectById(productId);
        if (p == null) throw new BizException(1005, "商品不存在");
        p.setStatus(status);
        p.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(p);
        return p;
    }

    @Transactional
    public Map<String, Object> adjustStock(Long productId, String mode, Integer value, String reason) {
        Product p = productMapper.selectById(productId);
        if (p == null) throw new BizException(1005, "商品不存在");
        int before = p.getStock();
        int after;
        if ("SET".equals(mode)) {
            after = value;
        } else {
            after = before + value;
        }
        if (after < 0) throw new BizException(3011, "库存不能为负");
        p.setStock(after);
        p.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(p);
        Map<String, Object> map = new HashMap<>();
        map.put("productId", productId);
        map.put("stockBefore", before);
        map.put("stock", after);
        map.put("adjustId", 0); // simplified
        return map;
    }

    @Transactional
    public void delete(Long productId) {
        Product p = productMapper.selectById(productId);
        if (p == null) throw new BizException(1005, "商品不存在");
        Long count = exchangeOrderMapper.selectCount(
                new LambdaQueryWrapper<com.nuaa.recycle.entity.ExchangeOrder>().eq(com.nuaa.recycle.entity.ExchangeOrder::getProductId, productId));
        if (count > 0) throw new BizException(1006, "该商品已有订单关联，请改为下架");
        productMapper.deleteById(productId);
    }
}
