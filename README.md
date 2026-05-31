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

---

# Kafka Event-Driven Communication

## Producer
transaction-service publishes transaction completion events to Kafka.

## Consumer
notification-service consumes Kafka events and processes notifications asynchronously.

## Dead Letter Topic (DLT)
Failed Kafka events are redirected to DLT and retried later.

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

# Security Features

- JWT Authentication
- API Gateway Request Filtering
- AES Encrypted Kafka Payloads
- Distributed Redis-based Rate Limiting

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

---

# Author

Kalyan Ashis Deb