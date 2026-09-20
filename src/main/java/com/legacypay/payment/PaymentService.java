package com.legacypay.payment;

import java.util.concurrent.locks.ReentrantLock;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final IdempotencyLockRegistry idempotencyLockRegistry;
    private final PaymentProcessor paymentProcessor;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public PaymentService(IdempotencyLockRegistry idempotencyLockRegistry,
                          PaymentProcessor paymentProcessor,
                          PaymentTransactionRepository paymentTransactionRepository) {
        this.idempotencyLockRegistry = idempotencyLockRegistry;
        this.paymentProcessor = paymentProcessor;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    public PaymentResponse acceptPayment(PaymentRequest request, String idempotencyKey) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        ReentrantLock lock = idempotencyLockRegistry.lockFor(idempotencyKey);
        lock.lock();
        try {
            return paymentProcessor.process(request, idempotencyKey);
        } catch (DataIntegrityViolationException exception) {
            PaymentTransaction existingTransaction = waitForExistingTransaction(idempotencyKey);
            if (!PaymentTransactionMatcher.hasSamePaymentDetails(existingTransaction, request)) {
                throw new IdempotencyKeyReuseException();
            }
            return PaymentTransactionMatcher.responseFor(existingTransaction);
        } finally {
            lock.unlock();
        }
    }

    public PaymentStatusResponse getPaymentStatus(Long paymentId) {
        return paymentTransactionRepository.findById(paymentId)
                .map(PaymentStatusResponse::new)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private PaymentTransaction waitForExistingTransaction(String idempotencyKey) {
        for (int attempt = 0; attempt < 5; attempt++) {
            PaymentTransaction transaction = paymentTransactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElse(null);
            if (transaction != null) {
                return transaction;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new IllegalStateException("Could not find payment transaction for duplicate idempotency key");
    }
}
