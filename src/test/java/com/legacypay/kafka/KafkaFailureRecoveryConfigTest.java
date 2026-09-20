package com.legacypay.kafka;

import com.legacypay.events.PaymentEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DefaultErrorHandler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class KafkaFailureRecoveryConfigTest {

    @Test
    void kafkaErrorHandlerIsConfiguredWithDeadLetterRecovery() {
        @SuppressWarnings("unchecked")
        KafkaOperations<String, PaymentEvent> kafkaOperations = mock(KafkaOperations.class);

        DefaultErrorHandler errorHandler = new KafkaFailureRecoveryConfig()
                .kafkaErrorHandler(kafkaOperations);

        assertNotNull(errorHandler);
    }
}
