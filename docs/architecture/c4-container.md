# C4 Container Diagram — GuicedEE Websockets

**Level 2: Container Architecture**

Reflects the actual containers in the current codebase (Vert.x 5, GuicedEE DI, GuicedWebSocket flow).

```mermaid
graph TB
    subgraph Clients["External"]
        WEB["Web Client<br/>(Browser / Mobile)"]
    end

    subgraph "Vert.x Runtime [Reactive JVM]"
        HTTP["HttpServer<br/>(webSocketHandler)"]
        WSOCKET["ServerWebSocket<br/>(per connection)"]
        EVENTLOOP["Event Loop<br/>(non-blocking)"]
        EVENTBUS["EventBus<br/>(group publish)"]
        WORKER["Shared WorkerExecutor<br/>(websocket-worker-thread)"]
    end

    subgraph "GuicedEE Websockets [Bridge Layer]"
        CONFIG["VertxSocketHttpWebSocketConfigurator<br/>(IGuicePostStartup)"]
        GROUPMAP["groupSockets + groupConsumers<br/>(in-memory per instance)"]
        OPTIONS["WebSocketServerOptions<br/>(DI config)"]
        CALLSCOPE["CallScopeProperties + CallScoper<br/>(RequestContextId)"]
        DISPATCH["GuicedWebSocket<br/>(message decode + dispatch)"]
    end

    subgraph "GuicedEE DI [Scopes & SPI]"
        INJECTOR["Guice Injector<br/>(GuicedEE Core/Web/Client)"]
        MODULE["VertxWebSocketsModule<br/>(bindings)"]
        SPI["SPI Hooks<br/>(GuicedWebSocketOn*, IWebSocketMessageReceiver map)"]
    end

    subgraph "Host Application [Business Logic]"
        HANDLERS["IWebSocketMessageReceiver impls<br/>(per action)"]
        GROUPHOOKS["GuicedWebSocketOnAdd/Remove/Publish<br/>(optional overrides)"]
        SERVICES["Domain Services<br/>(async DB/Auth calls)"]
    end

    WEB -->|HTTP Upgrade + frames| HTTP
    HTTP -->|creates| WSOCKET
    WSOCKET -->|callbacks| EVENTLOOP

    CONFIG -->|registers handler + options| HTTP
    CONFIG -->|populates| GROUPMAP
    CONFIG -->|uses| OPTIONS
    CONFIG -->|scopes| CALLSCOPE

    WSOCKET -->|textMessageHandler| DISPATCH
    DISPATCH -->|ObjectMapper decode + route| HANDLERS
    DISPATCH -->|ServiceLoader fallback| GROUPHOOKS
    DISPATCH -->|publish| EVENTBUS
    EVENTBUS -->|fan-out| WSOCKET

    CALLSCOPE -->|binds ServerWebSocket + RequestContextId| INJECTOR
    MODULE -->|bindings + multibinders| INJECTOR
    INJECTOR -->|injects| HANDLERS
    INJECTOR -->|injects| GROUPHOOKS

    HANDLERS -->|async calls| SERVICES
    SERVICES -->|results| HANDLERS

    style HTTP fill:#fff3e0,stroke:#e65100,stroke-width:2px
    style WSOCKET fill:#fff3e0,stroke:#e65100,stroke-width:2px
    style EVENTLOOP fill:#fff3e0,stroke:#e65100,stroke-width:1px
    style EVENTBUS fill:#fff3e0,stroke:#e65100,stroke-width:1px
    style WORKER fill:#fff3e0,stroke:#e65100,stroke-width:1px
    style CONFIG fill:#e1f5ff,stroke:#01579b,stroke-width:2px
    style DISPATCH fill:#e1f5ff,stroke:#01579b,stroke-width:2px
    style GROUPMAP fill:#e1f5ff,stroke:#01579b,stroke-width:1px
    style OPTIONS fill:#e1f5ff,stroke:#01579b,stroke-width:1px
    style CALLSCOPE fill:#e1f5ff,stroke:#01579b,stroke-width:1px
    style INJECTOR fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px
    style MODULE fill:#e8f5e9,stroke:#1b5e20,stroke-width:1px
    style SPI fill:#e8f5e9,stroke:#1b5e20,stroke-width:1px
    style HANDLERS fill:#fce4ec,stroke:#880e4f,stroke-width:1px
    style GROUPHOOKS fill:#fce4ec,stroke:#880e4f,stroke-width:1px
    style SERVICES fill:#f1f8e9,stroke:#33691e,stroke-width:1px
```

