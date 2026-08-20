# 🚀 LegacyPay

> **A Payment System Modernization Learning Project**
>
> Bridging the reliability of **COBOL/Mainframe systems** with the agility of **Java, Spring Boot, APIs, and modern backend engineering**.

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=for-the-badge&logo=springboot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/PostgreSQL-Database-blue?style=for-the-badge&logo=postgresql" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/JPA%2FHibernate-Persistence-59666C?style=for-the-badge&logo=hibernate" alt="JPA Hibernate">
  <img src="https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven" alt="Maven">
  <img src="https://img.shields.io/badge/Tests-14%20Passing-success?style=for-the-badge" alt="14 Tests Passing">
</p>

---

## 💡 What is LegacyPay?

**LegacyPay** is a learning-focused payment modernization project that demonstrates how a traditional, legacy-style payment system can be **modernized gradually instead of being rewritten all at once**.

The project starts with a simulated legacy payment engine and a modern REST API, then progressively introduces:

- reliable payment business rules
- persistent account data
- transactional money movement
- payment history
- predictable API errors
- idempotent payment requests
- and, in future milestones, concurrency protection, events, observability, Kafka, Docker, and migration strategies

### The core idea

```text
┌─────────────────────────────┐
│ Legacy / Mainframe World    │
│ COBOL-style Payment Logic   │
└──────────────┬──────────────┘
               │
               │ Adapter / Modernization Boundary
               ▼
┌─────────────────────────────┐
│ Modern Java / Spring Boot   │
│ REST API + Payment Service  │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│ PostgreSQL                  │
│ Accounts + Payment History  │
└─────────────────────────────┘
```

---

## 🎯 Why this project?

Real payment systems have a different set of challenges from a normal CRUD application.

A payment system must answer questions such as:

> What if the same request is retried?

> What if the sender has insufficient funds?

> What if one side of a transfer succeeds and the other side fails?

> How do we keep a permanent history of payment attempts?

> How can a modern application interact with legacy business logic?

LegacyPay uses those questions as individual engineering milestones.

---

# 🏗️ Current Architecture

### End-to-end payment flow

```text
                     ┌───────────────────────┐
                     │   Client / Postman    │
                     │   Browser / curl      │
                     └───────────┬───────────┘
                                 │
                         POST /api/payments
                                 │
                                 ▼
                    ┌─────────────────────────┐
                    │   PaymentController     │
                    │ HTTP + DTO + Validation │
                    └───────────┬─────────────┘
                                │
                                ▼
                    ┌─────────────────────────┐
                    │     PaymentService      │
                    │ Business Rules          │
                    │ @Transactional          │
                    │ Idempotency             │
                    └──────┬─────────┬────────┘
                           │         │
               ┌───────────┘         └──────────────┐
               ▼                                    ▼
     ┌────────────────────┐              ┌────────────────────────┐
     │  AccountRepository │              │ PaymentTransactionRepo │
     │  JPA / Hibernate   │              │ JPA / Hibernate        │
     └──────────┬─────────┘              └───────────┬────────────┘
                │                                    │
                ▼                                    ▼
        ┌────────────────────────────────────────────────────┐
        │                    PostgreSQL                      │
        │                                                    │
        │  accounts                  payment_transactions    │
        │  ─────────                ─────────────────────    │
        │  A100 | 5000              sender | receiver        │
        │  A200 | 1000              amount | status           │
        │                           reason | created_at       │
        │                           idempotency_key           │
        └────────────────────────────────────────────────────┘

                Modern integration boundary
                           ▲
                           │
               ┌───────────┴───────────┐
               │ LegacyPaymentAdapter │
               └───────────┬───────────┘
                           │
                           ▼
               ┌────────────────────────┐
               │ LegacyPaymentEngine    │
               │ Simulated COBOL-style  │
               │ result codes:          │
               │ 00 / 51 / 14           │
               └────────────────────────┘
```

---

## 🔄 Payment Processing Flow

```text
1. Receive payment request
           ↓
2. Validate request
           ↓
3. Require Idempotency-Key
           ↓
4. Check whether key already exists
           ↓
      ┌────┴────┐
      │         │
   Exists    New request
      │         │
      ▼         ▼
Return old   Validate accounts
result           ↓
             Check balance
                  ↓
             Debit sender
                  ↓
             Credit receiver
                  ↓
          Save payment transaction
                  ↓
               Commit
                  ↓
             Return response
```

