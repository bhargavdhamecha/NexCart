# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project purpose

NexCart is a small-scale Amazon-like e-commerce app built primarily as **interview prep for Java Backend/SDE-2 roles** — the emphasis throughout is concurrency, idempotency, distributed systems patterns, and production-style troubleshooting, not feature breadth. See `context.md` at the repo root for the original spec (domain model, target production scenarios to simulate, learning approach: "40% build, 30% intentionally break, 30% debug"). When working in this repo, prefer the interview-relevant, production-honest way of doing something over the fastest way — that's the whole point of the project.

Two independent apps: `backend/` (Spring Boot) and `frontend/` (Angular), talking over REST. No shared code between them.

## Commands

**Backend** (from `backend/`):
```bash
./gradlew bootRun                    # run the API on :8080 (needs Postgres+Kafka — see below)
./gradlew test                       # full suite (runs against H2, not Postgres)
./gradlew test --tests "com.nexcart.backend.payment.PaymentIntegrationTest"   # single class
./gradlew test --tests "*.methodName"                                        # single test method
./gradlew compileJava                # fast compile-only check
```

**Frontend** (from `frontend/`):
```bash
npm start        # ng serve, :4200
npm run build    # ng build (production)
npm test         # ng test (Karma) — not actually used anywhere in this codebase yet; no spec files exist beyond the CLI-generated app.spec.ts
```

**Gateway** (from `gateway/`): `./gradlew bootRun` runs the Spring Cloud Gateway on `:8081` (rate-limits `/api/v1/products/**`, passes everything else through to the backend — see `gateway/.../RouteConfig.java`). The frontend talks to this, not the backend directly, in both dev (`environment.development.ts`) and production (reverse-proxied by `frontend/nginx.conf`).

**Infra**: `docker-compose up -d` from the repo root starts just Postgres (`:5432`), Kafka (`:9092`, single-node KRaft mode, official `apache/kafka` image — Bitnami's was removed from Docker Hub in 2025, don't use it), and Redis (`:6379`, backs both product-catalog caching in the backend and the gateway's rate limiter) — the three infra dependencies, for IDE-based `./gradlew bootRun` dev. `docker compose --profile full up -d --build` additionally builds and runs `backend`/`gateway`/`frontend` as containers alongside them (each has its own `Dockerfile`) — this is the full containerized stack used for deployment. See `DEPLOYMENT.md` for the actual EC2 deployment runbook, including the free-tier memory-tuning numbers (`JAVA_OPTS`, `mem_limit`, Kafka's dual-listener setup) baked into the `profiles: ["full"]` services in `docker-compose.yml`.

**Known local gotcha**: on this machine, Java's IPv4 loopback to the Dockerized Postgres can silently hang forever at `HikariPool - Starting...` (TCP handshake succeeds, but the connection never completes) while IPv6 works fine — a Docker Desktop networking issue, not a code bug. `spring.datasource.url` in `application.properties` is env-var-driven (`${DB_HOST:localhost}` etc., needed so the same jar works both via plain `./gradlew bootRun` and containerized against the `postgres` Compose service name — see `DEPLOYMENT.md`) — on this machine specifically, running `bootRun` directly against Dockerized-infra-only Postgres needs `DB_HOST=[::1]` (brackets included) set to route around the hang. If a fresh clone on a different machine can't connect, this is likely why — just leave `DB_HOST` unset there.

## Backend architecture

Modular monolith under `com.nexcart.backend`, one package per domain: `user`, `product`, `inventory`, `cart`, `order`, `payment`, `notification`, `security`, `config`, `common`. Each module is internally layered `controller → service (interface + Impl) → repository → domain`, with DTOs in `dto/`.

**Cross-module discipline**: a module only ever calls another module through its `Service` interface, never reaches into another module's `Repository` directly — enforced by convention, not tooling, and called out explicitly in code comments where it matters (e.g. `OrderService.markPendingPayment`'s javadoc). The one visible exception is `OrderItemRepository`, which `PaymentServiceImpl` reads directly — treat that as the established precedent if a similar narrow case comes up, not license to skip the Service-interface boundary generally.

**Concurrency patterns actually used** (this is the core subject matter of the project, get these right):
- **Pessimistic locking** (`@Lock(PESSIMISTIC_WRITE)` + a `@Query` repository method, e.g. `InventoryRepository.findByProductIdForUpdate`, `PaymentRepository.findByIdAndUserIdForUpdate`, `OrderRepository.findByIdAndUserIdForUpdate`) for "read-then-mutate-atomically" operations: inventory decrement, payment completion, and payment initiation's per-order dedup.
  - **The trap to know about**: if an entity is read *unlocked* earlier in the same transaction, a later locked re-read of the *same* entity can return the stale object from Hibernate's first-level cache instead of a freshly-locked one — the DB lock is real, the in-memory value checked against it isn't. `InventoryServiceImpl` hit this for real (caused an actual oversell) and fixed it with `entityManager.refresh()`. The safer default demonstrated elsewhere (`PaymentServiceImpl.initiate()`) is: make the locked read the *first and only* read of that entity in the transaction.
