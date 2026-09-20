package com.legacypay.payment;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Long paymentId) {
        super("Payment transaction not found: " + paymentId);
    }
}
