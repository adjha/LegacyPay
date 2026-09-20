package com.legacypay.payment;

import java.math.BigDecimal;

import com.legacypay.audit.AuditLog;
import com.legacypay.audit.AuditLogRepository;
import com.legacypay.events.PaymentEvent;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@Transactional
@RecordApplicationEvents
class PaymentServiceTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private EntityManager entityManager;

    @Test
    void successfulTransferPersistsBalancesThroughRepository() {
        PaymentResponse response = paymentService.acceptPayment(
                paymentRequest("A100", "A200", "500.00"), "service-success-key");
        entityManager.flush();
        entityManager.clear();

        assertEquals("ACCEPTED", response.getStatus());
        assertEquals(new BigDecimal("4500.00"), balanceOf("A100"));
        assertEquals(new BigDecimal("1500.00"), balanceOf("A200"));

        PaymentTransaction transaction = savedTransaction();
        assertEquals("A100", transaction.getSenderAccount());
        assertEquals("A200", transaction.getReceiverAccount());
        assertEquals(new BigDecimal("500.00"), transaction.getAmount());
        assertEquals("ACCEPTED", transaction.getStatus());
        assertEquals("COMPLETED", transaction.getLifecycleStatus());
        assertNull(transaction.getReason());
        assertNotNull(transaction.getCreatedAt());
        assertEquals("service-success-key", transaction.getIdempotencyKey());
        assertAuditEvents(transaction.getId(), "PAYMENT_INITIATED", "PAYMENT_ACCEPTED");
    }

    @Test
    void insufficientFundsRejectsTransferWithoutChangingBalances() {
        PaymentResponse response = paymentService.acceptPayment(
                paymentRequest("A100", "A200", "6000.00"), "service-insufficient-funds-key");
        entityManager.flush();
        entityManager.clear();

        assertEquals("REJECTED", response.getStatus());
        assertEquals("INSUFFICIENT_FUNDS", response.getMessage());
        assertEquals(new BigDecimal("5000.00"), balanceOf("A100"));
        assertEquals(new BigDecimal("1000.00"), balanceOf("A200"));

        PaymentTransaction transaction = savedTransaction();
        assertEquals("REJECTED", transaction.getStatus());
        assertEquals("FAILED", transaction.getLifecycleStatus());
        assertEquals("INSUFFICIENT_FUNDS", transaction.getReason());
        assertAuditEvents(transaction.getId(), "PAYMENT_INITIATED", "PAYMENT_REJECTED");
    }

    @Test
    void missingAccountRejectsTransfer() {
        PaymentResponse response = paymentService.acceptPayment(
                paymentRequest("A999", "A200", "500.00"), "service-invalid-account-key");

        assertEquals("REJECTED", response.getStatus());
        assertEquals("INVALID_ACCOUNT", response.getMessage());
        assertEquals(new BigDecimal("1000.00"), balanceOf("A200"));

        PaymentTransaction transaction = savedTransaction();
        assertEquals("REJECTED", transaction.getStatus());
        assertEquals("FAILED", transaction.getLifecycleStatus());
        assertEquals("INVALID_ACCOUNT", transaction.getReason());
        assertAuditEvents(transaction.getId(), "PAYMENT_INITIATED", "PAYMENT_REJECTED");
    }

    @Test
    void paymentStatusLookupReturnsPersistedLifecycle() {
        paymentService.acceptPayment(
                paymentRequest("A100", "A200", "500.00"), "service-status-lookup-key");

        PaymentTransaction transaction = savedTransaction();
        PaymentStatusResponse statusResponse = paymentService.getPaymentStatus(transaction.getId());

        assertEquals(transaction.getId(), statusResponse.getId());
        assertEquals("ACCEPTED", statusResponse.getStatus());
        assertEquals("COMPLETED", statusResponse.getLifecycleStatus());
        assertEquals("A100", statusResponse.getSenderAccount());
        assertEquals("A200", statusResponse.getReceiverAccount());
    }

    @Test
    void successfulTransferIncrementsAcceptedMetric() {
        double before = paymentMetricCount("accepted");

        paymentService.acceptPayment(
                paymentRequest("A100", "A200", "500.00"), "service-metric-key");

        assertEquals(before + 1.0, paymentMetricCount("accepted"));
    }

    @Test
    void successfulTransferPublishesPaymentEvent() {
        paymentService.acceptPayment(
                paymentRequest("A100", "A200", "500.00"), "service-domain-event-key");

        PaymentEvent event = applicationEvents.stream(PaymentEvent.class)
                .reduce((first, second) -> second)
                .orElseThrow();

        assertEquals("ACCEPTED", event.status());
        assertEquals("COMPLETED", event.lifecycleStatus());
    }

    private BigDecimal balanceOf(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).orElseThrow().getBalance();
    }

    private PaymentTransaction savedTransaction() {
        assertEquals(1, paymentTransactionRepository.count());
        return paymentTransactionRepository.findAll().getFirst();
    }

    private PaymentRequest paymentRequest(String senderAccount, String receiverAccount, String amount) {
        PaymentRequest request = new PaymentRequest();
        request.setSenderAccount(senderAccount);
        request.setReceiverAccount(receiverAccount);
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private double paymentMetricCount(String outcome) {
        return meterRegistry.counter("legacypay.payments", "outcome", outcome).count();
    }

    private void assertAuditEvents(Long paymentId, String... expectedEventTypes) {
        assertEquals(
                java.util.List.of(expectedEventTypes),
                auditLogRepository.findByPaymentTransactionIdOrderByCreatedAtAsc(paymentId)
                        .stream()
                        .map(AuditLog::getEventType)
                        .toList());
    }
}
