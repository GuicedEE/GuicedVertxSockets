# C4 Component Diagram — WebSocket Message Routing

**Level 3: Component Architecture**

Component view based on current classes (`VertxSocketHttpWebSocketConfigurator`, `GuicedWebSocket`, `WebSocketServerOptions`, `CallScoper`, SPI hooks).

```mermaid
graph TB
    subgraph "Vert.x ServerWebSocket [I/O Boundary]"
        WSOCKET["ServerWebSocket<br/>(per connection)"]
    end

    subgraph "Connection Setup"
        CONFIG["VertxSocketHttpWebSocketConfigurator"]
        OPTIONS["WebSocketServerOptions"]
        GROUPS["groupSockets/groupConsumers<br/>(in-memory)"]
        CALLSCOPE["CallScoper + CallScopeProperties"]
    end

    subgraph "Message Dispatch"
        DISPATCH["GuicedWebSocket"]
        DECODE["ObjectMapper<br/>(JSON → WebSocketMessageReceiver)"]
        LISTENERS["IGuicedWebSocket.getMessagesListeners()<br/>(action→handler map)"]
    end

    subgraph "SPI Hooks (ServiceLoader/Multibinder)"
        ADDGRP["GuicedWebSocketOnAddToGroup"]
        RMGRP["GuicedWebSocketOnRemoveFromGroup"]
        PUBGRP["GuicedWebSocketOnPublish"]
    end

    subgraph "Host Handlers"
        ACTIONH["IWebSocketMessageReceiver impls<br/>(per action)"]
    end

    subgraph "Outbound Paths"
        WRITE["writeTextMessage<br/>(direct)"]
        BROADCAST["EventBus publish<br/>(group fan-out)"]
    end

    %% Flows
    WSOCKET -->|onConnect| CONFIG
    CONFIG -->|validate| OPTIONS
    CONFIG -->|create group consumers| GROUPS
    CONFIG -->|enter scope| CALLSCOPE
    CALLSCOPE -->|bind ServerWebSocket + RequestContextId| DISPATCH

    WSOCKET -->|textMessageHandler| DISPATCH
    DISPATCH -->|decode| DECODE
    DECODE -->|receiver POJO| DISPATCH
    DISPATCH -->|lookup action| LISTENERS
    LISTENERS -->|invoke| ACTIONH

    DISPATCH -->|group add/remove fallback| GROUPS
    DISPATCH -->|SPI override?| ADDGRP
    DISPATCH -->|SPI override?| RMGRP
    DISPATCH -->|SPI override?| PUBGRP

    ACTIONH -->|reply| WRITE
    ACTIONH -->|publish| BROADCAST
    BROADCAST -->|fan-out| GROUPS
    GROUPS -->|write| WRITE
    WRITE -->|frame| WSOCKET

    style WSOCKET fill:#fff3e0,stroke:#e65100,stroke-width:2px
    style CONFIG fill:#e1f5ff,stroke:#01579b,stroke-width:2px
    style OPTIONS fill:#e1f5ff,stroke:#01579b,stroke-width:1px
    style GROUPS fill:#e1f5ff,stroke:#01579b,stroke-width:1px
    style CALLSCOPE fill:#e1f5ff,stroke:#01579b,stroke-width:1px
    style DISPATCH fill:#e1f5ff,stroke:#01579b,stroke-width:2px
    style DECODE fill:#e1f5ff,stroke:#01579b,stroke-width:1px
    style LISTENERS fill:#e8f5e9,stroke:#1b5e20,stroke-width:1px
    style ADDGRP fill:#fce4ec,stroke:#880e4f,stroke-width:1px
    style RMGRP fill:#fce4ec,stroke:#880e4f,stroke-width:1px
    style PUBGRP fill:#fce4ec,stroke:#880e4f,stroke-width:1px
    style ACTIONH fill:#fce4ec,stroke:#880e4f,stroke-width:1px
    style WRITE fill:#fff3e0,stroke:#e65100,stroke-width:1px
    style BROADCAST fill:#fff3e0,stroke:#e65100,stroke-width:1px
```

## Component Responsibilities

### Connection Setup
| Component | Responsibility |
|-----------|----------------|
| **VertxSocketHttpWebSocketConfigurator** | Registers the WebSocket handler, sets up group listeners, scopes connections, and cleans up on close/exception |
| **WebSocketServerOptions** | Validates compression/frame/chunk limits before attaching handler |
| **groupSockets / groupConsumers** | Track active sockets and EventBus consumers per group and per `RequestContextId` |
| **CallScoper + CallScopeProperties** | Enter/exit scoped context; bind `ServerWebSocket` and `RequestContextId` for downstream handlers |

### Message Dispatch
| Component | Responsibility |
|-----------|----------------|
| **GuicedWebSocket** | Decodes JSON payloads to `WebSocketMessageReceiver`, stamps broadcast group, dispatches to action handlers |
| **ObjectMapper** | Deserializes text messages; throws on malformed input |
| **MessagesListeners Map** | Registry (action → receiver handler) leveraged by `IGuicedWebSocket` |

### SPI Hooks
| Hook | Purpose |
|------|---------|
| **GuicedWebSocketOnAddToGroup** | Optional override when joining a group; can short-circuit default group map |
| **GuicedWebSocketOnRemoveFromGroup** | Optional override for removal |
| **GuicedWebSocketOnPublish** | Optional override for broadcast |

### Host Handlers
| Component | Responsibility |
|-----------|----------------|
| **IWebSocketMessageReceiver implementations** | Handle specific actions; may respond directly or publish to groups |

### Outbound Paths
| Path | Behavior |
|------|----------|
| **writeTextMessage** | Direct write to current socket (used by `broadcastMessageSync` and EventBus consumer fan-out) |
| **EventBus publish** | Broadcast to all sockets registered to a group; consumer iterates `groupSockets` |

## Design Notes

- **Scope Discipline**: `processMessageInContext` re-enters `CallScoper` for every frame to ensure scoped beans reflect the current connection.
- **Failure Handling**: Exceptions during hook execution or decoding are wrapped in `WebSocketException` or logged; missing actions generate WARN without interrupting other handlers.
- **Performance**: In-memory group maps favor low-latency single-instance deployments; clustering would require replacing group maps with shared data.
- **Extensibility**: SPI hooks discovered via `ServiceLoader` by default; multibinders allow DI-provided implementations without modifying core code.

---

**See Also**
- [c4-container.md](./c4-container.md) — container view
- [sequence-websocket-lifecycle.md](./sequence-websocket-lifecycle.md) — scope entry/exit flow
- [sequence-message-routing.md](./sequence-message-routing.md) — dispatch flow
- [../IMPLEMENTATION.md](../IMPLEMENTATION.md) — code module map
