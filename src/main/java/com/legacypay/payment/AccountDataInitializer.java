package com.legacypay.payment;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountDataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;

    public AccountDataInitializer(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        createAccountIfMissing("A100", "5000.00");
        createAccountIfMissing("A200", "1000.00");
    }

    private void createAccountIfMissing(String accountNumber, String balance) {
        if (accountRepository.findByAccountNumber(accountNumber).isEmpty()) {
            accountRepository.save(new Account(accountNumber, new BigDecimal(balance)));
        }
    }
}
