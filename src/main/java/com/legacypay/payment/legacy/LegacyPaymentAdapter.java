package com.legacypay.payment.legacy;

import com.legacypay.payment.PaymentRequest;
import org.springframework.stereotype.Component;

@Component
public class LegacyPaymentAdapter {

    private final LegacyPaymentEngine legacyPaymentEngine;

    public LegacyPaymentAdapter(LegacyPaymentEngine legacyPaymentEngine) {
        this.legacyPaymentEngine = legacyPaymentEngine;
    }

    public LegacyPaymentResult process(PaymentRequest request) {
        return legacyPaymentEngine.processPayment(
                request.getSenderAccount(),
                request.getReceiverAccount(),
                request.getAmount());
    }
}
