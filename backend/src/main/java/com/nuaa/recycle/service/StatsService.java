package com.nuaa.recycle.service;

import com.nuaa.recycle.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class StatsService {

    @Autowired private ModuleDeliveryRecordMapper deliveryRecordMapper;
    @Autowired private ModuleExchangeOrderMapper exchangeOrderMapper;
    @Autowired private ModuleUserMapper userMapper;
    @Autowired private ModuleRecyclerMapper recyclerMapper;
    @Autowired private ModuleAppealMapper appealMapper;
    @Autowired private ModuleRecycleCategoryMapper categoryMapper;
    @Autowired private ModuleProductMapper productMapper;

    public Map<String, Object> overview() {
        Map<String, Object> map = new HashMap<>();
        // 使用 MyBatis-Plus 的 selectCount / selectObjs 简化查询
        long userCount = userMapper.selectCount(null);
        long recyclerCount = recyclerMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.nuaa.recycle.entity.Recycler>()
                        .eq(com.nuaa.recycle.entity.Recycler::getStatus, "APPROVED"));
        long pendingRecyclerCount = recyclerMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.nuaa.recycle.entity.Recycler>()
                        .eq(com.nuaa.recycle.entity.Recycler::getStatus, "PENDING"));
        long pendingAppealCount = appealMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.nuaa.recycle.entity.Appeal>()
                        .eq(com.nuaa.recycle.entity.Appeal::getStatus, "PENDING"));

        map.put("userCount", userCount);
        map.put("recyclerCount", recyclerCount);
        map.put("pendingRecyclerCount", pendingRecyclerCount);
        map.put("pendingAppealCount", pendingAppealCount);
        // 其他字段由实际 SQL 统计，简化实现返回 0
        map.put("totalWeight", 0);
        map.put("totalPointsIssued", 0);
        map.put("totalPointsRedeemed", 0);
        map.put("completedOrderCount", 0);
        map.put("expiredPendingOrderCount", 0);
        return map;
    }

    public List<Map<String, Object>> categories(String from, String to) {
        List<Map<String, Object>> list = new ArrayList<>();
        // 简化实现，返回空列表或示例数据
        return list;
    }

    public List<Map<String, Object>> trend(int days) {
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Map<String, Object> m = new HashMap<>();
            m.put("date", d.format(DateTimeFormatter.ISO_LOCAL_DATE));
            m.put("weight", 0);
            m.put("points", 0);
            m.put("orderCount", 0);
            m.put("deliveryCount", 0);
            list.add(m);
        }
        return list;
    }

    public List<Map<String, Object>> top(String type, Integer limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        // 简化实现
        return list;
    }

    public Map<String, Object> workbench(Long recyclerId) {
        Map<String, Object> map = new HashMap<>();
        map.put("todayWeight", 0);
        map.put("todayPoints", 0);
        map.put("todayDeliveryCount", 0);
        map.put("todayRedeemCount", 0);
        map.put("siteName", "");
        map.put("weekTrend", new ArrayList<>());
        map.put("monthRank", null);
        return map;
    }
}
