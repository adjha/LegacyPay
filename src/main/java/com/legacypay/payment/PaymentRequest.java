package com.legacypay.payment;

import java.math.BigDecimal;

import jakarta.validation.constraints.Positive;

public class PaymentRequest {

    private String senderAccount;
    private String receiverAccount;

    @Positive(message = "amount must be greater than zero")
    private BigDecimal amount;

    public String getSenderAccount() {
        return senderAccount;
    }

    public void setSenderAccount(String senderAccount) {
        this.senderAccount = senderAccount;
    }

    public String getReceiverAccount() {
        return receiverAccount;
    }

    public void setReceiverAccount(String receiverAccount) {
        this.receiverAccount = receiverAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
