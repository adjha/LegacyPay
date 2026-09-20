package com.legacypay.payment;

import com.legacypay.audit.AuditService;
import com.legacypay.events.PaymentEvent;
import com.legacypay.events.PaymentEventPublisher;
import com.legacypay.observability.PaymentMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessor.class);

    private final AccountRepository accountRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AuditService auditService;
    private final PaymentMetrics paymentMetrics;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentProcessor(AccountRepository accountRepository,
                            PaymentTransactionRepository paymentTransactionRepository,
                            AuditService auditService,
                            PaymentMetrics paymentMetrics,
                            PaymentEventPublisher paymentEventPublisher) {
        this.accountRepository = accountRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.auditService = auditService;
        this.paymentMetrics = paymentMetrics;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @Transactional(noRollbackFor = IdempotencyKeyReuseException.class)
    public PaymentResponse process(PaymentRequest request, String idempotencyKey) {
        PaymentTransaction existingTransaction = paymentTransactionRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (existingTransaction != null) {
            if (!PaymentTransactionMatcher.hasSamePaymentDetails(existingTransaction, request)) {
                auditService.record("IDEMPOTENCY_CONFLICT", existingTransaction.getId(),
                        "Idempotency key was reused with different payment details");
                paymentMetrics.recordOutcome("idempotency_conflict");
                logger.warn("Idempotency conflict for paymentTransactionId={}", existingTransaction.getId());
                throw new IdempotencyKeyReuseException();
            }
            auditService.record("IDEMPOTENCY_REPLAY", existingTransaction.getId(),
                    "Duplicate request returned the original payment result");
            paymentMetrics.recordOutcome("idempotency_replay");
            publishEvent(existingTransaction);
            return PaymentTransactionMatcher.responseFor(existingTransaction);
        }

        PaymentTransaction transaction = PaymentTransaction.initiated(
                request.getSenderAccount(), request.getReceiverAccount(), request.getAmount(), idempotencyKey);
        paymentTransactionRepository.saveAndFlush(transaction);
        auditService.record("PAYMENT_INITIATED", transaction.getId(), "Payment request was received");
        transaction.markProcessing();

        Account sender = accountRepository.findByAccountNumber(request.getSenderAccount()).orElse(null);
        Account receiver = accountRepository.findByAccountNumber(request.getReceiverAccount()).orElse(null);

        if (sender == null || receiver == null) {
            transaction.markFailed("INVALID_ACCOUNT");
            paymentTransactionRepository.save(transaction);
            auditService.record("PAYMENT_REJECTED", transaction.getId(), "Payment was rejected: INVALID_ACCOUNT");
            paymentMetrics.recordOutcome("rejected_invalid_account");
            logger.info("Payment rejected paymentTransactionId={} reason=INVALID_ACCOUNT", transaction.getId());
            publishEvent(transaction);
            return new PaymentResponse("REJECTED", "INVALID_ACCOUNT");
        }

        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            transaction.markFailed("INSUFFICIENT_FUNDS");
            paymentTransactionRepository.save(transaction);
            auditService.record("PAYMENT_REJECTED", transaction.getId(), "Payment was rejected: INSUFFICIENT_FUNDS");
            paymentMetrics.recordOutcome("rejected_insufficient_funds");
            logger.info("Payment rejected paymentTransactionId={} reason=INSUFFICIENT_FUNDS", transaction.getId());
            publishEvent(transaction);
            return new PaymentResponse("REJECTED", "INSUFFICIENT_FUNDS");
        }

        sender.debit(request.getAmount());
        receiver.credit(request.getAmount());
        accountRepository.save(sender);
        accountRepository.save(receiver);

        transaction.markCompleted();
        paymentTransactionRepository.save(transaction);
        auditService.record("PAYMENT_ACCEPTED", transaction.getId(), "Payment completed successfully");
        paymentMetrics.recordOutcome("accepted");
        logger.info("Payment accepted paymentTransactionId={}", transaction.getId());
        publishEvent(transaction);

        return new PaymentResponse("ACCEPTED", "Payment request received");
    }

    private void publishEvent(PaymentTransaction transaction) {
        paymentEventPublisher.publish(new PaymentEvent(
                transaction.getId(),
                transaction.getStatus(),
                transaction.getLifecycleStatus(),
                transaction.getReason()));
    }
}
