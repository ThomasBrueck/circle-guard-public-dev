package com.circleguard.form.integration;

import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.repository.QuestionnaireRepository;
import com.circleguard.form.service.QuestionnaireService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(QuestionnaireService.class)
class QuestionnaireServiceIntegrationTest {

    @Autowired
    private QuestionnaireService questionnaireService;

    @Autowired
    private QuestionnaireRepository repository;

    @Test
    void shouldPersistAndRetrieveQuestionnaireViaServiceLayer() {
        Questionnaire q = Questionnaire.builder()
                .title("Health Check v1")
                .description("Daily health questionnaire")
                .version(1)
                .isActive(false)
                .build();

        Questionnaire saved = questionnaireService.saveQuestionnaire(q);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals("Health Check v1", saved.getTitle());
    }

    @Test
    void shouldReturnActiveQuestionnaireAfterActivation() {
        Questionnaire q1 = questionnaireService.saveQuestionnaire(
                Questionnaire.builder().title("v1").version(1).isActive(false).build());
        Questionnaire q2 = questionnaireService.saveQuestionnaire(
                Questionnaire.builder().title("v2").version(2).isActive(false).build());

        questionnaireService.activateQuestionnaire(q2.getId());

        Optional<Questionnaire> active = questionnaireService.getActiveQuestionnaire();
        assertTrue(active.isPresent());
        assertEquals(q2.getId(), active.get().getId());
    }

    @Test
    void shouldDeactivatePreviousQuestionnaireWhenNewOneIsActivated() {
        Questionnaire first = questionnaireService.saveQuestionnaire(
                Questionnaire.builder().title("first").version(1).isActive(false).build());
        questionnaireService.activateQuestionnaire(first.getId());

        Questionnaire second = questionnaireService.saveQuestionnaire(
                Questionnaire.builder().title("second").version(2).isActive(false).build());
        questionnaireService.activateQuestionnaire(second.getId());

        Questionnaire reloaded = repository.findById(first.getId()).orElseThrow();
        assertFalse(reloaded.getIsActive());
    }

    @Test
    void shouldReturnAllSavedQuestionnaires() {
        questionnaireService.saveQuestionnaire(
                Questionnaire.builder().title("A").version(1).isActive(false).build());
        questionnaireService.saveQuestionnaire(
                Questionnaire.builder().title("B").version(2).isActive(false).build());

        List<Questionnaire> all = questionnaireService.getAllQuestionnaires();
        assertEquals(2, all.size());
    }

    @Test
    void shouldReturnEmptyOptionalWhenNoActiveQuestionnaire() {
        questionnaireService.saveQuestionnaire(
                Questionnaire.builder().title("Inactive").version(1).isActive(false).build());

        Optional<Questionnaire> active = questionnaireService.getActiveQuestionnaire();
        assertFalse(active.isPresent());
    }
}
