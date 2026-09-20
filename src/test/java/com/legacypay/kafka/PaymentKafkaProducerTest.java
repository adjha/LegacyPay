package com.legacypay.kafka;

import com.legacypay.events.PaymentEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentKafkaProducerTest {

    @Test
    void publishSendsPaymentEventToConfiguredTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, PaymentEvent> kafkaTemplate = mock(KafkaTemplate.class);
        PaymentKafkaProducer producer = new PaymentKafkaProducer(kafkaTemplate, "test-payment-events");
        PaymentEvent event = new PaymentEvent(42L, "ACCEPTED", "COMPLETED", null);

        producer.publish(event);

        verify(kafkaTemplate).send("test-payment-events", "42", event);
    }
}
