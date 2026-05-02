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
class CertificateValidationE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldSubmitSurveyWithAttachmentAndReceivePendingStatus() {
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(UUID.randomUUID())
                .hasFever(true)
                .hasCough(true)
                .attachmentPath("/uploads/2024/medical-certificate-001.pdf")
                .build();

        ResponseEntity<HealthSurvey> response = restTemplate.postForEntity(
                "/api/v1/surveys", survey, HealthSurvey.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ValidationStatus.PENDING, response.getBody().getValidationStatus());
        assertEquals("/uploads/2024/medical-certificate-001.pdf", response.getBody().getAttachmentPath());
    }

    @Test
    void shouldNotAssignPendingStatusWhenNoAttachmentProvided() {
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(UUID.randomUUID())
                .hasFever(false)
                .hasCough(false)
                .build();

        ResponseEntity<HealthSurvey> response = restTemplate.postForEntity(
                "/api/v1/surveys", survey, HealthSurvey.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getValidationStatus());
    }

    @Test
    void shouldAssignPersistentIdToSurveyWithCertificate() {
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(UUID.randomUUID())
                .hasFever(true)
                .attachmentPath("/uploads/cert.pdf")
                .build();

        ResponseEntity<HealthSurvey> response = restTemplate.postForEntity(
                "/api/v1/surveys", survey, HealthSurvey.class);

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertDoesNotThrow(() -> response.getBody().getId().toString());
    }

    @Test
    void shouldPreserveSymptomDataAlongsideCertificate() {
        UUID anonymousId = UUID.randomUUID();
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(anonymousId)
                .hasFever(true)
                .hasCough(true)
                .otherSymptoms("sore throat, fatigue")
                .attachmentPath("/uploads/doctor-note.pdf")
                .build();

        ResponseEntity<HealthSurvey> response = restTemplate.postForEntity(
                "/api/v1/surveys", survey, HealthSurvey.class);

        assertNotNull(response.getBody());
        assertEquals(anonymousId, response.getBody().getAnonymousId());
        assertEquals("sore throat, fatigue", response.getBody().getOtherSymptoms());
        assertTrue(response.getBody().getHasFever());
        assertEquals(ValidationStatus.PENDING, response.getBody().getValidationStatus());
    }

    @Test
    void shouldHandleMultiplePendingCertificatesForDifferentUsers() {
        HealthSurvey s1 = HealthSurvey.builder()
                .anonymousId(UUID.randomUUID()).hasFever(true).attachmentPath("/uploads/cert1.pdf").build();
        HealthSurvey s2 = HealthSurvey.builder()
                .anonymousId(UUID.randomUUID()).hasFever(false).attachmentPath("/uploads/cert2.pdf").build();

        ResponseEntity<HealthSurvey> r1 = restTemplate.postForEntity("/api/v1/surveys", s1, HealthSurvey.class);
        ResponseEntity<HealthSurvey> r2 = restTemplate.postForEntity("/api/v1/surveys", s2, HealthSurvey.class);

        assertNotNull(r1.getBody());
        assertNotNull(r2.getBody());
        assertEquals(ValidationStatus.PENDING, r1.getBody().getValidationStatus());
        assertEquals(ValidationStatus.PENDING, r2.getBody().getValidationStatus());
        assertNotEquals(r1.getBody().getId(), r2.getBody().getId());
    }
}
