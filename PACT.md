# PACT — GuicedEE Websockets

## Agreement & Vision
This document establishes the shared understanding between stakeholders, architects, and implementation teams for the GuicedEE Websockets library.

### Project Identity
- **Name**: GuicedEE Websockets (GuicedVertxSockets)
- **Organization**: GuicedEE
- **Repository**: https://github.com/GuicedEE/GuicedVertxSockets
- **Description**: Provides reactive Websocket functionality for GuicedEE applications via Vert.x integration, enabling real-time bidirectional communication with dependency injection and modular configuration.
- **License**: Apache 2.0

### Core Promise
GuicedEE Websockets bridges Vert.x 5 WebSocket capabilities with the GuicedEE injection framework, offering:
- **Declarative WebSocket lifecycle management** via SPI and dependency injection
- **Type-safe message routing** leveraging Java 25 generics and nullable annotations (@Nullable/@NotNull via JSpecify)
- **Reactive composition** with Vert.x event loops and scope-aware resource cleanup
- **Modular architecture** supporting multi-tenancy, custom message handlers, and group publish/subscribe patterns

### Target Users
1. GuicedEE application developers building real-time features (chat, notifications, dashboards)
2. Library maintainers extending WebSocket behavior via SPI contracts
3. Enterprise teams requiring tested, production-ready reactive WebSocket integration

### Key Principles (from RULES.md)
- **Specification-Driven Design (SDD)**: RULES and GUIDES precede code; contracts are test-driven
- **Forward-Only Changes**: Legacy APIs removed cleanly; migration docs provided
- **Modular Documentation**: Docs-as-Code via topic-scoped glossaries and role-specific guides
- **Fluent API (CRTP)**: Manual fluent setters returning `(S)this` for builder patterns; no Lombok @Builder
- **Java 25 LTS**: Modern language features, records, sealed types, pattern matching
- **Reactive First**: Vert.x async/reactive model throughout

### Stakeholder Commitments
| Role | Commitment |
|------|-----------|
| **Architects** | Define C4 container/component boundaries; validate async scopes and error flows |
| **Developers** | Implement against RULES; inject via GuicedEE patterns; document via living guides |
| **Maintainers** | Review RULES/GUIDES alignment; refactor legacy code under forward-only policy |
| **CI/CD** | GitHub Actions maven-package workflow; automated Java 25 compilation + javadoc |

### Scope Boundaries
**In Scope**:
- WebSocket server setup via Vert.x HttpServer
- Lifecycle hooks: @IGuicePostStartup, @IOnCallScopeEnter, @IOnCallScopeExit
- Message routing via @IWebSocketMessageReceiver (SPI)
- Group publish/subscribe patterns
- JSON serialization via guiced-json-representation

**Out of Scope** (defer to downstream projects or complementary libraries):
- Client-side WebSocket APIs (reactive-client libraries)
- Protocol extensions beyond RFC 6455 (fallback to application layer)
- Custom authentication (use GuicedEE security abstractions)
- Schema evolution strategies (versioning at message envelope level)

### Success Criteria
1. **API Stability**: Exported public classes in `com.guicedee.vertx.websockets` remain stable per SemVer 2.0
2. **Test Coverage**: ≥70% code coverage via JUnit 5 tests
3. **Documentation**: RULES, GUIDES, IMPLEMENTATION, and GLOSSARY kept in sync; links resolve bidirectionally
4. **Performance**: <5ms p99 latency for message delivery on standard VMs (benchmarked in CI)
5. **Adoption**: Integrated into GuicedEE showcase applications and downstream projects

### Architecture Anchors (C4 Context)
```
[External Clients] <--WebSocket--> [Vert.x HttpServer + GuicedEE DI]
                                       |
                                    [WebSocket Handler]
                                       |
                                 [SPI: IWebSocketMessageReceiver]
                                 [SPI: GuicedWebSocketOnAddToGroup, ...]
                                       |
                                 [Application Modules]
```

### Roadmap (High-Level)
**Phase 1 (Current)**: Stabilize reactive WebSocket lifecycle; document RULES, API surface, and C4 architecture
**Phase 2**: Add performance benchmarks; extend message serialization strategies
**Phase 3**: Integrate with GuicedEE observability (tracing, metrics)

### References
- [RULES.md](./RULES.md) — Technical and behavioral constraints
- [GLOSSARY.md](./GLOSSARY.md) — Canonical terminology and prompt language alignment
- [GUIDES.md](./GUIDES.md) — How-to guides and design patterns
- [IMPLEMENTATION.md](./IMPLEMENTATION.md) — Code layout, module structure, and back-links
- [rules/generative/backend/guicedee/README.md](./rules/generative/backend/guicedee/README.md) — GuicedEE framework rules
- [rules/generative/backend/vertx/README.md](./rules/generative/backend/vertx/README.md) — Vert.x reactive patterns

### Review & Approval
- **Created**: 2025-11-29
- **Last Reviewed**: —
- **Approved By**: —
- **Next Review**: —
