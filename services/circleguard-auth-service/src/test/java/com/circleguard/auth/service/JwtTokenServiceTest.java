package com.circleguard.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private static final String SECRET = "test-jwt-secret-that-is-at-least-32-characters-long-for-hmac";
    private static final long EXPIRATION = 3600000L;

    private JwtTokenService service;

    @BeforeEach
    void setUp() {
        service = new JwtTokenService(SECRET, EXPIRATION);
    }

    @Test
    void shouldGenerateTokenWithAnonymousIdAsSubject() {
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("user:read")));

        String token = service.generateToken(anonymousId, auth);

        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();

        assertEquals(anonymousId.toString(), claims.getSubject());
    }

    @Test
    void shouldGenerateTokenContainingPermissionsClaim() {
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(
                        new SimpleGrantedAuthority("user:read"),
                        new SimpleGrantedAuthority("user:write")));

        String token = service.generateToken(anonymousId, auth);

        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();

        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) claims.get("permissions");
        assertNotNull(permissions);
        assertTrue(permissions.contains("user:read"));
        assertTrue(permissions.contains("user:write"));
    }

    @Test
    void shouldGenerateTokenThatIsNotYetExpired() {
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken("user", null, List.of());

        String token = service.generateToken(anonymousId, auth);

        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();

        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    void shouldGenerateTokenWithEmptyPermissionsForAnonymousUser() {
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken("visitor", null, List.of());

        String token = service.generateToken(anonymousId, auth);

        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();

        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) claims.get("permissions");
        assertNotNull(permissions);
        assertTrue(permissions.isEmpty());
    }

    @Test
    void shouldGenerateDifferentTokensForDifferentUsers() {
        Authentication auth = new UsernamePasswordAuthenticationToken("user", null, List.of());

        String token1 = service.generateToken(UUID.randomUUID(), auth);
        String token2 = service.generateToken(UUID.randomUUID(), auth);

        assertNotEquals(token1, token2);
    }
}
