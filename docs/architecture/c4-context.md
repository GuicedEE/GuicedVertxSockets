# C4 Context Diagram — GuicedEE Websockets

**Level 1: System Context**

Context of the GuicedEE Websockets library inside a GuicedEE host application. Aligned to current source (`GuicedWebSocket`, `VertxSocketHttpWebSocketConfigurator`, `WebSocketServerOptions`).

```mermaid
graph TB
    WEB["Web Clients<br/>(Browsers, Mobile, Devices)"]
    SYSTEM["GuicedEE Websockets<br/>[Java 25 / Vert.x 5]<br/>com.guicedee.vertx.websockets"]
    DI["GuicedEE Core/Web/Client<br/>[DI, Scopes, SPI]"]
    VERTX["Vert.x Runtime<br/>[HttpServer, EventBus]"]
    APP["Host Application Modules<br/>(SPI handlers, business logic)"]
    EXTERNAL["External Services<br/>(Auth, DB, APIs)"]

    WEB -->|WebSocket RFC 6455| SYSTEM
    SYSTEM -->|JSON frames| WEB

    SYSTEM -->|Injector, scopes| DI
    SYSTEM -->|Event loop + EventBus| VERTX
    SYSTEM -->|SPI contracts| APP
    APP -->|Async calls| EXTERNAL

    style SYSTEM fill:#e1f5ff,stroke:#01579b,stroke-width:2px
    style WEB fill:#f3e5f5,stroke:#4a148c,stroke-width:1px
    style DI fill:#e8f5e9,stroke:#1b5e20,stroke-width:1px
    style VERTX fill:#fff3e0,stroke:#e65100,stroke-width:1px
    style APP fill:#fce4ec,stroke:#880e4f,stroke-width:1px
    style EXTERNAL fill:#f1f8e9,stroke:#33691e,stroke-width:1px
```

## System Responsibilities

| Actor / System | Responsibility |
|----------------|----------------|
| **Web Clients** | Initiate WebSocket upgrades and exchange JSON frames |
| **GuicedEE Websockets** | Owns connection lifecycle, group membership, message dispatch, and broadcasting |
| **GuicedEE (DI/Scopes)** | Provides injector, `CallScoper`, `CallScopeProperties`, and SPI discovery |
| **Vert.x Runtime** | Supplies `HttpServer`, `ServerWebSocket`, worker executors, and EventBus |
| **Host Application Modules** | Supply `IWebSocketMessageReceiver` handlers and optional group hooks (`GuicedWebSocketOn*`) |
| **External Services** | Called from handlers for persistence, auth, and integrations |

## Trust Boundaries & Threat Surface

| Boundary | Trust Level | Notes / Controls |
|----------|-------------|------------------|
| Web client ↔ Vert.x upgrade | Untrusted input | Validate frame size via `WebSocketServerOptions`; reject oversized/invalid frames |
| Vert.x event loop ↔ Host handlers | Trusted API, untrusted payloads | All decoding goes through `ObjectMapper`; handler exceptions logged; avoid blocking the event loop |
| EventBus ↔ Group broadcasts | Internal but shared | Guard against group fan-out spikes; `groupSockets` is in-memory per instance |
| Host handlers ↔ External services | Depends on service | Use async clients; propagate auth context in handler code |

## Key Interactions

1. Client upgrades HTTP → WebSocket; Vert.x creates `ServerWebSocket`.
2. `VertxSocketHttpWebSocketConfigurator` scopes the connection (`CallScoper`), registers EventBus consumers, and attaches message handlers.
3. Incoming JSON frames are decoded and routed through `GuicedWebSocket` to registered `IWebSocketMessageReceiver` implementations.
4. Group operations (`addToGroup`, `removeFromGroup`, `broadcastMessage`) use SPI hooks when present, otherwise fall back to in-memory `groupSockets` + EventBus publish.
5. External calls run inside handlers; blocking work must leave the event loop (worker executor).

## Technology Stack (Visible Here)

- Protocol: WebSocket (RFC 6455) over HTTP/HTTPS
- Runtime: Java 25 LTS, Vert.x 5.x (non-blocking)
- DI/Scopes: GuicedEE Core/Web/Client
- Serialization: Jackson `ObjectMapper`
- Build: Maven; JPMS enabled

---

**See Also**
- [c4-container.md](./c4-container.md) — container-level flow and boundaries
- [../RULES.md](../RULES.md) — behavioral and technical constraints
- [../../rules/generative/backend/vertx/README.md](../../rules/generative/backend/vertx/README.md) — Vert.x async guidance
