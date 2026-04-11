# GuicedEE WebSockets

[![Build](https://github.com/GuicedEE/GuicedVertxSockets/actions/workflows/build.yml/badge.svg)](https://github.com/GuicedEE/GuicedVertxSockets/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.guicedee/websockets)](https://central.sonatype.com/artifact/com.guicedee/websockets)
[![Maven Snapshot](https://img.shields.io/nexus/s/com.guicedee/websockets?server=https%3A%2F%2Foss.sonatype.org&label=Maven%20Snapshot)](https://oss.sonatype.org/content/repositories/snapshots/com/guicedee/websockets/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](https://www.apache.org/licenses/LICENSE-2.0)

![Java 25+](https://img.shields.io/badge/Java-25%2B-green)
![Guice 7](https://img.shields.io/badge/Guice-7%2B-green)
![Vert.X 5](https://img.shields.io/badge/Vert.x-5%2B-green)
![Maven 4](https://img.shields.io/badge/Maven-4%2B-green)

Lightweight **RFC 6455 WebSocket support** for [GuicedEE](https://github.com/GuicedEE) applications using **Vert.x 5**.
Connections are call-scoped, messages are dispatched through an action-based receiver SPI, and group membership is managed via the Vert.x EventBus. Builds on top of [web](../web) for HTTP server plumbing.

Built on [Vert.x](https://vertx.io/) · [Google Guice](https://github.com/google/guice) · [Mutiny](https://smallrye.io/smallrye-mutiny/) · JPMS module `com.guicedee.vertx.sockets` · Java 25+

## 📦 Installation

```xml
<dependency>
  <groupId>com.guicedee</groupId>
  <artifactId>websockets</artifactId>
</dependency>
```

<details>
<summary>Gradle (Kotlin DSL)</summary>

```kotlin
implementation("com.guicedee:websockets:2.0.0-RC3")
```
</details>

## ✨ Features

- **Auto-configured WebSocket server** — `VertxSocketHttpWebSocketConfigurator` registers the WebSocket handler on the Vert.x HTTP server automatically via SPI
- **Call-scoped connections** — each WebSocket connection gets its own Guice `@CallScope` with access to `CallScopeProperties`, `ServerWebSocket`, and `IGuicedWebSocket`
- **Action-based message routing** — inbound JSON messages are deserialized to `WebSocketMessageReceiver` and dispatched to `IWebSocketMessageReceiver` handlers by action name
- **Group management** — connections join/leave named groups; broadcast messages are delivered to all group members via the Vert.x EventBus
- **SPI-driven lifecycle hooks** — `GuicedWebSocketOnAddToGroup`, `GuicedWebSocketOnRemoveFromGroup`, and `GuicedWebSocketOnPublish` let you intercept group operations
- **Reactive message processing** — `receiveMessage()` returns `Uni<Void>` for non-blocking composition
- **Per-message compression** — RFC 7692 WebSocket compression enabled by default
- **JSpecify nullability annotations** — `@NonNull` / `@Nullable` on public API for clarity and safety
- **Configurable server options** — injectable `WebSocketServerOptions` singleton for compression, frame sizes, and connection limits

## 🚀 Quick Start

**Step 1** — Implement a message receiver:

```java
public class ChatReceiver implements IWebSocketMessageReceiver<Void, ChatReceiver> {

    @Override
    public Set<String> messageNames() {
        return Set.of("chat");
    }

    @Override
    public Uni<Void> receiveMessage(WebSocketMessageReceiver<?> message) {
        String text = (String) message.getData().get("text");
        IGuicedWebSocket ws = IGuiceContext.get(IGuicedWebSocket.class);
        ws.broadcastMessage("chat:lobby", text);
        return Uni.createFrom().voidItem();
    }
}
```

**Step 2** — Register via JPMS:

```java
module my.app {
    requires com.guicedee.vertx.sockets;

    provides com.guicedee.client.services.websocket.IWebSocketMessageReceiver
        with my.app.ChatReceiver;
}
```

**Step 3** — Bootstrap GuicedEE (WebSocket server starts automatically):

```java
IGuiceContext.registerModuleForScanning.add("my.app");
IGuiceContext.instance();
// WebSocket server is now accepting connections on the HTTP port
```

Clients connect to `ws://localhost:8080` and send JSON:

```json
{ "action": "chat", "data": { "text": "Hello, world!" } }
```

## 📐 Architecture

```
Startup
  IGuiceContext.instance()
   └─ VertxWebServerPostStartup              (from web module — creates HTTP server)
       └─ VertxSocketHttpWebSocketConfigurator
           ├─ HttpServerOptions builder       (compression, frame sizes)
           ├─ HttpServer builder              (registers webSocketHandler)
           └─ Router builder                  (no-op — WebSockets bypass the router)
```

### Connection lifecycle

```
Client connects (ws://...)
 → Vert.x HttpServer.webSocketHandler()
   → CallScoper enters @CallScope
   → CallScopeProperties initialized (RequestContextId = textHandlerID)
   → Connection added to "Everyone" group
   → Per-connection EventBus consumer registered (address = textHandlerID)
   → textMessageHandler installed
     → JSON → WebSocketMessageReceiver deserialization
     → IGuicedWebSocket.getMessagesListeners() lookup by action
     → IWebSocketMessageReceiver.receiveMessage() → Uni<Void>
   → closeHandler / exceptionHandler
     → Connection removed from all groups
     → CallScopeProperties cleaned up
```

### Message flow

```
Client sends JSON:  { "action": "chat", "data": { "text": "hi" } }
 → textMessageHandler
   → processMessageInContext()          ← re-enters CallScope for this connection
     → GuicedWebSocket.receiveMessage()
       → ObjectMapper.readValue() → WebSocketMessageReceiver
       → Lookup action "chat" in messageListeners
       → ChatReceiver.receiveMessage()  ← your handler
       → Uni subscribed, errors logged
```

### Group broadcast flow

```
GuicedWebSocket.broadcastMessage("chat:lobby", "Hello")
 → GuicedWebSocketOnPublish SPI check  ← custom publish hook (if registered)
 → Fallback: iterate groupSockets["chat:lobby"]
   → writeTextMessage() to each ServerWebSocket
```

## 💬 Message Protocol

Inbound messages are JSON-deserialized into `WebSocketMessageReceiver`:

| Field | Type | Required | Purpose |
|---|---|---|---|
| `action` | `String` | ✅ | Routes to the matching `IWebSocketMessageReceiver` |
| `data` | `Map<String, Object>` | ❌ | Arbitrary key/value payload |
| `broadcastGroup` | `String` | ❌ | Set automatically to the connection's `RequestContextId` |
| `dataService` | `String` | ❌ | Optional service discriminator |
| `webSocketSessionId` | `String` | ❌ | Optional client-set session identifier |

Unknown JSON fields are captured via `@JsonAnySetter` into the `data` map.

### Example messages

```json
{ "action": "join", "data": { "room": "lobby" } }
```

```json
{ "action": "send", "data": { "text": "Hello!", "to": "user-42" } }
```

## 👥 Group Management

Every connection is automatically added to the **`Everyone`** group and a **per-connection** group (keyed by `textHandlerID`).

### Join / leave groups

```java
IGuicedWebSocket ws = IGuiceContext.get(IGuicedWebSocket.class);

// Join a named group
ws.addToGroup("chat:lobby");

// Leave a named group
ws.removeFromGroup("chat:lobby");
```

### Broadcast to a group

```java
// Async — broadcasts via iterating group sockets
ws.broadcastMessage("chat:lobby", "Hello everyone!");

// Send to this connection only (via EventBus)
ws.broadcastMessage("Private message for you");

// Sync — writes directly to the current ServerWebSocket
ws.broadcastMessageSync("chat:lobby", "Immediate message");
```

### How groups work

Each group has:
1. An **EventBus consumer** that forwards messages to all group members
2. A **socket list** (`CopyOnWriteArrayList<ServerWebSocket>`) tracking connected clients

When a connection closes or errors, it is automatically removed from all groups.

## 🔌 SPI Extension Points

All SPIs are discovered via `ServiceLoader`. Register implementations with JPMS `provides...with` or `META-INF/services`.

### `IWebSocketMessageReceiver`

The primary extension point — handles inbound messages routed by action name:

```java
public class EchoReceiver implements IWebSocketMessageReceiver<Void, EchoReceiver> {

    @Override
    public Set<String> messageNames() {
        return Set.of("echo");
    }

    @Override
    public Uni<Void> receiveMessage(WebSocketMessageReceiver<?> message) {
        String text = (String) message.getData().get("text");
        IGuicedWebSocket ws = IGuiceContext.get(IGuicedWebSocket.class);
        ws.broadcastMessage(message.getBroadcastGroup(), text);
        return Uni.createFrom().voidItem();
    }
}
```

### `GuicedWebSocketOnAddToGroup`

Intercepts group join operations. Return `true` from the `CompletableFuture` to indicate the join was handled (skips the default group logic):

```java
public class AuditGroupJoin implements GuicedWebSocketOnAddToGroup<AuditGroupJoin> {

    @Override
    public CompletableFuture<Boolean> onAddToGroup(String groupName) {
        auditService.logGroupJoin(groupName);
        return CompletableFuture.completedFuture(false); // proceed with default
    }
}
```

### `GuicedWebSocketOnRemoveFromGroup`

Intercepts group leave operations. Same `CompletableFuture<Boolean>` contract as add:

```java
public class AuditGroupLeave implements GuicedWebSocketOnRemoveFromGroup<AuditGroupLeave> {

    @Override
    public CompletableFuture<Boolean> onRemoveFromGroup(String groupName) {
        auditService.logGroupLeave(groupName);
        return CompletableFuture.completedFuture(false);
    }
}
```

### `GuicedWebSocketOnPublish`

Intercepts broadcast operations. Return `true` to indicate the publish was handled (skips the default broadcast logic):

```java
public class FilteredPublish implements GuicedWebSocketOnPublish<FilteredPublish> {

    @Override
    public boolean publish(String groupName, String message) throws Exception {
        if (containsProfanity(message)) {
            return true; // swallow the message
        }
        return false; // proceed with default broadcast
    }
}
```

### SPI summary

| SPI | Purpose | Return |
|---|---|---|
| `IWebSocketMessageReceiver` | Handle inbound messages by action name | `Uni<R>` |
| `GuicedWebSocketOnAddToGroup` | Intercept group join operations | `CompletableFuture<Boolean>` |
| `GuicedWebSocketOnRemoveFromGroup` | Intercept group leave operations | `CompletableFuture<Boolean>` |
| `GuicedWebSocketOnPublish` | Intercept broadcast operations | `boolean` |
| `IOnCallScopeEnter` | Hook into call scope entry | — |
| `IOnCallScopeExit` | Hook into call scope exit | — |

## ⚙️ Configuration

### `WebSocketServerOptions`

An injectable `@Singleton` that controls WebSocket server behavior. Override defaults by injecting and configuring before startup, or by providing a custom Guice binding:

| Property | Default | Purpose |
|---|---|---|
| `perMessageCompressionSupported` | `true` | Enable RFC 7692 per-message compression |
| `compressionLevel` | `9` | Compression level (0–9) |
| `maxFrameSize` | `65536` | Max WebSocket frame size in bytes |
| `maxChunkSize` | `65536` | Max HTTP chunk size in bytes |
| `maxFormAttributeSize` | `65536` | Max form attribute size in bytes |
| `registerWebSocketWriteHandlers` | `true` | Register write handlers for backpressure |
| `idleTimeoutSeconds` | `300` | Connection idle timeout in seconds |
| `maxGroupSize` | `10000` | Max WebSocket connections per group |

Options are validated at startup — invalid values throw `IllegalArgumentException`.

### Customizing options

```java
public class MyWebSocketConfig extends AbstractModule implements IGuiceModule<MyWebSocketConfig> {

    @Override
    protected void configure() {
        bind(WebSocketServerOptions.class).toInstance(new WebSocketServerOptions() {{
            setCompressionLevel(6);
            setMaxFrameSize(131072);
            setIdleTimeoutSeconds(600);
        }});
    }
}
```

## 💉 Dependency Injection

WebSocket connections run inside Guice's `@CallScope`. The following are available for injection within a WebSocket context:

| Type | Scope | Purpose |
|---|---|---|
| `IGuicedWebSocket` | `@CallScope` | Group management and broadcasting |
| `ServerWebSocket` | `@CallScope` | The raw Vert.x WebSocket connection |
| `CallScopeProperties` | `@CallScope` | Per-connection properties (`RequestContextId`, `ServerWebSocket`, etc.) |
| `Vertx` | `@Singleton` | The shared Vert.x instance |
| `WebSocketServerOptions` | `@Singleton` | Server configuration |

### Guice module bindings

`VertxWebSocketsModule` automatically configures:

- `ServerWebSocket` → provided from `CallScopeProperties` in `@CallScope`
- `IGuicedWebSocket` → bound to `GuicedWebSocket`
- `Multibinder` extension points for `GuicedWebSocketOnAddToGroup`, `GuicedWebSocketOnRemoveFromGroup`, and `GuicedWebSocketOnPublish`

## 🔄 Startup Flow

```
IGuiceContext.instance()
 └─ IGuiceModule hooks
     └─ VertxWebSocketsModule           (binds ServerWebSocket, IGuicedWebSocket, SPI multibinders)
 └─ IGuicePostStartup hooks
     └─ VertxSocketHttpWebSocketConfigurator (sortOrder = 55)
         ├─ HttpServerOptions builder    (applies WebSocketServerOptions)
         ├─ HttpServer builder           (registers webSocketHandler with group/scope setup)
         └─ Router builder               (no-op pass-through)
```

## 🗺️ Module Graph

```
com.guicedee.vertx.sockets
 ├── com.guicedee.vertx.web          (HTTP server, Router, BodyHandler)
 ├── com.guicedee.client             (CallScope, SPI contracts, IGuicedWebSocket)
 ├── io.vertx.core                   (Vertx, ServerWebSocket, EventBus)
 └── org.jspecify                    (nullability annotations)
```

## 🧩 JPMS

Module name: **`com.guicedee.vertx.sockets`**

The module:
- **exports** `com.guicedee.vertx.websockets`
- **provides** `IGuicePostStartup`, `VertxHttpServerConfigurator`, `VertxHttpServerOptionsConfigurator` with `VertxSocketHttpWebSocketConfigurator`
- **provides** `IGuiceModule` with `VertxWebSocketsModule`
- **uses** `IWebSocketMessageReceiver`, `IOnCallScopeEnter`, `IOnCallScopeExit`, `GuicedWebSocketOnAddToGroup`, `GuicedWebSocketOnRemoveFromGroup`, `GuicedWebSocketOnPublish`

In non-JPMS environments, `META-INF/services` discovery still works.

## 🏗️ Key Classes

| Class | Package | Role |
|---|---|---|
| `VertxSocketHttpWebSocketConfigurator` | `websockets` | Registers the WebSocket handler, configures server options, manages group EventBus consumers |
| `GuicedWebSocket` | `websockets` | `@CallScope` facade — group management, message broadcast, inbound message dispatch |
| `WebSocketServerOptions` | `websockets` | `@Singleton` configurable options (compression, frame sizes, timeouts) |
| `WebSocketException` | `websockets` | Unchecked exception for WebSocket operation failures |
| `VertxWebSocketsModule` | `implementations` | Guice module — binds `ServerWebSocket`, `IGuicedWebSocket`, SPI multibinders |
| `IGuicedWebSocket` | `client` (SPI) | Contract for group management and message broadcasting |
| `IWebSocketMessageReceiver` | `client` (SPI) | Contract for action-based inbound message handling |
| `WebSocketMessageReceiver` | `client` (SPI) | DTO for deserialized inbound WebSocket messages |
| `GuicedWebSocketOnAddToGroup` | `client` (SPI) | Hook for intercepting group join operations |
| `GuicedWebSocketOnRemoveFromGroup` | `client` (SPI) | Hook for intercepting group leave operations |
| `GuicedWebSocketOnPublish` | `client` (SPI) | Hook for intercepting broadcast operations |

## 🤝 Contributing

Issues and pull requests are welcome — please add tests for new message receivers, group behaviors, or server configurations.

## 📄 License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)
