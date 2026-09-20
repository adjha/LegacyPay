package com.legacypay.payment;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentConcurrencyTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Test
    void concurrentDuplicateRequestsProcessOnlyOnce() throws Exception {
        BigDecimal senderBefore = balanceOf("A100");
        BigDecimal receiverBefore = balanceOf("A200");
        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Callable<PaymentResponse> paymentCall = () -> {
            startTogether.await();
            return paymentService.acceptPayment(
                    paymentRequest("A100", "A200", "500.00"), "concurrent-duplicate-key");
        };

        var first = executorService.submit(paymentCall);
        var second = executorService.submit(paymentCall);
        startTogether.countDown();

        List<PaymentResponse> responses = List.of(first.get(), second.get());
        executorService.shutdown();

        assertEquals(List.of("ACCEPTED", "ACCEPTED"),
                responses.stream().map(PaymentResponse::getStatus).toList());
        assertEquals(senderBefore.subtract(new BigDecimal("500.00")), balanceOf("A100"));
        assertEquals(receiverBefore.add(new BigDecimal("500.00")), balanceOf("A200"));
        assertEquals(1, paymentTransactionRepository.findAll().stream()
                .filter(transaction -> "concurrent-duplicate-key".equals(transaction.getIdempotencyKey()))
                .count());
    }

    private BigDecimal balanceOf(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).orElseThrow().getBalance();
    }

    private PaymentRequest paymentRequest(String senderAccount, String receiverAccount, String amount) {
        PaymentRequest request = new PaymentRequest();
        request.setSenderAccount(senderAccount);
        request.setReceiverAccount(receiverAccount);
        request.setAmount(new BigDecimal(amount));
        return request;
    }
}
