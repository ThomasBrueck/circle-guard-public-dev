package com.circleguard.form.integration;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.model.ValidationStatus;
import com.circleguard.form.service.HealthSurveyService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"survey.submitted", "certificate.validated"})
@DirtiesContext
class SurveyKafkaIntegrationTest {

    @Autowired
    private HealthSurveyService healthSurveyService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void shouldPublishSurveySubmittedEventToKafkaWhenSurveyIsSubmitted() {
        UUID anonymousId = UUID.randomUUID();
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(anonymousId)
                .hasFever(true)
                .hasCough(false)
                .build();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-survey-consumer", "true", embeddedKafka);
        ConsumerFactory<String, Object> cf = new org.springframework.kafka.core.DefaultKafkaConsumerFactory<>(consumerProps);

        try (var consumer = cf.createConsumer()) {
            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "survey.submitted");

            healthSurveyService.submitSurvey(survey);

            ConsumerRecord<String, Object> record = KafkaTestUtils.getSingleRecord(consumer, "survey.submitted");
            assertThat(record).isNotNull();
            assertThat(record.key()).isEqualTo(anonymousId.toString());
        }
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

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-cert-consumer", "true", embeddedKafka);
        ConsumerFactory<String, Object> cf = new org.springframework.kafka.core.DefaultKafkaConsumerFactory<>(consumerProps);

        try (var consumer = cf.createConsumer()) {
            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "certificate.validated");

            UUID adminId = UUID.randomUUID();
            healthSurveyService.validateSurvey(saved.getId(), ValidationStatus.APPROVED, adminId);

            ConsumerRecord<String, Object> record = KafkaTestUtils.getSingleRecord(consumer, "certificate.validated");
            assertThat(record).isNotNull();
            assertThat(record.key()).isEqualTo(anonymousId.toString());
        }
    }
}
