# Why TradeSentry Uses These Technologies

This page explains the main technologies in TradeSentry in business-friendly terms: what each one does, why it suits this project, and what would change if it were not present.

TradeSentry is an AML (anti-money-laundering) monitoring system. It receives a transaction, screens it for obvious risk, investigates flagged activity, and records the outcome. The technology choices support three key needs: reliable record keeping, independent processing stages, and fast access to investigation data.

## At a glance

| Technology | What it does here | Why it is useful |
|---|---|---|
| Java 21 | Runs all backend services | Mature, safe language for long-running financial workflows |
| Spring Boot | Provides the application framework | Makes services, APIs, configuration, and integrations consistent |
| PostgreSQL | Stores transactions and their status | Gives a durable, queryable record of each transaction |
| Apache Kafka | Moves events between workflow stages | Lets intake, screening, and investigation work independently |
| gRPC + Protocol Buffers | Connects investigation to case-data service | Gives a strict, efficient internal data contract |
| Docker + Docker Compose | Runs the full system locally in containers | Makes the multi-service environment repeatable |
| Maven | Builds, tests, and packages the Java modules | Keeps dependencies and builds consistent |
| JUnit | Tests the decision graph | Helps prevent unintended decision-flow changes |

## Java 21

**What it does:** Java is the language used to implement `core-service`, `agent-service`, and `case-data-service`.

**Why it is needed:** Financial-monitoring services normally run continuously and process important business data. Java provides strong typing, predictable runtime behaviour, good performance, and a large ecosystem for web APIs, databases, messaging, and testing. Java 21 also supports concise immutable data structures, which are useful for transaction events and investigation state.

**With Java:** The system has a structured, maintainable backend implementation with mature libraries for every integration it needs.

**Without Java:** The project would need a different programming language and equivalent libraries. The workflow could still exist, but all services would need to be rebuilt and the current Spring-based implementation would not run.

## Spring Boot

**What it does:** Spring Boot is the framework that starts each service and wires together its components. It powers the REST API, database access, Kafka listeners, validation, configuration, and gRPC integration.

**Why it is needed:** Each service needs many standard capabilities: accepting HTTP requests, reading configuration, connecting to PostgreSQL, publishing Kafka messages, and running background consumers. Spring Boot supplies these in a conventional way, so the project can focus on AML rules and investigation logic rather than infrastructure plumbing.

**With Spring Boot:** Services are easier to start, configure, test, and extend. Common concerns such as dependency injection and application configuration follow one consistent pattern.

**Without Spring Boot:** The team would manually configure HTTP servers, database connections, message consumers, object serialization, lifecycle management, and dependency wiring. The project would contain much more setup code and be harder to maintain.

## Spring Web and REST

**What it does:** Spring Web exposes the transaction intake API in `core-service`.

The public endpoints are:

| Endpoint | Role |
|---|---|
| `POST /api/transactions` | Accept a new transaction for monitoring |
| `GET /api/transactions/{id}` | Check its current processing status |

**Why it is needed:** REST gives another banking system, a UI, or a test client a simple standard way to submit transactions and follow their progress.

**With REST:** External users and systems have a clear front door to the platform.

**Without REST:** Transactions would have to be inserted directly into a database or Kafka topic, which is less safe, less discoverable, and bypasses validation and lifecycle handling.

## Jakarta Validation

**What it does:** Validates incoming transaction requests before processing them. For example, account ID and currency must be present, and amount must be positive.

**Why it is needed:** AML decisions are only as dependable as the data entering the system. Rejecting obviously invalid requests prevents avoidable processing errors and misleading investigations.

**With validation:** Bad input is rejected at the edge of the system.

**Without validation:** Invalid or incomplete transaction data could be saved and passed into downstream screening and investigation, creating failures or unreliable results later.

## PostgreSQL, JPA, and Hibernate

**What they do:** PostgreSQL is the database. Spring Data JPA and Hibernate map the Java `Transaction` object to the `transactions` table and handle ordinary database reads and writes.

**Why they are needed:** The system needs a durable source of truth for a transaction and its lifecycle status: `SUBMITTED`, `CLEARED_EARLY`, `FLAGGED`, or `ADJUDICATED`. Kafka transports events, but it is not the primary query interface for the transaction-status API.

**With PostgreSQL:** A caller can submit a transaction, later ask for it by ID, and see its latest persisted status even after a service restart.

**Without PostgreSQL:** The system would lose its straightforward durable transaction record. Status checks would be difficult, historical reporting would be limited, and a restart could lose in-memory data.

**Why JPA/Hibernate matter:** They remove repetitive SQL and mapping code for the current simple transaction model. For highly specialised reporting queries, explicit SQL can still be added later.

## Apache Kafka and Spring Kafka

**What they do:** Kafka is the system's internal event pipeline. Spring Kafka lets the Java services publish and consume those events.

The important topics are:

| Topic | Meaning |
|---|---|
| `transactions.ingested` | A transaction was accepted |
| `transactions.flagged` | Screening found suspicious indicators |
| `transactions.adjudicated` | The investigation reached a decision |
| `transactions.dlq` | Reserved for future failed-message handling |

**Why they are needed:** Transaction intake, rule screening, audit, investigation, and case management should not all have to finish inside one HTTP request. Kafka lets each stage do its own work after receiving an event. If investigation is slow, intake can still accept new transactions.

Kafka also supports **fan-out**. The screening component and audit component can each independently receive every ingested transaction. It preserves order for an individual account because messages are keyed by `accountId`.

