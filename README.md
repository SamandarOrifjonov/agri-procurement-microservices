# 🌾 Digital Procurement Platform - Microservices

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Microservices](https://img.shields.io/badge/Architecture-Microservices-success.svg)]()
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-blue.svg)]()

Enterprise-grade microservices platform for agricultural procurement with event-driven architecture, Saga pattern, and async processing.

## 🎯 Key Features

### ✅ Microservices Architecture
- **3 Independent Services**: Procurement, Supplier, Contract
- **API Gateway**: Centralized routing and load balancing
- **Loose Coupling**: Services communicate via events

### ✅ Event-Driven Communication
- **ProcurementCreatedEvent (v1)**: Versioned event system
- **Async Event Publishing**: Non-blocking communication
- **Eventual Consistency**: Distributed data synchronization

### ✅ Reliability Patterns
- **Saga Pattern**: 4-step distributed transaction with compensation
- **Async Processing**: @Async and CompletableFuture support
- **Automatic Rollback**: Compensation logic for failed transactions

### ✅ Security & Access Control
- **Role-Based Access**: BUYER, SUPPLIER, ADMIN, AUDITOR
- **AccessControl Utility**: Centralized permission checks
- **Secure Endpoints**: Role validation on all operations

### ✅ Database & Persistence
- **PostgreSQL 16**: Production-grade database
- **3 Separate Databases**: procurement_db, supplier_db, contract_db
- **Hibernate ORM**: Automatic schema management

## 🏗 Architecture

```
                    ┌─────────────────────┐
                    │   API Gateway       │
                    │   (Port 8080)       │
                    └──────────┬──────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
┌───────▼────────┐    ┌────────▼────────┐    ┌───────▼────────┐
│  Procurement   │    │    Supplier     │    │   Contract     │
│   Service      │    │    Service      │    │   Service      │
│  (Port 8084)   │    │  (Port 8085)    │    │  (Port 8086)   │
└───────┬────────┘    └────────┬────────┘    └───────┬────────┘
        │                      │                      │
┌───────▼────────┐    ┌────────▼────────┐    ┌───────▼────────┐
│ procurement_db │    │  supplier_db    │    │  contract_db   │
│  PostgreSQL    │    │  PostgreSQL     │    │  PostgreSQL    │
└────────────────┘    └─────────────────┘    └────────────────┘
```

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 16
- 8GB RAM (minimum)

### 1. Clone Repository
```bash
git clone https://github.com/SamandarOrifjonov/agri-procurement-microservices.git
cd agri-procurement-microservices
```

### 2. Setup PostgreSQL
```bash
# Install PostgreSQL (macOS)
brew install postgresql@16
brew services start postgresql@16

# Create databases
psql -U postgres -f setup-databases.sql
```

### 3. Build Project
```bash
mvn clean install -DskipTests
```

### 4. Start Services
```bash
# Automated
./start-services.sh

# Or manually (separate terminals)
cd procurement-service && mvn spring-boot:run
cd supplier-service && mvn spring-boot:run
cd contract-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

### 5. Verify
```bash
curl http://localhost:8080
```

## 📊 Services Overview

| Service | Port | Responsibility | Database | Key Features |
|---------|------|----------------|----------|--------------|
| **API Gateway** | 8080 | Routing, Load Balancing | - | Spring Cloud Gateway |
| **Procurement Service** | 8084 | Buyer requirements, procurement lifecycle | procurement_db | Event publishing, Async |
| **Supplier Service** | 8085 | Supplier management, product catalog | supplier_db | Eventual consistency |
| **Contract Service** | 8086 | Contract management with Saga | contract_db | Saga pattern, Compensation |

## 🔄 Event-Driven Flow

```
1. Buyer creates Procurement
   ↓
2. ProcurementCreatedEvent (v1) published
   ↓
3. Supplier Service listens (eventual consistency)
   ↓
4. Contract Service creates contract via Saga
   ↓
5. Saga executes 4 steps with compensation:
   - Validate Contract
   - Reserve Supplier Capacity
   - Create Contract Record
   - Notify Parties
```

## 🛡️ Saga Pattern Implementation

### Contract Creation Saga (4 Steps):

1. **Validate Contract** - Check data integrity
2. **Reserve Supplier Capacity** - Lock resources (with rollback)
3. **Create Contract Record** - Persist to database (with deletion on failure)
4. **Notify Parties** - Send notifications (with cancellation)

**Automatic Compensation**: If any step fails, all previous steps are rolled back in reverse order.

## 📚 API Examples

### Create Procurement
```bash
curl -X POST http://localhost:8080/api/procurements \
  -H "Content-Type: application/json" \
  -H "X-User-Roles: BUYER" \
  -d '{
    "title": "Wheat Purchase",
    "description": "Need 100 tons of wheat",
    "buyerId": 1,
    "productCategory": "GRAINS",
    "quantity": 100,
    "unit": "ton",
    "budget": 50000,
    "deadline": "2026-03-01T00:00:00"
  }'
