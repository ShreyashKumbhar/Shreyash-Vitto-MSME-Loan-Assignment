# MSME Lending Engine - Technical Writeup

## Architecture
The system is built as a monolith using a standard layered architecture in **Spring Boot 3** (Controllers → Services → Repositories → DB) and a single-page application in **React**.

- **Persistence Layer:** Uses **PostgreSQL** for primary transactional data (Business Profiles, Loan Applications, Decisions) due to its ACID guarantees and relational integrity. We leverage the `JSONB` column type for the `scoreBreakdown` object in the Decision table to avoid over-normalizing nested arbitrary data. To ensure strict schema integrity, the backend is configured to perform strict Hibernate schema validation (`ddl-auto: validate`) against the schema initialized by **Flyway**. Explicit type conversions like `@JdbcTypeCode(SqlTypes.CHAR)` for `pan` and `@JdbcTypeCode(SqlTypes.SMALLINT)` for `credit_score` are used to align Java types with database constraints.
- **Audit Layer:** Uses **MongoDB** for fire-and-forget audit trails (`APPLICATION_SUBMITTED`, `DECISION_COMPUTED`). This isolates read-heavy/write-heavy analytical audit logs from the main relational DB.
- **Frontend Layer:** A lightweight React application using Vite and standard CSS for high performance, interacting with the backend via REST over JSON.

## Trade-offs Made
1. **Monolith vs Microservices:** We opted for a monolith over microservices. Given the bounded context (a single credit engine), microservices would introduce unnecessary network overhead, deployment complexity, and distributed transaction issues.
2. **In-Memory Rate Limiting:** We implemented the token bucket rate limiter in-memory using Spring Interceptors and `ConcurrentHashMap`. While Redis would be better for a multi-instance deployment, an in-memory solution keeps local setup simple and dependency-free.
3. **No Stateful Orchestration:** For the `mode=async` bonus, we utilized Spring's `@Async` and a simple polling mechanism on the frontend rather than introducing heavy workflow engines (like Temporal or Camunda). This satisfies the requirement while keeping the architecture lean.
4. **Strict Schema Validation vs. Dynamic Generation:** Rather than allowing Hibernate to dynamically auto-create/update tables (`ddl-auto: update`), we use Flyway migrations as the absolute source of truth. We use strict JPA validation (`validate`), solving dialect/type mismatches (such as Postgres `CHAR(10)` mapping to a Java `String` or `SMALLINT` to an `Integer`) via explicit Hibernate mapping annotations (`@JdbcTypeCode(SqlTypes.CHAR)` and `@JdbcTypeCode(SqlTypes.SMALLINT)`). This guarantees that type mismatches are caught early on application startup rather than causing queries to fail dynamically at runtime.

## Future Work & Scalability
If the system were to scale to thousands of loan requests per minute, the following enhancements would be required:

1. **Caching (Redis):** Idempotency checks and rate limiting currently hit the database or use local memory. Introducing Redis would allow distributed caching and rate limiting across multiple scaled instances.
2. **Message Queues (Kafka / RabbitMQ):** The async decision engine could be decoupled into a worker pool processing events from a message queue, guaranteeing at-least-once delivery and smoothing out traffic spikes.
3. **Event-Driven Audit Trails:** Instead of `@Async` calls to MongoDB within the monolith, we could emit domain events to an event bus. A separate microservice could then consume these events and flush them to MongoDB, entirely removing DB contention.
4. **Enhanced Security:** Implementing JWT-based authentication (Spring Security) for the API endpoints and restricting CORS origins in production to only the deployed frontend URL.