**With Kafka:** The pipeline is asynchronous, stages are loosely coupled, and separate services can be scaled or restarted independently. Messages provide an operational trail between workflow stages.

**Without Kafka:** The core service would need to call every later service directly and wait for them. A slow or unavailable investigation service could block transaction intake. Adding audit or another consumer would require changing the main processing code.

## gRPC and Protocol Buffers

**What they do:** The agent service uses gRPC to ask case-data-service for account history and similar previous cases. Protocol Buffers define the shared request and response contract in `proto/src/main/proto/casedata.proto` and generate Java client/server code during the build.

There are two calls:

| RPC | Role |
|---|---|
| `GetAccountHistory` | Returns one account-history summary |
| `RetrieveSimilarCases` | Streams several similar historical cases |

**Why they are needed:** Investigation needs supporting information before it can score risk. gRPC gives a well-defined internal interface so the agent and case-data services can be developed and deployed separately while agreeing on exactly what data is exchanged.

**With gRPC/Protobuf:** Calls are typed, compact, and contract-first. The generated code reduces mismatches between client and server. Server streaming is a natural fit for returning multiple similar cases.

**Without gRPC/Protobuf:** The agent could call a REST endpoint or query a shared database instead. REST would work but would need a separately maintained JSON contract; a shared database would tightly couple services and make future changes harder.

## The custom investigation state graph

**What it does:** The investigation agent is organised as a small state graph rather than one long method. It follows this route:

```text
enrich -> retrieve similar cases -> assess -> decide
                                      |
                                      +-> investigate deeper -> assess
```

The deeper-investigation loop is used only for borderline scores and is capped at two passes. A general step limit also prevents accidental infinite loops.

**Why it is needed:** AML investigations are naturally multi-step decisions. Keeping each step separate makes the flow easier to explain, test, and later improve—for example, by replacing deterministic scoring with an approved AI-assisted assessment component.

**With the state graph:** The investigation sequence is explicit, state is immutable, and individual nodes can be unit-tested.

**Without it:** Investigation logic would likely become a large conditional block. It would be harder to see the decision path, safely introduce loops or branches, and test each step independently.

## Docker and Docker Compose

**What they do:** Docker packages each service with its runtime. Docker Compose starts PostgreSQL, Kafka, core-service, agent-service, and case-data-service together with the correct network addresses and startup dependencies.

**Why they are needed:** This is a distributed project: it cannot be demonstrated fully by starting one application. Containers create the same local environment for each developer and avoid installing or configuring Kafka and PostgreSQL directly on every machine.

**With Docker Compose:** A developer can run a close-to-production local environment consistently. The services communicate through predictable container names such as `kafka`, `postgres`, and `case-data-service`.

**Without Docker Compose:** Every developer would need to install, configure, start, and connect the services manually. Differences in local setups would make onboarding and troubleshooting more difficult.

## Maven and the multi-module structure

**What it does:** Maven builds the project, downloads dependencies, runs tests, generates gRPC code, and packages each service as a runnable jar. The parent project coordinates four modules: `proto`, `core-service`, `agent-service`, and `case-data-service`.

**Why it is needed:** The services share build conventions but must be independently packaged. The `proto` module ensures generated gRPC code comes from one authoritative contract.

**With Maven:** Dependency versions, Java version, build commands, and generated contracts are controlled in one repeatable build system.

**Without Maven:** The team would manually manage library jars, compilation order, generated code, tests, and packaging. This becomes error-prone as services grow.

## JUnit testing

**What it does:** JUnit tests the generic state-graph engine and investigation outcomes, including normal paths, conditional routing, bounded loops, and escalation scenarios.

**Why it is needed:** Small changes to risk scoring or routing can materially change the result of an AML decision. Tests make expected behaviour explicit and highlight regressions before deployment.

**With tests:** The project can confidently verify that a strong-risk scenario escalates, a weak-risk scenario clears, and graph safety limits work.

**Without tests:** A future change could silently alter decisions or create a looping investigation flow, and the error might only be found during manual testing or after deployment.

## Logging

**What it does:** All services log major events: ingestion, screening result, gRPC enrichment, investigation score, decision, audit receipt, and case-management action.

**Why it is needed:** AML processing must be explainable. Logs let developers and operators trace what the system did for a transaction while the prototype is running.

**With logging:** It is easier to diagnose failures and follow an individual transaction through the pipeline.

**Without logging:** A distributed asynchronous flow becomes much harder to debug because work happens across multiple services and at different times.

> Important: application logs are helpful for development, but production AML audit records should be stored securely and immutably rather than relying only on standard service logs.

## What is intentionally not included yet

Some components are represented as future work rather than production features:

- **Real case-data integration:** case-data-service currently produces deterministic sample data rather than reading real banking or case-management data.
- **Persistent SAR cases:** an `ESCALATE` decision is logged as a case-filing action; it does not yet create a durable SAR/case record in an external system.
- **Dead-letter handling:** the Kafka DLQ topic exists, but retries and routing failed events to it are not yet implemented.
- **Security:** there is no authentication, user-role control, or encryption configuration in the application layer yet.
- **Outbox/reliable publication:** saving a transaction and publishing its Kafka event are not yet protected by an outbox pattern, so a production version should strengthen this consistency boundary.
- **LLM integration:** the agent is intentionally deterministic today. This makes its prototype decisions predictable and testable; any future AI component should be governed, explainable, and reviewed for AML use.

