package com.circleguard.form.integration;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.model.ValidationStatus;
import com.circleguard.form.service.HealthSurveyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
class SurveyKafkaIntegrationTest {

    @Autowired
    private HealthSurveyService healthSurveyService;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void shouldPublishSurveySubmittedEventToKafkaWhenSurveyIsSubmitted() {
        UUID anonymousId = UUID.randomUUID();
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(anonymousId)
                .hasFever(true)
                .hasCough(false)
                .build();

        healthSurveyService.submitSurvey(survey);

        verify(kafkaTemplate).send(eq("survey.submitted"), eq(anonymousId.toString()), any());
    }

    @Test
    void shouldPublishCertificateValidatedEventWhenSurveyIsApproved() {
        UUID anonymousId = UUID.randomUUID();
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(anonymousId)
                .hasFever(false)
                .hasCough(false)
                .attachmentPath("/uploads/cert.pdf")
                .build();

        HealthSurvey saved = healthSurveyService.submitSurvey(survey);
        assertThat(saved.getValidationStatus()).isEqualTo(ValidationStatus.PENDING);

        UUID adminId = UUID.randomUUID();
        healthSurveyService.validateSurvey(saved.getId(), ValidationStatus.APPROVED, adminId);

        verify(kafkaTemplate).send(eq("certificate.validated"), eq(anonymousId.toString()), any());
    }

    @Test
    void shouldNotPublishCertificateEventWhenSurveyIsRejected() {
        UUID anonymousId = UUID.randomUUID();
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(anonymousId)
                .hasFever(false)
                .attachmentPath("/uploads/cert.pdf")
                .build();

        HealthSurvey saved = healthSurveyService.submitSurvey(survey);

        UUID adminId = UUID.randomUUID();
        healthSurveyService.validateSurvey(saved.getId(), ValidationStatus.REJECTED, adminId);

        verify(kafkaTemplate, org.mockito.Mockito.never())
                .send(eq("certificate.validated"), any(), any());
    }
}
