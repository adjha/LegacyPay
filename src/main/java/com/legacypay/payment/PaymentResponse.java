package com.legacypay.payment;

public class PaymentResponse {

    private final String status;
    private final String message;

    public PaymentResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
