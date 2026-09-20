package com.legacypay.audit;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String eventType, Long paymentTransactionId, String message) {
        auditLogRepository.save(new AuditLog(eventType, paymentTransactionId, message));
    }

    public List<AuditLogResponse> findForPayment(Long paymentTransactionId) {
        return auditLogRepository.findByPaymentTransactionIdOrderByCreatedAtAsc(paymentTransactionId)
                .stream()
                .map(AuditLogResponse::new)
                .toList();
    }
}
