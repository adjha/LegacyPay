package com.legacypay.payment;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_account", nullable = false)
    private String senderAccount;

    @Column(name = "receiver_account", nullable = false)
    private String receiverAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status;

    @Column(name = "lifecycle_status", nullable = false)
    private String lifecycleStatus;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentTransaction() {
    }

    public PaymentTransaction(String senderAccount, String receiverAccount, BigDecimal amount,
                              String status, String reason, String idempotencyKey) {
        this.senderAccount = senderAccount;
        this.receiverAccount = receiverAccount;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
        this.idempotencyKey = idempotencyKey;
        this.lifecycleStatus = "INITIATED";
        this.createdAt = Instant.now();
    }

    public static PaymentTransaction initiated(String senderAccount, String receiverAccount,
                                               BigDecimal amount, String idempotencyKey) {
        return new PaymentTransaction(senderAccount, receiverAccount, amount,
                "PENDING", null, idempotencyKey);
    }

    public String getSenderAccount() {
        return senderAccount;
    }

    public Long getId() {
        return id;
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markProcessing() {
        lifecycleStatus = "PROCESSING";
    }

    public void markCompleted() {
        status = "ACCEPTED";
        reason = null;
        lifecycleStatus = "COMPLETED";
    }

    public void markFailed(String reason) {
        status = "REJECTED";
        this.reason = reason;
        lifecycleStatus = "FAILED";
    }
}
