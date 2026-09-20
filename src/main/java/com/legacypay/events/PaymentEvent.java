package com.legacypay.events;

public record PaymentEvent(Long paymentTransactionId, String status, String lifecycleStatus, String reason) {
}
