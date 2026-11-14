package com.nuaa.recycle.delivery.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PointsCalculatorTest {

    @Test
    void roundsHalfUp() {
        assertEquals(24, DeliveryService.calculatePoints(new BigDecimal("2.35"), 10));
    }

    @Test
    void minimumOnePointWhenWeightPositive() {
        assertEquals(1, DeliveryService.calculatePoints(new BigDecimal("0.01"), 8));
    }

    @Test
    void upperBoundary() {
        assertEquals(2000, DeliveryService.calculatePoints(new BigDecimal("100.00"), 20));
    }

    @Test
    void zeroWhenWeightZero() {
        assertEquals(0, DeliveryService.calculatePoints(BigDecimal.ZERO, 10));
    }
}
