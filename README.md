# Payment App

Enterprise-style payment processing system built using Spring Boot microservices with Kafka event-driven architecture, Redis-based rate limiting, JWT authentication, API Gateway routing, and AES-encrypted Kafka payload protection.

---

# Architecture Overview

The application follows a distributed microservices architecture where requests flow through an API Gateway and services communicate asynchronously using Apache Kafka.

## Microservices

- api-gateway
- auth-service
- account-service
- transaction-service
- notification-service
- common-security (shared encryption library)

---

# Tech Stack

## Backend
- Java 21
- Spring Boot 3.x
- Spring Security
- Spring Cloud Gateway
- Spring Data JPA
- Hibernate

## Messaging
- Apache Kafka
- Kafka Consumer Groups
- Dead Letter Topic (DLT)

## Database
- MySQL

## Caching / Distributed Components
- Redis

## Security
- JWT Authentication
- AES Payload Encryption

---

# Key Features

## JWT Authentication & Authorization
- Secure login flow using JWT
- Request validation at API Gateway
- Protected transaction endpoints

## API Gateway
- Centralized request routing
- JWT validation filter
- Global request logging

## Distributed Rate Limiting

### Sliding Window Rate Limiter
Implemented manually using Redis Sorted Set (ZSET).

### Token Bucket Rate Limiter
Implemented using Spring Cloud Gateway RedisRateLimiter.

## Transaction Reversal

Supports reversal of successful transfer transactions with balance validation, configurable reversal window checks, duplicate reversal protection, and compensation handling for partial failures.

## Statement Export

Supports account statement export in:

- CSV format
- PDF format

### Export CSV Statement

GET /transactions/export/csv/{accountNumber}

### Export PDF Statement

GET /transactions/export/pdf/{accountNumber}

Each statement includes:

- Account Number
- Current Balance
- Generated Timestamp
- Transaction History
- Formatted Date/Time

## Transactional Outbox Pattern

Implements the Transactional Outbox Pattern for reliable event publishing by persisting business transactions and corresponding Outbox events atomically within the same database transaction. A scheduled Outbox Publisher periodically processes **PENDING** events, publishes them to Kafka and marks them as **PUBLISHED** upon successful delivery, preventing dual-write inconsistencies and ensuring reliable event propagation.

## Idempotent Consumer

Prevents duplicate Kafka message processing by maintaining a `processed_events` table in the Notification Service. Before processing an incoming Kafka event, the consumer checks whether the transaction has already been processed using its unique transaction ID. If the event is new, it is processed successfully and recorded in the `processed_events` table. If the same event is received again due to retries, Outbox republishing, or duplicate Kafka delivery, it is safely ignored, ensuring that notifications are sent exactly once from the application's perspective.

---

# Kafka Event-Driven Communication

## Producer
transaction-service publishes transaction completion events to Kafka.

## Consumer
notification-service consumes Kafka events and processes notifications asynchronously.

## Dead Letter Topic (DLT)
Failed Kafka events are redirected to DLT and retried later.

---

### Transaction Completed Event

When a transfer is successfully completed, the Transaction Service publishes a TransactionCompletedEvent to Kafka. The Notification Service consumes the event and processes transaction notifications.

### Transaction Reversal Event

When a transaction reversal is successfully processed, the Transaction Service publishes a TransactionReversedEvent to Kafka. The Notification Service consumes the event, decrypts the payload, deserializes the event, and processes reversal notifications.

---

# Kafka Payload Encryption

Kafka messages are encrypted before publishing and decrypted at the consumer side using AES encryption.

## Encryption Flow

Transaction Event  
→ JSON Serialization  
→ AES Encryption  
→ Kafka Topic  
→ AES Decryption  
→ Object Deserialization

## Shared Security Library
A reusable Maven module named `common-security` contains the AES encryption utility shared across services.

---

# Rate Limiting

## Sliding Window
Custom implementation using:
- Redis Sorted Set
- Request timestamps
- Expiry cleanup logic

## Token Bucket
Implemented using:
- Spring Cloud Gateway
- RedisRateLimiter
- KeyResolver

---

# Outbox Pattern

## Event Publishing Flow (Outbox Pattern)

