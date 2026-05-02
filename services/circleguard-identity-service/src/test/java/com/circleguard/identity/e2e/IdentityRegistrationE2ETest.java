package com.circleguard.identity.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext
class IdentityRegistrationE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void shouldMapRealIdentityToAnonymousIdViaRestEndpoint() {
        Map<String, String> request = Map.of("realIdentity", "student@university.edu");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/identities/map", request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("anonymousId"));
        String anonymousId = (String) response.getBody().get("anonymousId");
        assertDoesNotThrow(() -> UUID.fromString(anonymousId));
    }

    @Test
    void shouldReturnSameAnonymousIdForSameIdentity() {
        Map<String, String> request = Map.of("realIdentity", "idempotent@university.edu");

        ResponseEntity<Map> first = restTemplate.postForEntity(
                "/api/v1/identities/map", request, Map.class);
        ResponseEntity<Map> second = restTemplate.postForEntity(
                "/api/v1/identities/map", request, Map.class);

        assertNotNull(first.getBody());
        assertNotNull(second.getBody());
        assertEquals(first.getBody().get("anonymousId"), second.getBody().get("anonymousId"));
    }

    @Test
    void shouldGenerateDifferentAnonymousIdsForDifferentIdentities() {
        ResponseEntity<Map> r1 = restTemplate.postForEntity(
                "/api/v1/identities/map",
                Map.of("realIdentity", "alice@university.edu"),
                Map.class);
        ResponseEntity<Map> r2 = restTemplate.postForEntity(
                "/api/v1/identities/map",
                Map.of("realIdentity", "bob@university.edu"),
                Map.class);

        assertNotNull(r1.getBody());
        assertNotNull(r2.getBody());
        assertNotEquals(r1.getBody().get("anonymousId"), r2.getBody().get("anonymousId"));
    }

    @Test
    void shouldRegisterVisitorAndReturnAnonymousId() {
        Map<String, String> visitorRequest = Map.of(
                "name", "John Visitor",
                "email", "visitor@external.com",
                "reason_for_visit", "Meeting with Professor"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/identities/visitor", visitorRequest, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        String anonymousId = (String) response.getBody().get("anonymousId");
        assertDoesNotThrow(() -> UUID.fromString(anonymousId));
    }

    @Test
    void shouldReturn401WhenAccessingProtectedLookupWithoutAuth() {
        UUID randomId = UUID.randomUUID();

        ResponseEntity<Void> response = restTemplate.getForEntity(
                "/api/v1/identities/lookup/" + randomId, Void.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