### Failure path

```text
Invalid request
      ↓
HTTP 400
      ↓
ApiError

Invalid account
      ↓
REJECTED
      ↓
No balance change

Insufficient funds
      ↓
REJECTED
      ↓
No balance change

Unexpected database failure
      ↓
Transaction rollback
      ↓
No partial transfer
```

---

# ✅ What has been completed

| # | Milestone | Status |
|---|---|---|
| 1 | Spring Boot foundation + Health API | ✅ |
| 2 | `POST /api/payments` | ✅ |
| 3 | Legacy Payment Engine + Adapter | ✅ |
| 4 | Account + Debit/Credit business logic | ✅ |
| 5 | PostgreSQL + JPA/Hibernate | ✅ |
| 6 | Persistent payment transaction history | ✅ |
| 7 | Consistent API error handling | ✅ |
| 8 | Idempotency-Key / duplicate-payment protection | ✅ |

### Current verification

**14 tests passing ✅**

The current implementation is intentionally incremental and educational.

---

# 🧩 Core Components

| Component | Responsibility |
|---|---|
| `PaymentController` | Receives HTTP requests and returns API responses |
| `PaymentRequest` | Input DTO |
| `PaymentResponse` | Success/business-response DTO |
| `PaymentService` | Core payment business rules |
| `Account` | Account entity and debit/credit operations |
| `AccountRepository` | Persistent account access |
| `PaymentTransaction` | Payment/audit record |
| `PaymentTransactionRepository` | Transaction-history persistence |
| `LegacyPaymentAdapter` | Modern-to-legacy integration boundary |
| `LegacyPaymentEngine` | Simulated legacy payment logic |
| `GlobalExceptionHandler` | Consistent API validation errors |

---

# 💳 Example API

## Create a payment

```http
POST /api/payments
Content-Type: application/json
Idempotency-Key: payment-abc-123
```

### Request

```json
{
  "senderAccount": "A100",
  "receiverAccount": "A200",
  "amount": 500
}
```

### Successful response

```json
{
  "status": "ACCEPTED",
  "message": "Payment request received"
}
```

### Insufficient funds

```json
{
  "status": "REJECTED",
  "message": "INSUFFICIENT_FUNDS"
}
```

### Invalid amount

```json
{
  "status": 400,
  "message": "amount must be greater than zero"
}
```

### Idempotency conflict

Using an existing key with different payment details returns:

```http
409 Conflict
```

---

# 🔑 Why Idempotency Matters

A client can lose the response after a successful payment and retry the same request.

Without idempotency:

```text
Request #1 → ₹500 transferred ✅
Network timeout
Request #2 → ₹500 transferred again ❌
```

With LegacyPay:

```text
Request #1 + key ABC
        ↓
Payment processed

Retry + key ABC
        ↓
Existing transaction found
        ↓
Original result returned
        ↓
No second transfer
```

This is one of the key payment-system reliability concepts demonstrated by the project.

---

# 💰 Transaction Safety

Payment processing is wrapped in a database transaction.

Conceptually:

```text
Debit sender
     +
Credit receiver
     +
Create payment record
     =
ONE DATABASE TRANSACTION
```

If an unexpected failure occurs:

```text
┌─────────────────────┐
│ Debit                │
│ Credit               │
│ Transaction Record   │
└──────────┬──────────┘
           │
      failure?
           │
           ▼
       ROLLBACK
```

The goal is to avoid partially completed money movement.

---

# 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 | Backend language |
| Spring Boot | Application framework |
| Spring Web | REST APIs |
| Spring Data JPA | Persistence abstraction |
| Hibernate | JPA implementation |
| PostgreSQL | Persistent database |
| Maven | Build & dependency management |
| JUnit 5 | Testing |
| Mockito | Test support |
| Git / GitHub | Version control & collaboration |

---

# 📁 Project Structure

