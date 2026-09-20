package com.legacypay.payment;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

@Component
public class IdempotencyLockRegistry {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock lockFor(String idempotencyKey) {
        return locks.computeIfAbsent(idempotencyKey, ignored -> new ReentrantLock());
    }
}
