package com.circleguard.gateway.e2e;

import com.circleguard.gateway.service.QrValidationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.security.Key;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        })
@ActiveProfiles("test")
class GateEntryE2ETest {

    private static final String QR_SECRET = "my-qr-secret-key-for-tests-1234567890ab";

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        valueOps = Mockito.mock(ValueOperations.class);
        Mockito.when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void shouldAllowEntryForUserWithClearStatus() {
        UUID anonymousId = UUID.randomUUID();
        String token = buildToken(anonymousId);
        Mockito.when(valueOps.get("user:status:" + anonymousId)).thenReturn("CLEAR");

        ResponseEntity<QrValidationService.ValidationResult> response = restTemplate.postForEntity(
                "/api/v1/gate/validate",
                Map.of("token", token),
                QrValidationService.ValidationResult.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().valid());
        assertEquals("GREEN", response.getBody().status());
    }

    @Test
    void shouldDenyEntryForUserWithContagiedStatus() {
        UUID anonymousId = UUID.randomUUID();
        String token = buildToken(anonymousId);
        Mockito.when(valueOps.get("user:status:" + anonymousId)).thenReturn("CONTAGIED");

        ResponseEntity<QrValidationService.ValidationResult> response = restTemplate.postForEntity(
                "/api/v1/gate/validate",
                Map.of("token", token),
                QrValidationService.ValidationResult.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().valid());
        assertEquals("RED", response.getBody().status());
    }

    @Test
    void shouldDenyEntryForMalformedToken() {
        ResponseEntity<QrValidationService.ValidationResult> response = restTemplate.postForEntity(
                "/api/v1/gate/validate",
                Map.of("token", "this.is.not.a.jwt"),
                QrValidationService.ValidationResult.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().valid());
        assertEquals("RED", response.getBody().status());
    }

    @Test
    void shouldAllowEntryWhenRedisStatusIsNull() {
        UUID anonymousId = UUID.randomUUID();
        String token = buildToken(anonymousId);
        Mockito.when(valueOps.get("user:status:" + anonymousId)).thenReturn(null);

        ResponseEntity<QrValidationService.ValidationResult> response = restTemplate.postForEntity(
                "/api/v1/gate/validate",
                Map.of("token", token),
                QrValidationService.ValidationResult.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().valid());
        assertEquals("GREEN", response.getBody().status());
    }

    @Test
    void shouldDenyEntryForUserWithPotentialStatus() {
        UUID anonymousId = UUID.randomUUID();
        String token = buildToken(anonymousId);
        Mockito.when(valueOps.get("user:status:" + anonymousId)).thenReturn("POTENTIAL");

        ResponseEntity<QrValidationService.ValidationResult> response = restTemplate.postForEntity(
                "/api/v1/gate/validate",
                Map.of("token", token),
                QrValidationService.ValidationResult.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().valid());
        assertEquals("RED", response.getBody().status());
    }

    private String buildToken(UUID anonymousId) {
        Key key = Keys.hmacShaKeyFor(QR_SECRET.getBytes());
        return Jwts.builder()
                .setSubject(anonymousId.toString())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
