package com.legacypay.payment;

import java.util.Objects;

final class PaymentTransactionMatcher {

    private PaymentTransactionMatcher() {
    }

    static boolean hasSamePaymentDetails(PaymentTransaction transaction, PaymentRequest request) {
        return Objects.equals(transaction.getSenderAccount(), request.getSenderAccount())
                && Objects.equals(transaction.getReceiverAccount(), request.getReceiverAccount())
                && transaction.getAmount().compareTo(request.getAmount()) == 0;
    }

    static PaymentResponse responseFor(PaymentTransaction transaction) {
        if ("ACCEPTED".equals(transaction.getStatus())) {
            return new PaymentResponse("ACCEPTED", "Payment request received");
        }
        return new PaymentResponse("REJECTED", transaction.getReason());
    }
}