```text
LegacyPay/
├── pom.xml
├── .gitignore
│
└── src/
    ├── main/
    │   ├── java/com/legacypay/
    │   │   ├── LegacyPayApplication.java
    │   │   │
    │   │   ├── api/
    │   │   │   ├── ApiError.java
    │   │   │   └── GlobalExceptionHandler.java
    │   │   │
    │   │   ├── health/
    │   │   │   └── HealthController.java
    │   │   │
    │   │   └── payment/
    │   │       ├── Account.java
    │   │       ├── AccountDataInitializer.java
    │   │       ├── AccountRepository.java
    │   │       ├── PaymentController.java
    │   │       ├── PaymentRequest.java
    │   │       ├── PaymentResponse.java
    │   │       ├── PaymentService.java
    │   │       ├── PaymentTransaction.java
    │   │       ├── PaymentTransactionRepository.java
    │   │       ├── IdempotencyKeyReuseException.java
    │   │       ├── MissingIdempotencyKeyException.java
    │   │       │
    │   │       └── legacy/
    │   │           ├── LegacyPaymentAdapter.java
    │   │           ├── LegacyPaymentEngine.java
    │   │           └── LegacyPaymentResult.java
    │   │
    │   └── resources/
    │       └── application.properties
    │
    └── test/
        ├── java/com/legacypay/
        └── resources/
```

---

# ▶️ Getting Started

## Prerequisites

- Java 21
- Maven
- PostgreSQL
- Git

## Clone

```bash
git clone https://github.com/adjha/LegacyPay.git
cd LegacyPay
```

## Configure PostgreSQL

LegacyPay expects PostgreSQL connection settings through environment variables.

```bash
export LEGACYPAY_DB_URL="jdbc:postgresql://localhost:5432/legacypay"
export LEGACYPAY_DB_USERNAME="your_username"
export LEGACYPAY_DB_PASSWORD="your_password"
```

> Never commit real database credentials, API keys, tokens, or other secrets to GitHub.

## Run

```bash
mvn spring-boot:run
```

Health check:

```http
GET http://localhost:8080/api/health
```

---

# 🧪 Run Tests

```bash
mvn test
```

Current project verification:

```text
14 tests
0 failures
0 errors
BUILD SUCCESS
```

---

# 🗺️ Roadmap

The project is intentionally being developed milestone-by-milestone.

- [x] Spring Boot foundation
- [x] Payment REST API
- [x] Legacy engine simulation
- [x] Debit / credit business logic
- [x] PostgreSQL + JPA
- [x] Payment transaction history
- [x] API error handling
- [x] Idempotency

### Next

- [ ] Concurrency-safe duplicate handling
- [ ] Payment status lifecycle
- [ ] Database migrations with Flyway
- [ ] Authentication & authorization
- [ ] Audit logging
- [ ] Observability & metrics
- [ ] Dockerized environment
- [ ] Payment events
- [ ] Kafka / asynchronous communication
- [ ] Retry & failure recovery
- [ ] Legacy-to-modern migration strategy
- [ ] Production-oriented hardening

---

# 🤝 Contributing

Contributions are welcome.

Good first contributions can include:

- improving documentation
- adding tests
- adding validation cases
- improving API examples
- identifying edge cases
- improving error messages
- proposing new modernization milestones

### Suggested contribution flow

```text
Fork
  ↓
Create a feature branch
  ↓
Implement
  ↓
Run tests
  ↓
Commit
  ↓
Push
  ↓
Open Pull Request
```

See `CONTRIBUTING.md` for detailed guidelines.

---

# ⚠️ Educational Project Disclaimer

LegacyPay is an **educational project**.

It is designed to demonstrate payment-system concepts, backend architecture, reliability patterns, and legacy modernization strategies.

It is **not a production banking platform** and should not be used to process real financial transactions.

---

# 🌱 Project Philosophy

> **Modernize incrementally. Preserve business knowledge. Improve reliability.**

LegacyPay is built around the idea that modernization is not simply:

```text
COBOL ❌
Java  ✅
```

It is closer to:

```text
Legacy Reliability
        +
Modern APIs
        +
Modern Persistence
        +
Modern Reliability Patterns
        ↓
Future-Ready Payment Architecture
```

---

## ⭐ If you find the project useful

A ⭐ on the repository and constructive feedback are welcome.

---

### License

This project is intended to be released under the **MIT License**.
