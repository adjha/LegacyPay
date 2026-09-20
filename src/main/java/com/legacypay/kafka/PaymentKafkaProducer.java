package com.legacypay.kafka;

import com.legacypay.events.PaymentEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "legacypay.kafka.enabled", havingValue = "true")
public class PaymentKafkaProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final String topicName;

    public PaymentKafkaProducer(KafkaTemplate<String, PaymentEvent> kafkaTemplate,
                                @Value("${legacypay.kafka.payment-events-topic}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    @EventListener
    public void publish(PaymentEvent event) {
        kafkaTemplate.send(topicName, String.valueOf(event.paymentTransactionId()), event);
    }
}