## Container Descriptions

### Vert.x Runtime
| Component | Responsibility |
|-----------|----------------|
| **HttpServer / ServerWebSocket** | Accepts upgrades, wires handlers via `webSocketHandler` |
| **Event Loop** | Executes callbacks; must never block |
| **EventBus** | Broadcast channel for groups; consumers registered per group |
| **WorkerExecutor** | Shared pool for blocking work (currently unused but configured) |

### GuicedEE Websockets Bridge
| Component | Responsibility |
|-----------|----------------|
| **VertxSocketHttpWebSocketConfigurator** | Startup hook; registers WebSocket handler, configures options, attaches listeners and cleanup |
| **groupSockets / groupConsumers** | In-memory tracking of connections and EventBus consumers per group or RequestContextId |
| **WebSocketServerOptions** | Runtime configuration for compression, chunk sizes, and max frames |
| **CallScopeProperties / CallScoper** | Propagate `RequestContextId`, bind `ServerWebSocket` into scope |
| **GuicedWebSocket** | Decodes JSON to `WebSocketMessageReceiver`, routes to message listeners, triggers SPI fallbacks |

### GuicedEE DI
| Component | Responsibility |
|-----------|----------------|
| **VertxWebSocketsModule** | Binds `ServerWebSocket` provider, `IGuicedWebSocket`, and multibinders for SPI hooks |
| **SPI Hooks** | `GuicedWebSocketOnAddToGroup`, `GuicedWebSocketOnRemoveFromGroup`, `GuicedWebSocketOnPublish`, message listener registry |
| **Injector** | Supplies handlers per scoped request; respects `CallScope` |

### Host Application
| Component | Responsibility |
|-----------|----------------|
| **Message Handlers** | Implement `IWebSocketMessageReceiver` keyed by action; handle decoded payloads |
| **Group Hooks** | Override default group lifecycle when provided |
| **Domain Services** | Async integrations (DB/Auth/Cache) invoked by handlers |

## Communication Flow

1. Injector starts; `VertxSocketHttpWebSocketConfigurator` registers `webSocketHandler` and validates `WebSocketServerOptions`.
2. On connection, `CallScoper` enters scope, binds `ServerWebSocket`, sets `RequestContextId`, and registers default group consumers (`EveryoneGroup` + per-connection group).
3. Text frames trigger `processMessageInContext`, which re-enters scope and delegates to `GuicedWebSocket`.
4. `GuicedWebSocket` decodes JSON to `WebSocketMessageReceiver`, stamps `broadcastGroup` to the request context, and dispatches to registered message listeners; missing actions log a warning.
5. Group broadcasts either run SPI overrides or publish to `groupSockets`/EventBus; cleanup removes sockets/consumers on close.

## Boundaries & Risks

| Boundary | Guardrail | Residual Risk |
|----------|-----------|---------------|
| Event loop ↔ blocking work | Use `WorkerExecutor`/`executeBlocking` for heavy I/O | Latency spikes if handlers block directly |
| In-memory group maps ↔ high fan-out | `WebSocketServerOptions.maxGroupSize`; warn on missing group | Memory pressure under large group counts |
| SPI hooks via ServiceLoader ↔ user code | Exceptions wrapped in `WebSocketException` and logged | User handlers may still block or throw |
| JSON decode ↔ malformed input | Jackson exceptions propagated as errors; logged in `receiveMessage` | Malformed frames drop message; client not notified |

---

**See Also**
- [c4-context.md](./c4-context.md) — system context
- [c4-component-websocket.md](./c4-component-websocket.md) — component detail and SPI interaction
- [sequence-websocket-lifecycle.md](./sequence-websocket-lifecycle.md) — connection lifecycle
- [sequence-message-routing.md](./sequence-message-routing.md) — message dispatch
- [../RULES.md](../RULES.md) — constraints and forward-only policy
