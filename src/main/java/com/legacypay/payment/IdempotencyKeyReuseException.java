package com.legacypay.payment;

public class IdempotencyKeyReuseException extends RuntimeException {

    public IdempotencyKeyReuseException() {
        super("Idempotency key has already been used for different payment details");
    }
}
