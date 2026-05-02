package com.circleguard.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QrTokenServiceTest {

    private static final String SECRET = "my-qr-secret-key-for-tests-1234567890ab";
    private static final long EXPIRATION = 60000L;

    private QrTokenService service;

    @BeforeEach
    void setUp() {
        service = new QrTokenService(SECRET, EXPIRATION);
    }

    @Test
    void shouldGenerateQrTokenWithCorrectSubject() {
        UUID anonymousId = UUID.randomUUID();

        String token = service.generateQrToken(anonymousId);

        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();

        assertEquals(anonymousId.toString(), claims.getSubject());
    }

    @Test
    void shouldGenerateTokenWithExpirationWithinExpectedRange() {
        UUID anonymousId = UUID.randomUUID();
        long before = System.currentTimeMillis();

        String token = service.generateQrToken(anonymousId);

        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();

        long expiresAt = claims.getExpiration().getTime();
        assertTrue(expiresAt > before);
        assertTrue(expiresAt <= before + EXPIRATION + 1000);
    }

    @Test
    void shouldGenerateExpiredTokenWhenExpirationIsZero() {
        QrTokenService shortLivedService = new QrTokenService(SECRET, 1L);
        UUID anonymousId = UUID.randomUUID();

        String token = shortLivedService.generateQrToken(anonymousId);

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        assertThrows(ExpiredJwtException.class, () ->
                Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token));
    }

    @Test
    void shouldGenerateDifferentTokensForDifferentUsers() {
        String token1 = service.generateQrToken(UUID.randomUUID());
        String token2 = service.generateQrToken(UUID.randomUUID());
        assertNotEquals(token1, token2);
    }
}
