package com.nuaa.recycle.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeGeneratorTest {

    private final CodeGenerator generator = new CodeGenerator();

    @Test
    void userCodeIsSixDigits() {
        assertTrue(generator.userCode().matches("^\\d{6}$"));
    }

    @Test
    void redeemCodeIsEightUppercaseChars() {
        assertTrue(generator.redeemCode().matches("^[A-Z2-9]{8}$"));
    }

    @Test
    void recordNoAndOrderNoHaveExpectedPrefix() {
        assertTrue(generator.recordNo().matches("^D\\d{14}$"));
        assertTrue(generator.orderNo().matches("^E\\d{14}$"));
    }
}
