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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

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
                        .with(httpBasic("dev-client", "dev-password"))
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
                .with(httpBasic("dev-client", "dev-password"))
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
                        .with(httpBasic("dev-client", "dev-password"))
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
                        .with(httpBasic("dev-client", "dev-password"))
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
                        .with(httpBasic("dev-client", "dev-password"))
                        .header("Idempotency-Key", "duplicate-payment-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(post("/api/payments")
                        .with(httpBasic("dev-client", "dev-password"))
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
                        .with(httpBasic("dev-client", "dev-password"))
                        .header("Idempotency-Key", "reused-payment-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequestBody))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/payments")
                        .with(httpBasic("dev-client", "dev-password"))
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
                        .with(httpBasic("dev-client", "dev-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Idempotency-Key header is required"));
    }

    @Test
    void getPaymentStatusReturnsPersistedLifecycle() throws Exception {
        PaymentRequest request = paymentRequest("A100", "A200", "500.00");
        paymentService().acceptPayment(request, "controller-status-key");
        Long paymentId = paymentTransactionRepository.findByIdempotencyKey("controller-status-key")
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/payments/{paymentId}", paymentId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/payments/{paymentId}", paymentId)
                        .with(httpBasic("dev-client", "dev-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.lifecycleStatus").value("COMPLETED"));
    }

    @Test
    void getAuditLogsReturnsPaymentEvents() throws Exception {
        PaymentRequest request = paymentRequest("A100", "A200", "500.00");
        paymentService().acceptPayment(request, "controller-audit-key");
        Long paymentId = paymentTransactionRepository.findByIdempotencyKey("controller-audit-key")
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/audit/payments/{paymentId}", paymentId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/audit/payments/{paymentId}", paymentId)
                        .with(httpBasic("dev-client", "dev-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("PAYMENT_INITIATED"))
                .andExpect(jsonPath("$[1].eventType").value("PAYMENT_ACCEPTED"));
    }

    @Test
    void unauthenticatedPaymentRequestIsRejected() throws Exception {
        String requestBody = """
                {
                  "senderAccount": "A100",
                  "receiverAccount": "A200",
                  "amount": 500
                }
                """;

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "unauthenticated-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    private BigDecimal balanceOf(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).orElseThrow().getBalance();
    }

    @Autowired
    private PaymentService paymentService;

    private PaymentService paymentService() {
        return paymentService;
    }

    private PaymentRequest paymentRequest(String senderAccount, String receiverAccount, String amount) {
        PaymentRequest request = new PaymentRequest();
        request.setSenderAccount(senderAccount);
        request.setReceiverAccount(receiverAccount);
        request.setAmount(new BigDecimal(amount));
        return request;
    }
}
