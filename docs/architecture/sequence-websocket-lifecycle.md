# Sequence Diagram — WebSocket Connection Lifecycle

Critical flow from HTTP upgrade through scope management, group registration, steady-state messaging, and cleanup (mirrors `VertxSocketHttpWebSocketConfigurator` + `GuicedWebSocket`).

```mermaid
sequenceDiagram
participant Client as Web Client
participant HttpServer as Vert.x HttpServer
participant Config as VertxSocketHttpWebSocketConfigurator
participant Scope as CallScoper and CallScopeProperties
participant Dispatcher as GuicedWebSocket
participant EventBus as Vert.x EventBus
participant Groups as groupSockets and groupConsumers
Client->>HttpServer: HTTP GET upgrade to WebSocket
HttpServer->>Config: webSocketHandler(ServerWebSocket ctx)
activate Config
Config->>Scope: enter and bind ctx as ServerWebSocket
Scope-->>Scope: set RequestContextId to ctx.textHandlerID()
Config->>Groups: configureGroupListener EveryoneGroup and ctx
Config->>Groups: configureGroupListener RequestContextId and ctx
deactivate Config
Note over Client,Dispatcher: Connection ready and scoped context live

Client->>HttpServer: send text frame JSON
HttpServer->>Dispatcher: textMessageHandler(msg)
activate Dispatcher
Dispatcher->>Scope: enter and bind ctx and properties
Dispatcher->>Dispatcher: receiveMessage(msg)
Dispatcher->>Dispatcher: decode with ObjectMapper to WebSocketMessageReceiver
Dispatcher-->>Dispatcher: messageReceived with broadcastGroup RequestContextId
alt action registered
    Dispatcher->>Dispatcher: lookup messagesListeners.get(action)
    Dispatcher->>Dispatcher: invoke handler Uni
else no action
    Dispatcher->>Dispatcher: log WARN No action registered
end
Dispatcher->>Scope: exit after Uni completes
deactivate Dispatcher
par Outbound paths
    Dispatcher->>Groups: broadcastMessage groupName payload
    Groups->>Groups: EventBus consumer publishes to sockets
    Groups-->>Client: writeTextMessage payload
and
    Dispatcher->>HttpServer: broadcastMessageSync current socket
end
rect rgb(220,220,255)
    Note over Client,Dispatcher: Client disconnects or exception
    Client-->>HttpServer: close frame or error
    HttpServer->>Config: closeHandler or exceptionHandler
    Config->>Groups: remove socket and consumers
    Config->>Scope: exit
end

Note over Client,Groups: Cleanup complete and group maps pruned
```

## Lifecycle Notes

- **Scope Entry/Exit**: Every text frame re-enters `CallScoper` to refresh scoped beans; `eventually(callScoper::exit)` ensures cleanup.
- **Group Registration**: Default groups are `EveryoneGroup` and the per-connection `RequestContextId`; additional groups use SPI hook first, then fall back to `groupSockets`.
- **Failure Handling**: Exceptions during hook or handler execution wrap in `WebSocketException` or log; cleanup still runs.
- **Backpressure**: Vert.x handles TCP backpressure; handlers must avoid blocking to keep the event loop responsive.

---

**See Also**
- [sequence-message-routing.md](./sequence-message-routing.md) — dispatch detail
- [c4-component-websocket.md](./c4-component-websocket.md) — component responsibilities
- [../RULES.md](../RULES.md) — async and scope rules
