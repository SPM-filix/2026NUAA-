package com.nuaa.recycle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nuaa.recycle.common.BizException;
import com.nuaa.recycle.common.error.ErrorCode;
import com.nuaa.recycle.entity.Appeal;
import com.nuaa.recycle.entity.Recycler;
import com.nuaa.recycle.mapper.ModuleAppealMapper;
import com.nuaa.recycle.mapper.ModuleRecyclerMapper;
import com.nuaa.recycle.mapper.ModuleStatsMapper;
import com.nuaa.recycle.mapper.ModuleUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计聚合（API-STA-01～05）。SQL 见 {@link ModuleStatsMapper}，这里只做数值归一化和补零。
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final ModuleStatsMapper statsMapper;
    private final ModuleUserMapper userMapper;
    private final ModuleRecyclerMapper recyclerMapper;
    private final ModuleAppealMapper appealMapper;

    public Map<String, Object> overview() {
        Map<String, Object> delivery = statsMapper.deliveryTotals();
        Map<String, Object> exchange = statsMapper.exchangeTotals();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalWeight", round2(decimal(value(delivery, "totalWeight"))));
        map.put("totalPointsIssued", toLong(value(delivery, "totalPointsIssued")));
        map.put("totalPointsRedeemed", toLong(value(exchange, "totalPointsRedeemed")));
        map.put("completedOrderCount", toLong(value(exchange, "completedOrderCount")));
        map.put("expiredPendingOrderCount", toLong(value(exchange, "expiredPendingOrderCount")));
        map.put("userCount", userMapper.selectCount(null));
        map.put("recyclerCount", recyclerMapper.selectCount(
                new LambdaQueryWrapper<Recycler>().eq(Recycler::getStatus, "APPROVED")));
        map.put("pendingRecyclerCount", recyclerMapper.selectCount(
                new LambdaQueryWrapper<Recycler>().eq(Recycler::getStatus, "PENDING")));
        map.put("pendingAppealCount", appealMapper.selectCount(
                new LambdaQueryWrapper<Appeal>().eq(Appeal::getStatus, "PENDING")));
        return map;
    }

    public List<Map<String, Object>> categories(String from, String to) {
        LocalDate fromDate = parseDate(from, "from");
        LocalDate toDate = parseDate(to, "to");
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), "开始日期不能晚于结束日期");
        }

        List<Map<String, Object>> rows = statsMapper.categoryStats(fromDate, toDate);
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            totalWeight = totalWeight.add(decimal(row.get("weight")));
        }

        List<Map<String, Object>> list = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            BigDecimal weight = decimal(row.get("weight"));
            BigDecimal ratio = totalWeight.signum() == 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : weight.divide(totalWeight, 2, RoundingMode.HALF_UP);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("categoryId", toLong(row.get("categoryId")));
            item.put("categoryName", row.get("categoryName"));
            item.put("weight", round2(weight));
            item.put("weightRatio", ratio);
            item.put("points", toLong(row.get("points")));
            item.put("recordCount", toLong(row.get("recordCount")));
            list.add(item);
        }
        return list;
    }

    public List<Map<String, Object>> trend(int days) {
        int span = days == 30 ? 30 : 7;
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(span - 1L);
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endExclusive = end.plusDays(1L).atStartOfDay();

        Map<String, Map<String, Object>> byDate = new LinkedHashMap<>();
        for (Map<String, Object> row : statsMapper.deliveryTrend(startAt, endExclusive)) {
            Map<String, Object> item = blankDay(String.valueOf(row.get("statDate")));
            item.put("weight", round2(decimal(row.get("weight"))));
            item.put("points", toLong(row.get("points")));
            item.put("deliveryCount", toLong(row.get("deliveryCount")));
            byDate.put(String.valueOf(row.get("statDate")), item);
        }
        for (Map<String, Object> row : statsMapper.orderTrend(startAt, endExclusive)) {
            String date = String.valueOf(row.get("statDate"));
            Map<String, Object> item = byDate.computeIfAbsent(date, this::blankDay);
            item.put("orderCount", toLong(row.get("orderCount")));
        }

        List<Map<String, Object>> list = new ArrayList<>(span);
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1L)) {
            String key = day.format(ISO_DATE);
            list.add(byDate.getOrDefault(key, blankDay(key)));
        }
        return list;
    }

    public List<Map<String, Object>> top(String type, Integer limit) {
        String normalized = "USER".equalsIgnoreCase(type) ? "USER" : "PRODUCT";
        int capped = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<Map<String, Object>> rows = "USER".equals(normalized)
                ? statsMapper.topUsers(capped)
                : statsMapper.topProducts(capped);

        List<Map<String, Object>> list = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", toLong(row.get("id")));
            item.put("name", "USER".equals(normalized)
                    ? maskNickname(row.get("name"))
                    : row.get("name"));
            item.put("value", "USER".equals(normalized)
                    ? round2(decimal(row.get("value")))
                    : toLong(row.get("value")));
            item.put("extra", toLong(row.get("extra")));
            list.add(item);
        }
        return list;
    }

    public Map<String, Object> workbench(Long recyclerId) {
        Recycler recycler = recyclerMapper.selectById(recyclerId);
        if (recycler == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "回收员不存在");
        }

        Map<String, Object> today = statsMapper.workbenchToday(recyclerId);
        Map<String, Object> redeem = statsMapper.workbenchTodayRedeem(recyclerId);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("todayWeight", round2(decimal(value(today, "todayWeight"))));
        map.put("todayPoints", toLong(value(today, "todayPoints")));
        map.put("todayDeliveryCount", toLong(value(today, "todayDeliveryCount")));
        map.put("todayRedeemCount", toLong(value(redeem, "todayRedeemCount")));
        map.put("siteName", statsMapper.workbenchSiteName(recyclerId));
        map.put("weekTrend", weekTrend(recyclerId));
        map.put("monthRank", statsMapper.monthRank(recyclerId));
        return map;
    }

    private List<Map<String, Object>> weekTrend(Long recyclerId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        List<Map<String, Object>> rows = statsMapper.recyclerWeightTrend(
                recyclerId, start.atStartOfDay(), end.plusDays(1L).atStartOfDay());

        Map<String, BigDecimal> byDate = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            byDate.put(String.valueOf(row.get("statDate")), decimal(row.get("weight")));
        }

        List<Map<String, Object>> list = new ArrayList<>(7);
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1L)) {
            String key = day.format(ISO_DATE);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", key);
            item.put("weight", round2(byDate.getOrDefault(key, BigDecimal.ZERO)));
            list.add(item);
        }
        return list;
    }

    private Map<String, Object> blankDay(String date) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("date", date);
        item.put("weight", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        item.put("points", 0L);
        item.put("orderCount", 0L);
        item.put("deliveryCount", 0L);
        return item;
    }

    private String maskNickname(Object nickname) {
        if (nickname == null) {
            return "匿名用户";
        }
        String name = nickname.toString().trim();
        if (name.isEmpty()) {
            return "匿名用户";
        }
        return name.substring(0, 1) + "**";
    }

    private LocalDate parseDate(String value, String field) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new BizException(ErrorCode.PARAM_INVALID.getCode(), field + " 需为 yyyy-MM-dd 格式");
        }
    }

    private static Object value(Map<String, Object> row, String key) {
        return row == null ? null : row.get(key);
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal round2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return new BigDecimal(value.toString()).longValue();
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
