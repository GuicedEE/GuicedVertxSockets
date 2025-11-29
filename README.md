# GuicedEE Websockets (GuicedVertxSockets)

Lightweight WebSocket support for GuicedEE applications powered by Vert.x 5.x and a documentation-first engineering model.

## Table of Contents
- [Quick Links](#quick-links)
- [Features](#features)
- [Getting Started](#getting-started)
- [Architecture & Documentation](#architecture--documentation)
- [Development Workflow](#development-workflow)
- [Release & Versioning](#release--versioning)
- [Community & Contribution](#community--contribution)
- [License](#license)
- [Related Projects](#related-projects)

## Quick Links
- **Vision & Scope**: [PACT.md](./PACT.md)
- **Rules & Constraints**: [RULES.md](./RULES.md)
- **Guides**: [GUIDES.md](./GUIDES.md)
- **Implementation Roadmap**: [IMPLEMENTATION.md](./IMPLEMENTATION.md)
- **Design Validation**: [DESIGN_VALIDATION.md](./DESIGN_VALIDATION.md)
- **Stage 3+ Plans**: [STAGE_3_IMPLEMENTATION_PLAN.md](./STAGE_3_IMPLEMENTATION_PLAN.md)
- **Stage 4 Completion**: [STAGE_4_COMPLETION.md](./STAGE_4_COMPLETION.md)
- **Architecture Diagrams**: [docs/architecture/](./docs/architecture/)

## Features
- Vert.x RFC 6455 WebSocket handling with built-in reactive support.
- GuicedEE dependency injection anchoring scope-aware services and lifecycle hooks.
- SPI-driven receivers, lifecycle listeners, and configurators for extensibility.
- Fluent builders (CRTP) expose configuration and deployment options without leaking implementation details.
- JSpecify nullability annotations for clarity and safety in critical flows.
- Documentation-first process ensures design, validation, and implementation stay aligned.

## Getting Started

### Requirements
- Java 25 LTS
- Apache Maven
- GuicedEE 2.x compatible environment

### Installation

Add the artifact to your `pom.xml`:

```xml
<dependency>
    <groupId>com.guicedee</groupId>
    <artifactId>guiced-vertx-sockets</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

### Basic SPI Example

```java
import com.guicedee.client.services.websocket.IWebSocketMessageReceiver;
import io.vertx.core.http.ServerWebSocket;
import org.jspecify.annotations.NotNull;

public class ChatMessageReceiver implements IWebSocketMessageReceiver {
    @Override
    public void onMessage(@NotNull ServerWebSocket webSocket, @NotNull Object message) {
        webSocket.writeTextMessage("Message received");
    }
}
```

Register the receiver in a GuicedEE module:

```java
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.guicedee.client.services.websocket.IWebSocketMessageReceiver;

public class WebSocketConfigModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<IWebSocketMessageReceiver> binder =
            Multibinder.newSetBinder(binder(), IWebSocketMessageReceiver.class);
        binder.addBinding().to(ChatMessageReceiver.class);
    }
}
```

See [GUIDES.md](./GUIDES.md) for handler examples, group management, and test strategies.

## Architecture & Documentation

- **Vert.x HTTP Server** handles WebSocket lifecycles and frame syndication.
- **GuicedEE DI** injects configurators, event listeners, and lifecycle-aware services.
- **SPI Contracts** allow third-party receivers and fallback behaviors without tight coupling.
- **C4/Sequence** diagrams, ERDs, and domain models live in `docs/architecture/`.
- **Implementation reference**: [IMPLEMENTATION.md](./IMPLEMENTATION.md)

## Development Workflow

### Build

```bash
mvn clean package
```

### Testing

```bash
mvn clean test
```

### Documentation

- Architecture diagrams: `docs/architecture/*.md`
- Design rules: `RULES.md`
- How-to guides: `GUIDES.md`
- Implementation details: `IMPLEMENTATION.md`

### Engineering Policies

- Forward-only change model (see [RULES.md](./RULES.md) §6): breaking changes raise major versions, removals require docs updates, no rollbacks.
- Docs-first approach: All behavioral changes must be surfaced through docs before code updates.
- Generated artifacts stay aligned with RULES/GUIDES; hand edits require justification in the source docs.

## Release & Versioning

- Current artifact coordinates: `com.guicedee:guiced-vertx-sockets:2.0.0-SNAPSHOT`.
- Stage status: [Stage 4 (Code & Scaffolding) Complete](./STAGE_4_COMPLETION.md) — next is Stage 5 (Testing & Release).
- Breaking changes follow SemVer 2.0; document removals in `MIGRATION.md` when applicable.
- CI/CD and release automation conform to docs-first and forward-only rules (see [RULES.md](./RULES.md)).

## Community & Contribution

- Keep contributions aligned with [RULES.md](./RULES.md) and [GUIDES.md](./GUIDES.md).
- Open issues and PRs should describe the docs updates that justify behavioral changes.
- Prefer smaller, focused changes; large refactors must include updated implementation plan sections.
- Questions or design discussions can be opened via GitHub issues referencing the relevant docs.

## License

Apache 2.0 — see [LICENSE](./LICENSE)

## Related Projects

- [GuicedEE/GuicedInjection](https://github.com/GuicedEE/GuicedInjection) — Core DI framework
- [GuicedEE/GuicedVertxWeb](https://github.com/GuicedEE/GuicedVertxWeb) — Vert.x Web integration
- [Vert.x Documentation](https://vertx.io/docs)

**Last Updated**: 2025-11-29  
**Stage**: [Stage 4 (Code & Scaffolding) Complete](./STAGE_4_COMPLETION.md) — Ready for Stage 5 (Testing & Release)
