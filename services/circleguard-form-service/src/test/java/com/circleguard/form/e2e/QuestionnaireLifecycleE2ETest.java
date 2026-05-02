package com.circleguard.form.e2e;

import com.circleguard.form.model.Questionnaire;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"survey.submitted", "certificate.validated"})
@DirtiesContext
class QuestionnaireLifecycleE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateQuestionnaireViaRestAndRetrieveIt() {
        Questionnaire payload = Questionnaire.builder()
                .title("Campus Entry Check")
                .description("Daily symptom questionnaire for campus access")
                .version(1)
                .isActive(false)
                .build();

        ResponseEntity<Questionnaire> created = restTemplate.postForEntity(
                "/api/v1/questionnaires", payload, Questionnaire.class);

        assertEquals(HttpStatus.OK, created.getStatusCode());
        assertNotNull(created.getBody());
        assertNotNull(created.getBody().getId());
        assertEquals("Campus Entry Check", created.getBody().getTitle());
    }

    @Test
    void shouldReturnNotFoundWhenNoActiveQuestionnaire() {
        ResponseEntity<Void> response = restTemplate.getForEntity(
                "/api/v1/questionnaires/active", Void.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldActivateQuestionnaireAndReturnItAsActive() {
        Questionnaire payload = Questionnaire.builder()
                .title("Activate Test Q")
                .version(1)
                .isActive(false)
                .build();

        ResponseEntity<Questionnaire> created = restTemplate.postForEntity(
                "/api/v1/questionnaires", payload, Questionnaire.class);
        assertNotNull(created.getBody());

        restTemplate.postForEntity(
                "/api/v1/questionnaires/" + created.getBody().getId() + "/activate",
                null, Void.class);

        ResponseEntity<Questionnaire> active = restTemplate.getForEntity(
                "/api/v1/questionnaires/active", Questionnaire.class);

        assertEquals(HttpStatus.OK, active.getStatusCode());
        assertNotNull(active.getBody());
        assertEquals(created.getBody().getId(), active.getBody().getId());
        assertTrue(active.getBody().getIsActive());
    }

    @Test
    void shouldListAllQuestionnaires() {
        Questionnaire q1 = Questionnaire.builder().title("Q1").version(1).isActive(false).build();
        Questionnaire q2 = Questionnaire.builder().title("Q2").version(2).isActive(false).build();

        restTemplate.postForEntity("/api/v1/questionnaires", q1, Questionnaire.class);
        restTemplate.postForEntity("/api/v1/questionnaires", q2, Questionnaire.class);

        ResponseEntity<Questionnaire[]> all = restTemplate.getForEntity(
                "/api/v1/questionnaires", Questionnaire[].class);

        assertEquals(HttpStatus.OK, all.getStatusCode());
        assertNotNull(all.getBody());
        assertTrue(all.getBody().length >= 2);
    }

    @Test
    void shouldReturnTimestampsOnCreatedQuestionnaire() {
        Questionnaire payload = Questionnaire.builder()
                .title("Timestamped Q")
                .version(1)
                .isActive(false)
                .build();

        ResponseEntity<Questionnaire> created = restTemplate.postForEntity(
                "/api/v1/questionnaires", payload, Questionnaire.class);

        assertNotNull(created.getBody());
        assertNotNull(created.getBody().getCreatedAt());
        assertNotNull(created.getBody().getUpdatedAt());
    }
}
