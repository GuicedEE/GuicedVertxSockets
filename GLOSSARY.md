# GLOSSARY — GuicedEE Websockets

## Overview
This glossary is composed using the **topic-first precedence model**: topic-scoped glossaries (from the Rules Repository) take precedence over root terms. This document acts as an index and minimal aggregator, with enforced Prompt Language Alignment mappings copied here; all other terms link to topic glossaries.

## Glossary Precedence Policy
1. **Topic glossaries override root**: For any term defined in a topic's GLOSSARY.md (e.g., Vert.x, GuicedEE, JSpecify), use that definition.
2. **Root glossary as index**: This document links to each topic's glossary and provides canonical mappings for Prompt Language Alignment (e.g., CRTP fluent API naming).
3. **No duplication**: Full definitions appear only once; related terms link across scopes.

---

## Core Concepts (GuicedEE Websockets)

### WebSocket (Vert.x)
The full-duplex communication protocol (RFC 6455) implemented by Vert.x's `io.vertx.core.http.ServerWebSocket`. See [rules/generative/backend/vertx/README.md](./rules/generative/backend/vertx/README.md) for Vert.x terminology and lifecycle.

### Message Receiver
A service-provider interface (SPI) implementing `IWebSocketMessageReceiver`. Receives decoded messages from a WebSocket and routes them to application logic. Scoped to the call-activation (request-response lifecycle).

**Aliases**: Handler, Endpoint, Route

### Message Handler  
An application-defined function or method that processes a received WebSocket message, typically decorated with `@Receiver` or implementing `IWebSocketMessageReceiver`. May emit responses or group-published messages.

### Group (Publish/Subscribe)
A named collection of WebSocket connections. Operations:
- **Add**: `GuicedWebSocketOnAddToGroup` SPI
- **Remove**: `GuicedWebSocketOnRemoveFromGroup` SPI
- **Publish**: `GuicedWebSocketOnPublish` SPI

Implemented via Vert.x EventBus or shared state; transient lifecycle per server instance.

### Scope (Request/Activation)
A bound context for a WebSocket message or HTTP request lifetime. Injected beans are scoped to `@IOnCallScopeEnter`/`@IOnCallScopeExit`. See [rules/generative/backend/guicedee/inject/README.md](./rules/generative/backend/guicedee/inject/README.md).

### Fluent API (CRTP)
Manual builder pattern using Curiously Recurring Template Pattern (CRTP). Methods return `(S)this` (self type) to enable chaining:
```java
public class SocketConfig<S extends SocketConfig<S>> {
    public S withMaxFrameSize(int size) { /* ... */ return (S)this; }
}
```
**Policy**: Always use CRTP for GuicedEE Websockets; do not use Lombok @Builder.
**Reference**: [rules/generative/backend/fluent-api/crtp.rules.md](./rules/generative/backend/fluent-api/crtp.rules.md)

### Nullability Annotations
Use JSpecify `@Nullable` and `@NotNull` (not `@Nonnull`). See [rules/generative/backend/jspecify/README.md](./rules/generative/backend/jspecify/README.md).
**Policy**: Mark all public parameters and return types in the WebSocket API.

### Lombok Usage
Lombok is used for boilerplate (getters, setters, constructors) **only**. Do NOT use @Builder; use CRTP instead.
**Annotations**: @Data, @Getter, @Setter, @RequiredArgsConstructor, @AllArgsConstructor
**Forbidden**: @Builder, @Singular

---

## Topic-Scoped Glossaries (Linked)

### Backend & Framework

| Topic | Glossary Link | Applies To |
|-------|---------------|-----------|
| **Vert.x 5 (Reactive)** | [rules/generative/backend/vertx/README.md](./rules/generative/backend/vertx/README.md) | Event loops, handlers, async/await, HttpServer, ServerWebSocket |
| **GuicedEE Core** | [rules/generative/backend/guicedee/README.md](./rules/generative/backend/guicedee/README.md) | Dependency injection, binding, multibindings, module interfaces |
| **GuicedEE Web** | [rules/generative/backend/guicedee/web/README.md](./rules/generative/backend/guicedee/web/README.md) | Web request interception, Vert.x routing, context propagation |
| **Fluent API (CRTP)** | [rules/generative/backend/fluent-api/crtp.rules.md](./rules/generative/backend/fluent-api/crtp.rules.md) | Builder patterns, method chaining, self-types |
| **JSpecify Nullability** | [rules/generative/backend/jspecify/README.md](./rules/generative/backend/jspecify/README.md) | @Nullable, @NotNull annotations, non-null-by-default |
| **Logging** | [rules/generative/structural/logging/README.md](./rules/generative/structural/logging/README.md) | SLF4J, log levels, structured logging |

### Language & Tooling

| Topic | Glossary Link | Applies To |
|-------|---------------|-----------|
| **Java 25 LTS** | [rules/generative/language/java/java-25.rules.md](./rules/generative/language/java/java-25.rules.md) | Records, sealed classes, pattern matching, var inference |
| **Maven Build** | [rules/generative/language/java/build-tooling.md](./rules/generative/language/java/build-tooling.md) | pom.xml structure, plugins, artifact coordinates |

### DevOps & Observability

| Topic | Glossary Link | Applies To |
|-------|---------------|-----------|
| **GitHub Actions CI/CD** | [rules/generative/platform/ci-cd/providers/github-actions.md](./rules/generative/platform/ci-cd/providers/github-actions.md) | Workflows, jobs, secrets, maven-package |

---

## Prompt Language Alignment

### Enforced Mappings (copy from source)
None currently enforced at root level. All component/API terms link to their topic glossaries or RULES.md.

---

## Cross-Reference Index

### By Role

**Architects**:
- PACT (vision & scope)
- RULES.md (constraints)
- Architecture diagrams (docs/architecture/)

**Developers**:
- GLOSSARY (this file, terminology)
- GUIDES.md (how-to and examples)
- Topic GLOSSARY files (framework details)

**Maintainers**:
- RULES.md & GUIDES.md (keeping in sync)
- IMPLEMENTATION.md (module map)
- Forward-Only Policy in RULES.md section 6

### By Question

| Question | See |
|----------|-----|
| What is a WebSocket in this project? | "WebSocket (Vert.x)" above + [Vert.x glossary](./rules/generative/backend/vertx/README.md) |
| How do I inject dependencies? | [GuicedEE Core glossary](./rules/generative/backend/guicedee/README.md) + GUIDES.md |
| What fluent API style should I use? | GLOSSARY "Fluent API (CRTP)" + [CRTP rules](./rules/generative/backend/fluent-api/crtp.rules.md) |
| Which Java version? | Java 25 LTS; see [rules/generative/language/java/java-25.rules.md](./rules/generative/language/java/java-25.rules.md) |
| How do I null-check? | [JSpecify glossary](./rules/generative/backend/jspecify/README.md); use @Nullable/@NotNull |
| How do I add a new message handler? | GUIDES.md → "Implementing a Message Receiver" |

---

## See Also
- [PACT.md](./PACT.md) — Vision and stakeholder agreement
- [RULES.md](./RULES.md) — Technical and behavioral constraints
- [GUIDES.md](./GUIDES.md) — How-to guides and design patterns
- [IMPLEMENTATION.md](./IMPLEMENTATION.md) — Code layout and module structure
- [docs/architecture/README.md](./docs/architecture/README.md) — Architecture diagrams (C4, sequences, ERDs)
- [docs/PROMPT_REFERENCE.md](./docs/PROMPT_REFERENCE.md) — Selected stacks and diagram links
