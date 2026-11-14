package com.nuaa.recycle.delivery.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevokeWindowTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 11, 14, 12, 0, 0);

    @Test
    void withinWindowAt299Seconds() {
        assertTrue(DeliveryService.isWithinRevokeWindow(now.minusSeconds(299), now));
    }

    @Test
    void withinWindowAtExactly300Seconds() {
        assertTrue(DeliveryService.isWithinRevokeWindow(now.minusSeconds(300), now));
    }

    @Test
    void expiredAt301Seconds() {
        assertFalse(DeliveryService.isWithinRevokeWindow(now.minusSeconds(301), now));
    }

    @Test
    void nullCreatedAtIsNotRevocable() {
        assertFalse(DeliveryService.isWithinRevokeWindow(null, now));
    }
}
