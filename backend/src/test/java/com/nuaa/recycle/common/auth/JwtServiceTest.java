package com.nuaa.recycle.common.auth;

import com.nuaa.recycle.common.error.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "0123456789012345678901234567890123456789", 120);

    @Test
    void generatesAndParsesAccessToken() {
        String token = jwtService.generate(7L, Role.RECYCLER, 3);
        JwtService.JwtPayload payload = jwtService.parse(token);

        assertEquals(7L, payload.id());
        assertEquals(Role.RECYCLER, payload.role());
        assertEquals(3, payload.version());
        assertEquals("ACCESS", payload.type());
    }

    @Test
    void generatesQrTokenWithQrType() {
        String token = jwtService.generateQrToken(9L, 0);
        JwtService.JwtPayload payload = jwtService.parse(token);

        assertEquals(9L, payload.id());
        assertEquals("QR", payload.type());
    }

    @Test
    void rejectsMalformedToken() {
        assertThrows(BizException.class, () -> jwtService.parse("not-a-token"));
    }

    @Test
    void rejectsShortSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short-secret", 120));
    }
}
