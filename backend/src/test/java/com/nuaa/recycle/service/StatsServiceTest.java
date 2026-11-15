package com.nuaa.recycle.service;

import com.nuaa.recycle.common.BizException;
import com.nuaa.recycle.common.error.ErrorCode;
import com.nuaa.recycle.entity.Recycler;
import com.nuaa.recycle.mapper.ModuleAppealMapper;
import com.nuaa.recycle.mapper.ModuleRecyclerMapper;
import com.nuaa.recycle.mapper.ModuleStatsMapper;
import com.nuaa.recycle.mapper.ModuleUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private ModuleStatsMapper statsMapper;
    @Mock
    private ModuleUserMapper userMapper;
    @Mock
    private ModuleRecyclerMapper recyclerMapper;
    @Mock
    private ModuleAppealMapper appealMapper;

    @InjectMocks
    private StatsService statsService;

    @Test
    void overviewAggregatesTotalsAndCounts() {
        Map<String, Object> delivery = new LinkedHashMap<>();
        delivery.put("totalWeight", new BigDecimal("104.70"));
        delivery.put("totalPointsIssued", 1302L);
        when(statsMapper.deliveryTotals()).thenReturn(delivery);

        Map<String, Object> exchange = new LinkedHashMap<>();
        exchange.put("totalPointsRedeemed", 1600L);
        exchange.put("completedOrderCount", 3L);
        exchange.put("expiredPendingOrderCount", 1L);
        when(statsMapper.exchangeTotals()).thenReturn(exchange);

        when(userMapper.selectCount(isNull())).thenReturn(6L);
        when(recyclerMapper.selectCount(any())).thenReturn(3L, 1L);
        when(appealMapper.selectCount(any())).thenReturn(2L);

        Map<String, Object> result = statsService.overview();

        assertEquals(new BigDecimal("104.70"), result.get("totalWeight"));
        assertEquals(1302L, result.get("totalPointsIssued"));
        assertEquals(1600L, result.get("totalPointsRedeemed"));
        assertEquals(3L, result.get("completedOrderCount"));
        assertEquals(1L, result.get("expiredPendingOrderCount"));
        assertEquals(6L, result.get("userCount"));
        assertEquals(3L, result.get("recyclerCount"));
        assertEquals(1L, result.get("pendingRecyclerCount"));
        assertEquals(2L, result.get("pendingAppealCount"));
    }

    @Test
    void categoriesComputesWeightRatio() {
        Map<String, Object> paper = categoryRow(1L, "纸张", "30.00", 300L, 6L);
        Map<String, Object> metal = categoryRow(2L, "金属", "10.00", 200L, 2L);
        when(statsMapper.categoryStats(isNull(), isNull())).thenReturn(List.of(paper, metal));

        List<Map<String, Object>> list = statsService.categories(null, null);

        assertEquals(2, list.size());
        assertEquals(new BigDecimal("0.75"), list.get(0).get("weightRatio"));
        assertEquals(new BigDecimal("0.25"), list.get(1).get("weightRatio"));
        assertEquals(6L, list.get(0).get("recordCount"));
    }

    @Test
    void categoriesRejectsReversedDateRange() {
        BizException exception = assertThrows(BizException.class,
                () -> statsService.categories("2026-11-15", "2026-11-01"));

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), exception.getCode());
    }

    @Test
    void trendFillsMissingDays() {
        String today = LocalDate.now().toString();
        Map<String, Object> day = new LinkedHashMap<>();
        day.put("statDate", today);
        day.put("weight", new BigDecimal("5.00"));
        day.put("points", 50L);
        day.put("deliveryCount", 2L);
        when(statsMapper.deliveryTrend(any(), any())).thenReturn(List.of(day));

        Map<String, Object> order = new LinkedHashMap<>();
        order.put("statDate", today);
        order.put("orderCount", 1L);
        when(statsMapper.orderTrend(any(), any())).thenReturn(List.of(order));

        List<Map<String, Object>> list = statsService.trend(7);

        assertEquals(7, list.size());
        assertEquals(today, list.get(6).get("date"));
        assertEquals(2L, list.get(6).get("deliveryCount"));
        assertEquals(1L, list.get(6).get("orderCount"));
        assertEquals(0L, list.get(0).get("deliveryCount"));
        assertTrue(((BigDecimal) list.get(0).get("weight")).compareTo(BigDecimal.ZERO) == 0);
    }

    @Test
    void topMasksUserNickname() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 1L);
        row.put("name", "小魏");
        row.put("value", new BigDecimal("12.50"));
        row.put("extra", 120L);
        when(statsMapper.topUsers(eq(5))).thenReturn(List.of(row));

        List<Map<String, Object>> list = statsService.top("USER", 5);

        assertEquals("小**", list.get(0).get("name"));
        assertEquals(new BigDecimal("12.50"), list.get(0).get("value"));
        assertEquals(120L, list.get(0).get("extra"));
    }

    @Test
    void workbenchReturnsTodayAndRank() {
        Recycler recycler = new Recycler();
        recycler.setId(9L);
        recycler.setSiteId(1L);
        when(recyclerMapper.selectById(9L)).thenReturn(recycler);

        Map<String, Object> today = new LinkedHashMap<>();
        today.put("todayWeight", new BigDecimal("8.00"));
        today.put("todayPoints", 80L);
        today.put("todayDeliveryCount", 2L);
        when(statsMapper.workbenchToday(9L)).thenReturn(today);

        Map<String, Object> redeem = new LinkedHashMap<>();
        redeem.put("todayRedeemCount", 1L);
        when(statsMapper.workbenchTodayRedeem(9L)).thenReturn(redeem);
        when(statsMapper.workbenchSiteName(9L)).thenReturn("第一站点");
        when(statsMapper.recyclerWeightTrend(eq(9L), any(), any())).thenReturn(List.of());
        when(statsMapper.monthRank(9L)).thenReturn(2);

        Map<String, Object> result = statsService.workbench(9L);

        assertEquals(new BigDecimal("8.00"), result.get("todayWeight"));
        assertEquals(80L, result.get("todayPoints"));
        assertEquals(1L, result.get("todayRedeemCount"));
        assertEquals("第一站点", result.get("siteName"));
        assertEquals(2, result.get("monthRank"));
        assertEquals(7, ((List<?>) result.get("weekTrend")).size());
    }

    @Test
    void workbenchRejectsUnknownRecycler() {
        when(recyclerMapper.selectById(9L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> statsService.workbench(9L));

        assertEquals(ErrorCode.NOT_FOUND.getCode(), exception.getCode());
    }

    private Map<String, Object> categoryRow(Long id, String name, String weight, Long points, Long count) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("categoryId", id);
        row.put("categoryName", name);
        row.put("weight", new BigDecimal(weight));
        row.put("points", points);
        row.put("recordCount", count);
        return row;
    }
}
