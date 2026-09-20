package com.legacypay.audit;

import java.time.Instant;

public class AuditLogResponse {

    private final Long id;
    private final String eventType;
    private final Long paymentTransactionId;
    private final String message;
    private final Instant createdAt;

    public AuditLogResponse(AuditLog auditLog) {
        this.id = auditLog.getId();
        this.eventType = auditLog.getEventType();
        this.paymentTransactionId = auditLog.getPaymentTransactionId();
        this.message = auditLog.getMessage();
        this.createdAt = auditLog.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
