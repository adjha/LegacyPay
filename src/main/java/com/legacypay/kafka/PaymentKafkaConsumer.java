package com.legacypay.kafka;

import com.legacypay.events.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "legacypay.kafka.enabled", havingValue = "true")
public class PaymentKafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentKafkaConsumer.class);

    @KafkaListener(topics = "${legacypay.kafka.payment-events-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(PaymentEvent event) {
        logger.info("Kafka payment event consumed paymentTransactionId={} status={}",
                event.paymentTransactionId(), event.status());
    }
}
