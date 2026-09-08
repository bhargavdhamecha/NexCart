PROJECT CONTEXT: NexCart

Goal:
Build a production-oriented small-scale Amazon-like e-commerce backend called NexCart.
Primary purpose is interview preparation for Java Backend / SDE-2 roles, especially:
- Java/Spring Boot
- Concurrency
- Distributed Systems
- Production Troubleshooting
- HLD/LLD
- PostgreSQL
- Redis
- Kafka
- Docker

Architecture Direction:
Start as a modular monolith.
Do NOT start with many microservices immediately.
Keep clear domain boundaries so modules can later be extracted into services.

Main backend modules:
- user
- product
- inventory
- cart
- order
- payment
- notification
- security
- config
- common

Suggested project structure:

nexcart/
├── backend/
│   └── nexcart-backend/
├── frontend/
│   └── nexcart-frontend/
├── docker-compose.yml
└── README.md

Spring Boot setup:
- Packaging: JAR
- Java: 17 or 21
- Build: Gradle
- Group: com.nexcart
- Artifact: nexcart-backend

Main dependencies:
- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Spring Security
- Validation
- Kafka
- Redis
- Spring Boot Actuator
- JUnit
- Mockito
- OpenAPI/Swagger

Spring Data JPA is optional technically, but recommended.
PostgreSQL JDBC driver is required to connect to PostgreSQL.

Database:
Use PostgreSQL through Docker.
No need to install PostgreSQL locally.

Docker PostgreSQL example:

services:
  postgres:
    image: postgres:16
    container_name: nexcart-postgres
    environment:
      POSTGRES_DB: nexcart
      POSTGRES_USER: nexcart_user
      POSTGRES_PASSWORD: nexcart_password
    ports:
      - "5432:5432"
    volumes:
      - nexcart_postgres_data:/var/lib/postgresql/data

volumes:
  nexcart_postgres_data:

Spring Boot datasource:

spring.datasource.url=jdbc:postgresql://localhost:5432/nexcart
spring.datasource.username=nexcart_user
spring.datasource.password=nexcart_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

Important:
Docker Desktop asking for a Mac/system admin password is unrelated to the PostgreSQL password.
PostgreSQL credentials must match Docker environment values.

Core functional requirements:

User:
- Register
- Login
- Logout
- Authorization

Product:
- Browse/list products
- Product details
- Price
- Availability

Cart:
- Add product
- Remove product
- Update quantity
- View cart

Important cart rule:
Adding an item to cart DOES NOT reserve inventory.
Cart represents purchase intention only.
Inventory should normally be reserved during checkout.

Checkout:
- Revalidate product
- Revalidate latest price
- Check inventory
- Reserve inventory
- Initiate payment

Order:
- Create and track order
- Order should have state/status
- Successful payment leads to confirmation
- Failed payment should not confirm order

Inventory:
- Prevent overselling
- Handle concurrent purchase of last item
- Track available and reserved quantities

Payment:
- External payment provider concept
- Idempotent payment processing
- Handle:
  - success
  - failure
  - timeout
  - retry
  - duplicate request

Notification:
- Async email confirmation
- Order should not fail if notification service is down

Database model:

User:
- userId
- firstName
- lastName
- email
- password

Do NOT store list of orderIds inside User.

Product:
- productId
- title
- description
- price
- status
- createdAt
- updatedAt

Inventory:
- productId
- availableQuantity
- reservedQuantity
- updatedAt

One inventory row per product.

Cart:
- cartId
- userId
- createdAt
- updatedAt

CartItem:
- cartItemId
- cartId
- productId
- quantity

Order:
- orderId
- userId
- totalAmount
- orderDate
- status
- paymentId

OrderItem:
- orderItemId
- orderId
- productId
- quantity
- unitPrice
- itemTotal

Important:
Store unitPrice in OrderItem as historical purchase price.
Do not rely on current Product.price for old orders.

Payment:
- paymentId
- orderId
- userId
- amount
- status
- transactionId
- idempotencyKey
- paymentMethod
- createdAt
- updatedAt

Suggested Order status:
- CREATED
- PENDING_PAYMENT
- PAID
- CONFIRMED
- PAYMENT_FAILED
- CANCELLED

Suggested Payment status:
- INITIATED
- SUCCESS
- FAILED
- REFUNDED

Normalization:
Target approximately 3NF.
Do not store arrays/collections like orderItems in one column.
Use separate relational tables.

Order 1:N OrderItem
Cart 1:N CartItem
User 1:N Order

Historical unitPrice in OrderItem is intentional and correct.

Architecture reasoning:
Product:
- high read traffic
- good candidate for Redis caching

Inventory:
- concurrency-sensitive
- consistency-critical

Order:
- core business workflow
- coordinates inventory/payment

Payment:
- critical
- idempotency required

Notification:
- asynchronous
- good Kafka consumer candidate

Kafka usage:
Use Kafka for asynchronous events, not for everything.

Possible flow:

Order -> PaymentRequested event
Payment -> PaymentCompleted/PaymentFailed event
Order -> OrderConfirmed event
Notification consumes OrderConfirmed

Notification failure must not block order confirmation.

Order and Inventory interaction:
Inventory reservation is a key design problem.
Main scenario:
stock = 1
multiple users checkout concurrently
only one purchase should succeed.

Redis:
Use for:
- product caching
- rate limiting
- maybe distributed lock experiments
- counters
- TTL-based cache

Redis should generally NOT be the source of truth for inventory.

Kafka:
Use for:
- order events
- payment events
- notification events
- consumer lag experiments

Production scenarios to simulate:
1. High product traffic
2. Concurrent checkout for last stock item
3. Payment failure
4. Payment timeout
5. Duplicate payment request
6. Notification consumer down
7. Kafka consumer lag
8. Redis down
9. PostgreSQL slow/down
10. Retry and idempotency behavior

Testing tools:
- Postman
- Swagger/OpenAPI
- JMeter, k6, or Gatling for load/concurrency testing

Observability:
Start with:
- Spring Boot Actuator
- Micrometer
- structured logging

Later optional:
- Prometheus
- Grafana
- distributed tracing

Frontend/backend:
Frontend communicates with backend via REST APIs.

Example:
POST /api/auth/login

Login flow:
Frontend sends email/password
-> Spring Boot validates
-> backend generates JWT
-> frontend sends JWT on protected requests

Authorization header:
Authorization: Bearer <JWT>

JWT should be validated by Spring Security/filter before protected controller execution.

Project implementation order:
1. Create Spring Boot project
2. PostgreSQL + Docker
3. User registration/login
4. Product APIs
5. Cart
6. Order
7. Inventory
8. Concurrency handling
9. Payment
10. Kafka
11. Notification
12. Redis caching
13. Production failure simulations
14. Frontend

LLD approach:
Do not make LLD only:
Controller -> Service -> Repository

Focus on:
- domain entities
- relationships
- state machines
- service interfaces
- repository interfaces
- transaction boundaries
- concurrency
- idempotency
- Kafka events
- REST APIs
- database schema

Main learning principle:
40% build
30% intentionally break
30% debug

Keep solutions interview-friendly and production-oriented.
Prefer simple architecture first.
Do not introduce Kubernetes, Elasticsearch, service discovery, or unnecessary tools early.