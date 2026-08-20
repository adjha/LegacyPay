package com.legacypay.payment;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Test
    void createPaymentReturnsAcceptedResponse() throws Exception {
        String requestBody = """
                {
                  "senderAccount": "A100",
                  "receiverAccount": "A200",
                  "amount": 500
                }
                """;

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "controller-success-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.message").value("Payment request received"));
    }

    @Test
    void createPaymentRejectsZeroAmount() throws Exception {
        String requestBody = """
                {
                  "senderAccount": "A100",
                  "receiverAccount": "A200",
                  "amount": 0
                }
                """;

        mockMvc.perform(post("/api/payments")
                .header("Idempotency-Key", "controller-zero-amount-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("amount must be greater than zero"));
    }

    @Test
    void createPaymentRejectsNegativeAmountWithConsistentErrorResponse() throws Exception {
        String requestBody = """
                {
                  "senderAccount": "A100",
                  "receiverAccount": "A200",
                  "amount": -50
                }
                """;

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "controller-negative-amount-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("amount must be greater than zero"));
    }

    @Test
    void createPaymentRejectsAmountGreaterThanSenderBalanceWithoutChangingBalances() throws Exception {
        BigDecimal senderBalanceBefore = balanceOf("A100");
        BigDecimal receiverBalanceBefore = balanceOf("A200");

        String requestBody = """
                {
                  "senderAccount": "A100",
                  "receiverAccount": "A200",
                  "amount": 10000
                }
                """;

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "controller-insufficient-funds-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.message").value("INSUFFICIENT_FUNDS"));

        assertEquals(senderBalanceBefore, balanceOf("A100"));
        assertEquals(receiverBalanceBefore, balanceOf("A200"));
    }

    @Test
    void duplicateRequestWithSameKeyReturnsOriginalResultWithoutSecondTransfer() throws Exception {
        String requestBody = """
                {
                  "senderAccount": "A100",
                  "receiverAccount": "A200",
                  "amount": 500
                }
                """;

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "duplicate-payment-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "duplicate-payment-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        assertEquals(new BigDecimal("4500.00"), balanceOf("A100"));
        assertEquals(new BigDecimal("1500.00"), balanceOf("A200"));
        assertEquals(1, paymentTransactionRepository.count());
    }

    @Test
    void reusedKeyWithDifferentPaymentDetailsReturnsConflict() throws Exception {
        String firstRequestBody = """
                {
                  "senderAccount": "A100",
                  "receiverAccount": "A200",
                  "amount": 500
                }
                """;
        String changedRequestBody = """
                {
                  "senderAccount": "A100",
                  "receiverAccount": "A200",
                  "amount": 600
                }
                """;

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "reused-payment-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequestBody))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "reused-payment-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changedRequestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Idempotency key has already been used for different payment details"));
    }

    @Test
    void missingIdempotencyKeyReturnsBadRequest() throws Exception {
        String requestBody = """
                {
                  "senderAccount": "A100",
                  "receiverAccount": "A200",
                  "amount": 500
                }
                """;

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Idempotency-Key header is required"));
    }

    private BigDecimal balanceOf(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).orElseThrow().getBalance();
    }
}
