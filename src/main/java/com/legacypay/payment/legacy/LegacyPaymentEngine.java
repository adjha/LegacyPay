package com.legacypay.payment.legacy;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class LegacyPaymentEngine {

    private static final BigDecimal SIMULATED_BALANCE_LIMIT = new BigDecimal("10000");

    public LegacyPaymentResult processPayment(String senderAccount, String receiverAccount, BigDecimal amount) {
        if (isBlank(senderAccount) || isBlank(receiverAccount)) {
            return new LegacyPaymentResult(
                    LegacyPaymentResult.INVALID_ACCOUNT_CODE,
                    "INVALID_ACCOUNT");
        }

        if (amount.compareTo(SIMULATED_BALANCE_LIMIT) > 0) {
            return new LegacyPaymentResult(
                    LegacyPaymentResult.INSUFFICIENT_FUNDS_CODE,
                    "INSUFFICIENT_FUNDS");
        }

        return new LegacyPaymentResult(LegacyPaymentResult.SUCCESS_CODE, "SUCCESS");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