- **Optimistic locking** (`@Version` on `Order`) plus an explicit state-machine guard (`OrderServiceImpl.assertTransition`) — a concurrent double-transition fails cleanly with 409 rather than silently clobbering.
- **Idempotency-key pattern** (`Payment.initiate()`): app-level `findByIdempotencyKey` check first (an optimization, not the real guarantee) backed by a DB unique constraint + `saveAndFlush` + catching `DataIntegrityViolationException` as the actual concurrency-safe path — the loser of a race re-queries and returns the winner's row.
- **Real/Fake service abstraction for anything external**, always `@Profile("!test")` real impl + `@Profile("test")` fake impl behind a shared interface: `EventPublisher`/`KafkaEventPublisherImpl`/`FakeEventPublisherImpl` (Kafka), `StorageService`/`B2StorageServiceImpl`/`FakeStorageServiceImpl` (image storage), `RazorpayGatewayClient`/`RazorpayGatewayClientImpl`/`FakeRazorpayGatewayClientImpl` (payment gateway — the fake recomputes Razorpay's real HMAC-SHA256 signature formula against a fixed test secret, so tests exercise genuine signature verification with zero network calls). Follow this pattern for any new external dependency rather than mocking at the test layer.

**Config for external services**: a plain `@Getter @Setter @ConfigurationProperties(prefix = "app.x")` POJO plus a *separate* `@Configuration @EnableConfigurationProperties(XProperties.class)` class that also builds the actual client bean (see `RazorpayProperties`/`RazorpayConfig`, `StorageProperties`/`S3ClientConfig`, `NotificationProperties`/`NotificationConfig`). Client beans always construct successfully even with blank credentials — the corresponding service impl fails clearly (`ApiException`, `SERVICE_UNAVAILABLE`) the first time it's actually used, not at startup. Real secrets are wired as `${ENV_VAR:}` with a blank default and an explanatory comment in `application.properties`, never given a real fallback value.

**Kafka has no autoconfiguration** in this specific Spring Boot 4.1.1 / spring-kafka 4.1.1 combination (confirmed by inspecting both jars — no `AutoConfiguration.imports` entry exists) — it's entirely hand-wired in `config/KafkaConfig.java` (`ProducerFactory`/`ConsumerFactory`/`KafkaTemplate<Object, Object>`/`ConcurrentKafkaListenerContainerFactory`). If Kafka beans seem to be missing, this is why — don't go looking for a missing starter dependency.

**Payment flow (Razorpay, real integration, not simulated)**: `POST /payments/initiate` creates a real Razorpay order and stores its id on the `Payment` row; the frontend hands off to Razorpay's own hosted Checkout modal (no card/UPI form built into this app). `POST /payments/{id}/verify` is the only way a payment becomes `SUCCESS` — it recomputes and checks Razorpay's HMAC signature **server-side, against the order id this server stored at initiate() time, never one the client submits in the request** (a client-submitted order id would let a real signature from an attacker's own cheap transaction be replayed against a more expensive payment — this is the one security property in the whole payment flow that must never be relaxed). A failed/rejected `verify()` call touches no state at all; the order stays retryable. There is currently **no webhook handler** — a payment that succeeds at Razorpay but whose browser callback never fires (tab closed mid-payment) is invisible to this app and needs manual reconciliation. `OrderServiceImpl.markPaymentFailed()` and the `PAYMENT_FAILED`-retry branch in `initiate()` are dormant, kept ready for when a webhook exists — nothing currently drives a `Payment` to `FAILED`.

**Testing**: integration tests (`@SpringBootTest`, `MockMvc`, `@ActiveProfiles("test")`) against H2 in Postgres-compatibility mode (`src/test/resources/application-test.properties`), not a real Postgres. `@BeforeEach` cleans tables in FK-respecting order (children before parents) — copy this order when adding a new entity with foreign keys rather than guessing. There is exactly one Mockito-style unit test in the codebase (`NotificationServiceImplTest`); everything else is a full-stack MockMvc integration test — match whichever style fits what's being tested, but default to the integration-test style already dominant here.

## Frontend architecture

Angular 20, standalone components throughout (no `NgModule`s), signals for state — no NgRx/Akita. `core/services/*.service.ts` each own one piece of app state as a public writable `signal` (e.g. `CartService.cart`, `AuthService.currentUser`) that components read reactively; services sync that signal from the server via HTTP calls, components never mutate it directly. Routes are lazy (`loadComponent`) and guarded via `authGuard` for anything requiring login.

**Design system, all in `frontend/src/styles.css` (global, not per-component)**: CSS custom properties for light/dark theme (`--bg`, `--surface`, `--brand`, `--text`, `--muted`, `--danger`, `--accent`, `--warning`, etc.), toggled via a `[data-theme]` attribute on `<html>` (see `ThemeService` — three-layer cascade: explicit `[data-theme]` > `prefers-color-scheme` > light default). Reused global classes: `.panel` (the one card/surface style), `.primary-btn`/`.secondary-btn`/`.ghost-btn`/`.danger-btn`/`.link-btn`, `.container`/`.page-wrap`/`.section-head`/`.breadcrumb`. Form inputs follow one repeated (not componentized) convention: a `.field` wrapper, `.field-error` beneath an invalid input, `.form-error` for a persistent top-of-form error banner — copy this shape verbatim for new forms rather than inventing a new one. `InrCurrencyPipe` for all money display, `ToastService.success/error/show` for transient feedback (a single toast at a time, not a queue) — persistent form-level errors use `.form-error`, not a toast. **Icons are plain emoji glyphs everywhere** (🛒📦✓✕⌛) — this is the established house style, not a placeholder; don't introduce an SVG icon library for consistency's sake.

**Shared UI components** (`shared/components/`) worth knowing about before rebuilding: `StatusBadgeComponent` (order-status pill, colors keyed by status string), `OrderStepperComponent` (the Cart→Review→Payment→Confirmed progress indicator), `ProductCardComponent`, `ToastComponent`, `ThemeToggleComponent`.

**Cart-clearing is entirely backend-owned**: the cart is deliberately left populated through checkout and stays that way until a payment actually verifies as successful (`PaymentServiceImpl.verify()` calls `cartService.clearCart()`) — a declined or abandoned payment leaves the cart untouched so the user doesn't lose their cart over a failed attempt. The frontend never mutates cart state locally; it only ever calls `CartService.loadCart()` to refetch truth after an action that might have changed it server-side.
