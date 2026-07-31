# MSME Lending Decision System

This is a full-stack MSME Lending Decision Engine built using **Spring Boot 3 (Java 17)** for the backend and **React + TypeScript (Vite)** for the frontend. The system evaluates business profiles and loan requests based on a custom credit scoring model, persisting transactional data to **PostgreSQL** and audit trails to **MongoDB**.

## Features
- **Instant Credit Scoring:** Deterministic algorithm (0-100 scale, threshold 60) evaluating revenue ratio, loan multiple, tenure risk, and sector risk.
- **Async Processing:** Background decision computing available via `?mode=async` with a React frontend that polls for updates.
- **Robust Validation:** Strong PAN format validation, global exception handlers mapping to standard API envelopes (`INVALID_PAN_FORMAT`, `DATA_INCONSISTENCY`).
- **Fraud/Consistency Gate:** Automatically caps score at 20 and forces a "Rejected" decision if the loan requested is greater than 20x the monthly revenue.
- **Idempotency:** Requesting a decision for the same application ID returns the exact same historical decision instead of re-evaluating.
- **Rate Limiting:** In-memory Token Bucket rate limiter applied strictly on the decision computation endpoints.
- **Premium Frontend:** A beautiful, responsive glassmorphism design with micro-animations.

---

## Tech Stack
- **Backend:** Java 17, Spring Boot 3, Spring Data JPA, Spring Data MongoDB, Flyway.
- **Frontend:** React, TypeScript, Vite, Vanilla CSS.
- **Databases:** PostgreSQL (Relational Data), MongoDB (Audit Logs).
- **Containerization:** Docker Compose for seamless local development.

---

## Local Setup Instructions

### Prerequisites
- Docker and Docker Compose
- Java 17 / Maven
- Node.js (v18+)

### 1. Start Databases using Docker
The `docker-compose.yml` file in the root directory contains the configuration for PostgreSQL and MongoDB.

```bash
# Run this from the root of the project
docker-compose up -d
```

### 2. Run the Backend
The backend uses Flyway to automatically create the PostgreSQL schema on startup.

```bash
cd backend
mvn clean install
mvn spring-boot:run
```
The backend API runs on `http://localhost:8080`.

> **Note on Schema Validation:** Since the project is configured with strict Hibernate database schema validation (`spring.jpa.jpa.hibernate.ddl-auto: validate`), it verifies the entity mappings match the database schema exactly. If you change entity files or run into column type discrepancies (e.g., PostgreSQL `CHAR(10)` vs Java `String` or `SMALLINT` vs Java `Integer`), always ensure you run a clean compile (`mvn clean install` or Rebuild in your IDE) so that explicit type mappings like `@JdbcTypeCode` are correctly built and loaded.

### 3. Run the Frontend
```bash
cd frontend
npm install
npm run dev
```
The frontend UI runs on `http://localhost:5173`.

---

## Credit Scoring Logic

The scoring algorithm assigns a score out of **100**. Any score **>= 60** yields an **Approved** decision.

1. **Revenue-to-EMI Ratio (Max 40 points)**
   - EMI = `(Loan / Tenure) * 1.12` (assuming a flat 12% interest).
   - Ratio >= 3.0 (40 pts, `STRONG_REPAYMENT_CAPACITY`)
   - Ratio >= 2.0 (30 pts)
   - Ratio >= 1.5 (18 pts)
   - Ratio >= 1.0 (8 pts)
   - Ratio < 1.0 (0 pts, `LOW_REVENUE_TO_EMI`)

2. **Loan-to-Revenue Multiple (Max 25 points)**
   - Multiple <= 3 (25 pts, `HEALTHY_LOAN_SIZE`)
   - Multiple <= 6 (18 pts)
   - Multiple <= 10 (10 pts)
   - Multiple <= 15 (4 pts)
   - Multiple > 15 (0 pts, `HIGH_LOAN_RATIO`)

3. **Tenure Risk (Max 15 points)**
   - < 3 months (5 pts, `SHORT_TENURE_RISK`)
   - 3-6 months (10 pts)
   - 7-36 months (15 pts)
   - 37-60 months (10 pts)
   - > 60 months (5 pts, `LONG_TENURE_RISK`)

4. **Business Type Risk (Max 10 points)**
   - Services (10 pts)
   - Retail (7 pts)
   - Manufacturing (5 pts, `SECTOR_RISK`)

### Worked Example
- **Input**: Retail (`7 pts`), Monthly Revenue `₹40,000`. Loan `₹1,20,000` over `6 months`.
- **Calculations**:
  - EMI = `(120000 / 6) * 1.12` = `₹22,400`.
  - Ratio = `40000 / 22400` = `1.78` (`18 pts`).
  - Multiple = `120000 / 40000` = `3.0` (`25 pts`, `HEALTHY_LOAN_SIZE`).
  - Tenure = `6` (`10 pts`).
- **Result**: `7 + 18 + 25 + 10 = 60` (Approved).

---

## Edge Cases Handled

1. **Data Inconsistency / Fraud Gate:** 
   If a business requests a loan that is strictly greater than **20x their monthly revenue**, the engine triggers a fraud gate. The system forcibly caps the total score at **20**, immediately rejects the application, and attaches the `DATA_INCONSISTENCY` reason code.

2. **Idempotency:** 
   Evaluating the same loan application ID multiple times (`POST /api/v1/applications/{id}/decision`) simply fetches the previously stored `Decision` record from PostgreSQL rather than recomputing it, preventing duplicated credit footprints.

3. **Validation Error Contracts:**
   Missing fields yield `400 VALIDATION_ERROR`. However, if the PAN format is invalid, we specifically capture that constraint violation and map it directly to `400 INVALID_PAN_FORMAT` to ensure API consumers get exact contract envelopes.

4. **MongoDB Resilience:**
   Audit logs (`APPLICATION_SUBMITTED`, `DECISION_COMPUTED`) are dispatched asynchronously using Spring `@Async`. If MongoDB is down or unreachable, the system catches the exception and logs it to `System.err`, ensuring that primary PostgreSQL transactions do not block or fail.
