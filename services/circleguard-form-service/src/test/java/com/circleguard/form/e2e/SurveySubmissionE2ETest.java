package com.circleguard.form.e2e;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.model.ValidationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"survey.submitted", "certificate.validated"})
@DirtiesContext
class SurveySubmissionE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldAcceptHealthSurveySubmissionAndReturnSavedEntity() {
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(UUID.randomUUID())
                .hasFever(false)
                .hasCough(false)
                .build();

        ResponseEntity<HealthSurvey> response = restTemplate.postForEntity(
                "/api/v1/surveys", survey, HealthSurvey.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(survey.getAnonymousId(), response.getBody().getAnonymousId());
    }

    @Test
    void shouldSetPendingValidationStatusWhenAttachmentIsProvided() {
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(UUID.randomUUID())
                .hasFever(true)
                .attachmentPath("/uploads/doctor-note.pdf")
                .build();

        ResponseEntity<HealthSurvey> response = restTemplate.postForEntity(
                "/api/v1/surveys", survey, HealthSurvey.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ValidationStatus.PENDING, response.getBody().getValidationStatus());
    }

    @Test
    void shouldNotSetValidationStatusWhenNoAttachment() {
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(UUID.randomUUID())
                .hasFever(false)
                .hasCough(true)
                .build();

        ResponseEntity<HealthSurvey> response = restTemplate.postForEntity(
                "/api/v1/surveys", survey, HealthSurvey.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getValidationStatus());
    }

    @Test
    void shouldPersistSurveyWithCorrectAnonymousId() {
        UUID expectedAnonymousId = UUID.randomUUID();
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(expectedAnonymousId)
                .hasFever(true)
                .hasCough(true)
                .otherSymptoms("headache")
                .build();

        ResponseEntity<HealthSurvey> response = restTemplate.postForEntity(
                "/api/v1/surveys", survey, HealthSurvey.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expectedAnonymousId, response.getBody().getAnonymousId());
        assertEquals("headache", response.getBody().getOtherSymptoms());
    }

    @Test
    void shouldAssignUniqueIdToEachSubmittedSurvey() {
        HealthSurvey survey1 = HealthSurvey.builder().anonymousId(UUID.randomUUID()).hasFever(false).build();
        HealthSurvey survey2 = HealthSurvey.builder().anonymousId(UUID.randomUUID()).hasFever(false).build();

        ResponseEntity<HealthSurvey> r1 = restTemplate.postForEntity("/api/v1/surveys", survey1, HealthSurvey.class);
        ResponseEntity<HealthSurvey> r2 = restTemplate.postForEntity("/api/v1/surveys", survey2, HealthSurvey.class);

        assertNotNull(r1.getBody());
        assertNotNull(r2.getBody());
        assertNotEquals(r1.getBody().getId(), r2.getBody().getId());
    }
}
