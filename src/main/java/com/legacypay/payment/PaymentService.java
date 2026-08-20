package com.legacypay.payment;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final AccountRepository accountRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public PaymentService(AccountRepository accountRepository,
                          PaymentTransactionRepository paymentTransactionRepository) {
        this.accountRepository = accountRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @Transactional
    public PaymentResponse acceptPayment(PaymentRequest request, String idempotencyKey) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        PaymentTransaction existingTransaction = paymentTransactionRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (existingTransaction != null) {
            if (!hasSamePaymentDetails(existingTransaction, request)) {
                throw new IdempotencyKeyReuseException();
            }
            return responseFor(existingTransaction);
        }

        Account sender = accountRepository.findByAccountNumber(request.getSenderAccount()).orElse(null);
        Account receiver = accountRepository.findByAccountNumber(request.getReceiverAccount()).orElse(null);

        if (sender == null || receiver == null) {
            paymentTransactionRepository.save(new PaymentTransaction(
                    request.getSenderAccount(), request.getReceiverAccount(), request.getAmount(),
                    "REJECTED", "INVALID_ACCOUNT", idempotencyKey));
            return new PaymentResponse("REJECTED", "INVALID_ACCOUNT");
        }

        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            paymentTransactionRepository.save(new PaymentTransaction(
                    request.getSenderAccount(), request.getReceiverAccount(), request.getAmount(),
                    "REJECTED", "INSUFFICIENT_FUNDS", idempotencyKey));
            return new PaymentResponse("REJECTED", "INSUFFICIENT_FUNDS");
        }

        sender.debit(request.getAmount());
        receiver.credit(request.getAmount());
        accountRepository.save(sender);
        accountRepository.save(receiver);
        paymentTransactionRepository.save(new PaymentTransaction(
                request.getSenderAccount(), request.getReceiverAccount(), request.getAmount(),
                "ACCEPTED", null, idempotencyKey));

        return new PaymentResponse("ACCEPTED", "Payment request received");
    }

    private boolean hasSamePaymentDetails(PaymentTransaction transaction, PaymentRequest request) {
        return Objects.equals(transaction.getSenderAccount(), request.getSenderAccount())
                && Objects.equals(transaction.getReceiverAccount(), request.getReceiverAccount())
                && transaction.getAmount().compareTo(request.getAmount()) == 0;
    }

    private PaymentResponse responseFor(PaymentTransaction transaction) {
        if ("ACCEPTED".equals(transaction.getStatus())) {
            return new PaymentResponse("ACCEPTED", "Payment request received");
        }
        return new PaymentResponse("REJECTED", transaction.getReason());
    }
}
