package com.nuaa.recycle.common.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class CodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String REDEEM_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final AtomicLong recordSeq = new AtomicLong(0);
    private final AtomicLong orderSeq = new AtomicLong(0);

    public String userCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    public String recordNo() {
        return "D" + LocalDate.now().format(DATE) + next(recordSeq);
    }

    public String orderNo() {
        return "E" + LocalDate.now().format(DATE) + next(orderSeq);
    }

    public String redeemCode() {
        StringBuilder builder = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            builder.append(REDEEM_CHARS.charAt(RANDOM.nextInt(REDEEM_CHARS.length())));
        }
        return builder.toString();
    }

    private String next(AtomicLong sequence) {
        long value = Math.floorMod(sequence.incrementAndGet(), 1_000_000L);
        return String.format("%06d", value);
    }
}
