package com.legacypay.payment.legacy;

import java.math.BigDecimal;

import com.legacypay.payment.PaymentRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyPaymentAdapterTest {

    private final LegacyPaymentAdapter adapter = new LegacyPaymentAdapter(new LegacyPaymentEngine());

    @Test
    void processReturnsSuccessCodeForValidPayment() {
        LegacyPaymentResult result = adapter.process(paymentRequest("A100", "A200", "500"));

        assertEquals("00", result.getResultCode());
        assertEquals("SUCCESS", result.getDescription());
    }

    @Test
    void processReturnsInsufficientFundsCodeForAmountAboveSimulatedLimit() {
        LegacyPaymentResult result = adapter.process(paymentRequest("A100", "A200", "10001"));

        assertEquals("51", result.getResultCode());
        assertEquals("INSUFFICIENT_FUNDS", result.getDescription());
    }

    @Test
    void processReturnsInvalidAccountCodeForBlankSenderAccount() {
        LegacyPaymentResult result = adapter.process(paymentRequest(" ", "A200", "500"));

        assertEquals("14", result.getResultCode());
        assertEquals("INVALID_ACCOUNT", result.getDescription());
    }

    private PaymentRequest paymentRequest(String senderAccount, String receiverAccount, String amount) {
        PaymentRequest request = new PaymentRequest();
        request.setSenderAccount(senderAccount);
        request.setReceiverAccount(receiverAccount);
        request.setAmount(new BigDecimal(amount));
        return request;
    }
}