```text
Client
   │
   ▼
Transaction Service
   │
   ├── Save Transaction
   └── Save Outbox Event (PENDING)
           │
           ▼
      MySQL Outbox Table
           │
           ▼
 @Scheduled Outbox Publisher
           │
           ▼
     Publish to Kafka
           │
           ▼
Mark Outbox Event as PUBLISHED
           │
           ▼
      Notification Service
```
This implementation ensures that business transactions and event persistence occur atomically, preventing dual-write inconsistencies and enabling reliable event delivery through asynchronous publishing.

## Event Processing Flow (Idempotent Consumer)

```text
Kafka
   │
   ▼
Notification Consumer
   │
   ▼
Decrypt & Deserialize Event
   │
   ▼
Check processed_events Table
   │
   ├── Already Processed?
   │       │
   │       ├── Yes ──► Ignore Duplicate Event
   │       │
   │       └── No
   │
   ▼
Process Notification
   │
   ▼
Save Transaction ID to processed_events
```

### Benefits

* Prevents duplicate notification processing.
* Handles duplicate Kafka message delivery safely.
* Works seamlessly with the Transactional Outbox Pattern.
* Supports reliable retries without processing the same event multiple times.
* Improves fault tolerance in event-driven microservices.

# Security Features

- JWT Authentication
- API Gateway Request Filtering
- AES Encrypted Kafka Payloads
- Distributed Redis-based Rate Limiting

---

## Daily Transfer Limit

A configurable daily transfer limit has been implemented to prevent excessive fund transfers from a single account within a day.

### Configuration

The limit is configured in:

```yaml
transfer:
  daily-limit: 5000
```

---

# Project Structure

```text
payment-app/

├── api-gateway
├── auth-service
├── account-service
├── transaction-service
├── notification-service
├── common-security
```

---

# How To Run

## Prerequisites

- Java 21
- Maven
- MySQL
- Redis
- Apache Kafka
- Zookeeper

## Start Order

1. Redis
2. Zookeeper
3. Kafka
4. MySQL
5. auth-service
6. account-service
7. transaction-service
8. notification-service
9. api-gateway

---

# Sample Flow

1. User authenticates using JWT
2. Request reaches API Gateway
3. Gateway validates JWT
4. Rate limiter validates request quota
5. transaction-service processes transaction
6. Event published to Kafka
7. Kafka payload stored in encrypted format
8. notification-service consumes and decrypts event
9. Notification processed asynchronously

---

## Infrastructure Setup

This project uses Docker Compose to start the required infrastructure services.

### Start Kafka and Redis

```bash
docker compose up -d
```

---

## Database Setup

The project uses MySQL as the primary database.

### Import Schema

Import the provided schema file:

```bash
mysql -u root -p < database/schema.sql
```

This will create the required database objects and tables.

The application is configured with Hibernate DDL auto-update and can create/update tables automatically during startup.

---

## Admin User Setup

Some operations require an ADMIN user.

### Step 1: Register a User

Use the registration endpoint:

```http
POST /auth/register
```

Example:

```json
{
  "username": "super",
  "password": "password123"
}
```

### Step 2: Promote User to ADMIN

Execute the following SQL:

```sql
UPDATE authdb.users
SET role = 'ADMIN'
WHERE username = 'super';
```

The user can then access ADMIN-protected endpoints.

---

## Postman Collection

The Postman collection for testing all APIs is available under:

postman/payment-app-postman-collection.json

### Available APIs

#### Auth Service
- Register
- Login
- Logout

#### Account Service
- Create Account
- Credit
- Debit
- Balance

#### Transaction Service
- Transfer
- Transaction
- All Transactions

---

# Current Status

Implemented:
- JWT Authentication
- API Gateway Routing
- Redis Rate Limiting
- Kafka Integration
- Dead Letter Topic (DLT)
- AES Kafka Payload Encryption
- Shared Maven Security Library
- Daily Transfer Limit
- Transaction reversal for successful transfer transactions
- Reversal window validation
- Transaction history APIs
- Account statement export (CSV/PDF)
- Account balance and timestamp in statements

---

# Author

Kalyan Ashis Deb
