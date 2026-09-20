package com.legacypay.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventListener.class);

    @EventListener
    public void handle(PaymentEvent event) {
        logger.info("Payment event received paymentTransactionId={} status={} lifecycleStatus={}",
                event.paymentTransactionId(), event.status(), event.lifecycleStatus());
    }
}
