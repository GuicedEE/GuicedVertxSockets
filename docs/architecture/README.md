# Architecture Documentation — GuicedEE Websockets

This directory holds the docs-as-code architecture set for the GuicedEE Websockets library. All diagrams mirror current code and follow the C4 model plus flow/ERD views.

## Diagram Index

### C4 Model

| Diagram | File | Purpose |
|---------|------|---------|
| **Level 1 (Context)** | [c4-context.md](./c4-context.md) | External actors, frameworks, trust boundaries |
| **Level 2 (Container)** | [c4-container.md](./c4-container.md) | Vert.x runtime, DI bridge, group maps, EventBus |
| **Level 3 (Component)** | [c4-component-websocket.md](./c4-component-websocket.md) | `VertxSocketHttpWebSocketConfigurator`, `GuicedWebSocket`, SPI hooks |

### Supplementary Diagrams

| Diagram | File | Purpose |
|---------|------|---------|
| **Connection Lifecycle** | [sequence-websocket-lifecycle.md](./sequence-websocket-lifecycle.md) | Scope entry/exit, group registration, cleanup |
| **Message Routing** | [sequence-message-routing.md](./sequence-message-routing.md) | Decode → dispatch → publish flow for text frames |
| **Runtime ERD** | [erd-websocket-model.md](./erd-websocket-model.md) | In-memory group maps, handler registry, SPI hooks |

## Dependency & Integration Map (Stage 1)

| Area | Dependency | Notes |
|------|------------|-------|
| Runtime | Vert.x 5 (`HttpServer`, `ServerWebSocket`, `EventBus`) | Non-blocking event loop; EventBus for group fan-out |
| DI/Scopes | GuicedEE Core/Web/Client | `CallScoper`, `CallScopeProperties`, `IGuiceContext` |
| Serialization | Jackson `ObjectMapper` | Singleton; decodes JSON to `WebSocketMessageReceiver` |
| Config | `WebSocketServerOptions` | Compression, frame/chunk size, max group size validation |
| SPI | `GuicedWebSocketOn*`, `IWebSocketMessageReceiver` | ServiceLoader + DI multibinders; user-provided implementations |

## Threat Model Summary (Stage 1)

- **Untrusted entry**: WebSocket frames and upgrade requests from clients. Guard with frame size limits, compression settings, and JSON decode try/catch.
- **Event loop protection**: Handlers must not block; heavy work should use `WorkerExecutor` or `executeBlocking`.
- **In-memory group maps**: Per-instance only; protect against runaway group size via `maxGroupSize` and WARN when creating missing groups.
- **SPI hook isolation**: User hooks can short-circuit group ops; wrap failures in `WebSocketException` and log.
- **External calls**: Handlers call out to services; propagate auth/context in handler code and keep async.

## Reading Guide

- **New contributors**: Start with [c4-context.md](./c4-context.md) then [c4-container.md](./c4-container.md) to see runtime boundaries.
- **Implementers**: Use [c4-component-websocket.md](./c4-component-websocket.md) to place changes, then follow the sequences for lifecycle and routing.
- **Reviewers**: Validate threats/controls above against RULES and the code paths shown in sequences.

## Diagram Sources

- Format: Mermaid fenced blocks inside the `.md` source.
- Location: `docs/architecture/*.md` (sources are authoritative). Rendered images (if produced) belong in `docs/architecture/img/` but are not canonical.

## References

- [../PACT.md](../PACT.md) — Vision and scope
- [../RULES.md](../RULES.md) — Constraints and forward-only policy
- [../GLOSSARY.md](../GLOSSARY.md) — Topic-first terms and prompt alignment
- [../GUIDES.md](../GUIDES.md) — How-to guides
- [../IMPLEMENTATION.md](../IMPLEMENTATION.md) — Module map and SPI pointers
- [../PROMPT_REFERENCE.md](../PROMPT_REFERENCE.md) — Stack selections and diagram links

### Enterprise Rules
- [../../rules/generative/backend/vertx/README.md](../../rules/generative/backend/vertx/README.md)
- [../../rules/generative/backend/guicedee/README.md](../../rules/generative/backend/guicedee/README.md)
- [../../rules/generative/architecture/README.md](../../rules/generative/architecture/README.md)

## Updating These Diagrams

1. Edit the Mermaid source (e.g., [c4-container.md](./c4-container.md)).
2. Commit the source; optional rendered images go under `docs/architecture/img/`.
3. Keep names aligned with code (`VertxSocketHttpWebSocketConfigurator`, `GuicedWebSocket`, etc.).

Conventions: Use action verbs for relationships, mark async/event-loop boundaries, and keep component names in PascalCase to match Java classes.
