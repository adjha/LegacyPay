package com.legacypay.payment;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentStatusResponse {

    private final Long id;
    private final String senderAccount;
    private final String receiverAccount;
    private final BigDecimal amount;
    private final String status;
    private final String lifecycleStatus;
    private final String reason;
    private final Instant createdAt;

    public PaymentStatusResponse(PaymentTransaction transaction) {
        this.id = transaction.getId();
        this.senderAccount = transaction.getSenderAccount();
        this.receiverAccount = transaction.getReceiverAccount();
        this.amount = transaction.getAmount();
        this.status = transaction.getStatus();
        this.lifecycleStatus = transaction.getLifecycleStatus();
        this.reason = transaction.getReason();
        this.createdAt = transaction.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getSenderAccount() {
        return senderAccount;
    }

    public String getReceiverAccount() {
        return receiverAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getLifecycleStatus() {
        return lifecycleStatus;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