```

### Register Supplier
```bash
curl -X POST http://localhost:8080/api/suppliers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Green Farm LLC",
    "email": "greenfarm@example.com",
    "phone": "+998901234567",
    "region": "Tashkent",
    "address": "123 Farm Street"
  }'
```

### Create Contract (Saga Pattern)
```bash
curl -X POST http://localhost:8080/api/contracts \
  -H "Content-Type: application/json" \
  -H "X-User-Roles: BUYER" \
  -d '{
    "procurementId": 1,
    "buyerId": 1,
    "supplierId": 1,
    "amount": 45000,
    "quantity": 100
  }'
```

### Async Endpoint Example
```bash
curl http://localhost:8080/api/procurements/status/PUBLISHED/async
```

## 🔐 Security

### Role-Based Access Control
```java
// Example: Only BUYER can create procurement
AccessControl.requireRole(userRoles, Role.BUYER, Role.ADMIN);
```

### Available Roles:
- **BUYER** - Create procurements, manage contracts
- **SUPPLIER** - Submit bids, manage products
- **ADMIN** - Full system access
- **AUDITOR** - Read-only access for compliance

## 🗄️ Database Configuration

### Connection Settings
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/procurement_db
    username: postgres
    password: postgres
```

### Automatic Schema Management
- Hibernate DDL: `update`
- Tables created automatically
- No manual SQL scripts needed

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific service tests
cd procurement-service && mvn test

# Integration tests
./test-api.sh
```

## 🚀 CI/CD Pipeline

GitHub Actions workflow includes:
- ✅ Build with Maven
- ✅ Run tests
- ✅ PostgreSQL integration
- ✅ Automated deployment (staging/production)

## 📖 Documentation

### General Documentation
- **[DEPLOYMENT.md](DEPLOYMENT.md)** - Deployment guide
- **[INTELLIJ-SETUP.md](INTELLIJ-SETUP.md)** - IntelliJ IDEA setup

### Architecture Documentation
- **[DDD Domain Reasoning](docs/architecture/DDD-DOMAIN-REASONING.md)** - Why separate bounded contexts?
- **[Event Versioning Strategy](docs/architecture/EVENT-VERSIONING-STRATEGY.md)** - Event evolution & backward compatibility
- **[Saga Failure Scenarios](docs/architecture/SAGA-FAILURE-SCENARIOS.md)** - Complete failure handling guide
- **[Configuration Guide](docs/architecture/CONFIGURATION-GUIDE.md)** - Production-ready configuration

## 🎯 Technical Highlights

### Scalability
- Async event processing
- CompletableFuture for concurrent operations
- Loose coupling via events

### Reliability
- Saga pattern with compensation
- Automatic rollback on failures
- Eventual consistency

### Maintainability
- Clean architecture
- Comprehensive comments
- Configurable enums and versions

### Adaptability
- No hardcoded business rules
- Version-controlled events (v1)
- Regulatory compliance ready

## 🛠️ Technology Stack

- **Java 17** - Modern Java features
- **Spring Boot 3.2.0** - Framework
- **Spring Cloud Gateway** - API Gateway
- **PostgreSQL 16** - Database
- **Hibernate** - ORM
- **Maven** - Build tool
- **GitHub Actions** - CI/CD

## 👨‍💻 Author

**Samandar Orifjonov**
- GitHub: [@SamandarOrifjonov](https://github.com/SamandarOrifjonov)
- Repository: [agri-procurement-microservices](https://github.com/SamandarOrifjonov/agri-procurement-microservices)

## 📝 License

This project is proprietary software. All rights reserved.

---

⭐ **Built with enterprise-grade patterns for agricultural procurement**

**Key Achievements:**
- ✅ Microservices Architecture (3+ services)
- ✅ Event-Driven Communication (ProcurementCreatedEvent v1)
- ✅ Async/Concurrency (@Async + CompletableFuture)
- ✅ Saga Pattern (4 steps + compensation)
- ✅ PostgreSQL (3 databases)
- ✅ Eventual Consistency
- ✅ Role-Based Security
- ✅ Configurable & Adaptable
- ✅ Comprehensive Documentation
