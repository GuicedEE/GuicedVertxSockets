# GUIDES — GuicedEE Websockets

This document provides step-by-step how-to guides for common tasks in the GuicedEE Websockets library. All guides are aligned with RULES.md and GLOSSARY.md and reference enterprise rule indexes.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Implementing a Message Receiver](#implementing-a-message-receiver)
3. [Managing Groups (Join, Publish, Leave)](#managing-groups)
4. [Handling Lifecycle Events](#handling-lifecycle-events)
5. [Advanced Patterns (Types, Routing, Serialization)](#advanced-patterns)
6. [Error Handling & Logging](#error-handling--logging)
7. [Building with Maven](#building-with-maven)
8. [Testing WebSocket Handlers](#testing-websocket-handlers)
9. [Observability & Debugging](#observability--debugging)

---

## Getting Started

### 1.1 Add Dependency to Your Project

**Add to pom.xml**:
```xml
<dependency>
    <groupId>com.guicedee</groupId>
    <artifactId>guiced-vertx-sockets</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

This transitively includes:
- `guiced-vertx-web` (Vert.x HttpServer integration)
- `guice-inject-client` (ClientScoped DI)
- `json-representation` (JSON serialization)
- Vert.x 5.x and Guice 4.x

### 1.2 Configure Your Module

**Create a Guice module** that extends `AbstractModule`:

```java
package com.example.websocket;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.guicedee.client.services.websocket.IWebSocketMessageReceiver;

public class WebSocketConfigModule extends AbstractModule {
    @Override
    protected void configure() {
        // Bind your message receivers
        Multibinder<IWebSocketMessageReceiver> binder = 
            Multibinder.newSetBinder(binder(), IWebSocketMessageReceiver.class);
        binder.addBinding().to(ChatMessageReceiver.class);
        binder.addBinding().to(NotificationReceiver.class);
    }
}
```

### 1.3 GuicedEE Auto-Discovery

GuicedEE auto-discovers your module via the GuicedEE **SPI (Service Provider Interface)**. Ensure `module-info.java` declares:

```java
module com.example.websocket {
    requires com.guicedee.vertx.sockets;
    requires static lombok;
    
    provides com.guicedee.client.services.lifecycle.IGuiceModule 
        with com.example.websocket.WebSocketConfigModule;
}
```

Or create `META-INF/services/com.guicedee.client.services.lifecycle.IGuiceModule` with:
```
com.example.websocket.WebSocketConfigModule
```

---

## Implementing a Message Receiver

### 2.1 Basic Message Receiver (SPI)

**Create a class implementing `IWebSocketMessageReceiver`**:

```java
package com.example.websocket;

import com.guicedee.client.services.websocket.IWebSocketMessageReceiver;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NotNull;
import org.jspecify.annotations.Nullable;

@Slf4j
public class ChatMessageReceiver implements IWebSocketMessageReceiver {
    
    @Override
    public void onMessage(@NotNull ServerWebSocket webSocket, @NotNull Object message) {
        log.debug("Received message on connection {}: {}", webSocket.path(), message);
        
        // Cast to your message type
        if (message instanceof ChatMessage chatMsg) {
            processChatMessage(webSocket, chatMsg);
        } else {
            log.warn("Unknown message type: {}", message.getClass().getSimpleName());
        }
    }
    
    private void processChatMessage(@NotNull ServerWebSocket webSocket, @NotNull ChatMessage msg) {
        // Validate
        if (msg.text() == null || msg.text().isBlank()) {
            sendError(webSocket, "Message text cannot be empty");
            return;
        }
        
        // Process (e.g., save to DB, publish to group)
        // ... application logic ...
        
        // Send response
        ChatResponse response = new ChatResponse("ack", msg.id());
        webSocket.writeTextMessage(response.toJson());
    }
    
    private void sendError(@NotNull ServerWebSocket webSocket, @NotNull String errorMsg) {
        ErrorResponse err = new ErrorResponse("error", errorMsg);
        webSocket.writeTextMessage(err.toJson());
    }
}

// Example message DTO (use records for Java 25+)
record ChatMessage(String id, String text, String userId) {
    // Could add validation here
}

record ChatResponse(String type, String id) {
    String toJson() { /* ... */ }
}

record ErrorResponse(String type, String message) {
    String toJson() { /* ... */ }
}
```

### 2.2 With Dependency Injection

**Inject services into your receiver**:

```java
@Slf4j
public class ChatMessageReceiver implements IWebSocketMessageReceiver {
    
    private final ChatService chatService;      // @Inject
    private final GroupPublisher groupPublisher; // @Inject
    
    @Inject
    public ChatMessageReceiver(ChatService chatService, GroupPublisher groupPublisher) {
        this.chatService = chatService;
        this.groupPublisher = groupPublisher;
    }
    
    @Override
    public void onMessage(@NotNull ServerWebSocket webSocket, @NotNull Object message) {
        if (message instanceof ChatMessage chatMsg) {
            // Use injected services
            chatService.saveMessage(chatMsg);
            groupPublisher.publishToGroup("chat:lobby", chatMsg);
        }
    }
}
```

### 2.3 Multiple Receivers

**Multiple implementations are all invoked** (multibinding pattern):

```java
// In your module:
Multibinder<IWebSocketMessageReceiver> binder = 
    Multibinder.newSetBinder(binder(), IWebSocketMessageReceiver.class);
binder.addBinding().to(ChatMessageReceiver.class);
binder.addBinding().to(PresenceReceiver.class);  // Also called for every message
binder.addBinding().to(NotificationReceiver.class); // Also called for every message
```

Each receiver is independent; exceptions in one do not affect others.

---

## Managing Groups

### 3.1 Join a Group (Add to Group)

**Implement `GuicedWebSocketOnAddToGroup` SPI hook**:

```java
package com.example.websocket;

import com.guicedee.client.services.websocket.GuicedWebSocketOnAddToGroup;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NotNull;

@Slf4j
public class GroupJoinListener implements GuicedWebSocketOnAddToGroup {
    
    @Override
    public void onAddToGroup(@NotNull ServerWebSocket webSocket, @NotNull String groupName) {
        log.info("Connection {} joined group '{}'", webSocket.path(), groupName);
        
        // Notify other members or update presence
        // e.g., eventBus.publish(groupName, new PresenceEvent("joined", userId, timestamp))
    }
}
```

### 3.2 Leave a Group (Remove from Group)

**Implement `GuicedWebSocketOnRemoveFromGroup` SPI hook**:

```java
@Slf4j
public class GroupLeaveListener implements GuicedWebSocketOnRemoveFromGroup {
    
    @Override
    public void onRemoveFromGroup(@NotNull ServerWebSocket webSocket, @NotNull String groupName) {
        log.info("Connection {} left group '{}'", webSocket.path(), groupName);
        
        // Cleanup presence, notify others
        // e.g., update database, clear timers
    }
}
```

### 3.3 Publish to a Group (Broadcast)

**Inject and use `VertxEventBus` (or GroupPublisher wrapper)**:

```java
import io.vertx.core.eventbus.EventBus;

@Slf4j
public class ChatMessageReceiver implements IWebSocketMessageReceiver {
    
    private final EventBus eventBus;  // Injected Vert.x EventBus
    
    @Inject
    public ChatMessageReceiver(EventBus eventBus) {
        this.eventBus = eventBus;
    }
    
    @Override
    public void onMessage(@NotNull ServerWebSocket webSocket, @NotNull Object message) {
        if (message instanceof ChatMessage chatMsg) {
            // Broadcast to group
            eventBus.publish("group:chat", chatMsg);
            
            // Or send to a specific address (one recipient)
            eventBus.send("user:" + chatMsg.userId(), chatMsg);
        }
    }
}
```

### 3.4 Configure Group Handlers in Module

```java
public class WebSocketConfigModule extends AbstractModule {
    @Override
    protected void configure() {
        // Message handlers
        Multibinder<IWebSocketMessageReceiver> msgBinder = 
            Multibinder.newSetBinder(binder(), IWebSocketMessageReceiver.class);
        msgBinder.addBinding().to(ChatMessageReceiver.class);
        
        // Group lifecycle hooks
        Multibinder<GuicedWebSocketOnAddToGroup> addBinder = 
            Multibinder.newSetBinder(binder(), GuicedWebSocketOnAddToGroup.class);
        addBinder.addBinding().to(GroupJoinListener.class);
        
        Multibinder<GuicedWebSocketOnRemoveFromGroup> removeBinder = 
            Multibinder.newSetBinder(binder(), GuicedWebSocketOnRemoveFromGroup.class);
        removeBinder.addBinding().to(GroupLeaveListener.class);
    }
}
```

---

## Handling Lifecycle Events

### 4.1 Scope Enter (Connection Setup)

**Implement `IOnCallScopeEnter` to initialize per-connection state**:

```java
import com.guicedee.client.services.lifecycle.IOnCallScopeEnter;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NotNull;

@Slf4j
public class WebSocketScopeEnterHandler implements IOnCallScopeEnter {
    
    private final UserContextProvider userContextProvider; // @RequestScoped
    
    @Inject
    public WebSocketScopeEnterHandler(UserContextProvider userContextProvider) {
        this.userContextProvider = userContextProvider;
    }
    
    @Override
    public void onScopeEnter() {
        log.debug("WebSocket scope entered; initializing user context");
        
        // Initialize request-scoped beans, open DB connection, etc.
        // e.g., userContextProvider.initializeFromThread();
    }
}
```

### 4.2 Scope Exit (Cleanup)

**Implement `IOnCallScopeExit` to cleanup resources**:

```java
import com.guicedee.client.services.lifecycle.IOnCallScopeExit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketScopeExitHandler implements IOnCallScopeExit {
    
    private final DatabaseConnection dbConnection; // @RequestScoped
    
    @Inject
    public WebSocketScopeExitHandler(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }
    
    @Override
    public void onScopeExit() {
        log.debug("WebSocket scope exiting; cleaning up resources");
        
        try {
            dbConnection.close();
        } catch (Exception e) {
            log.warn("Error closing database connection", e);
        }
    }
}
```

### 4.3 Register Lifecycle Hooks

```java
public class WebSocketConfigModule extends AbstractModule {
    @Override
    protected void configure() {
        // Lifecycle hooks
        Multibinder<IOnCallScopeEnter> enterBinder = 
            Multibinder.newSetBinder(binder(), IOnCallScopeEnter.class);
        enterBinder.addBinding().to(WebSocketScopeEnterHandler.class);
        
        Multibinder<IOnCallScopeExit> exitBinder = 
            Multibinder.newSetBinder(binder(), IOnCallScopeExit.class);
        exitBinder.addBinding().to(WebSocketScopeExitHandler.class);
    }
}
```

---

## Advanced Patterns

### 5.1 Async/Mutiny Composition

### 5.1 Exception Handling in Message Receivers

**Always wrap in try-catch; log and respond gracefully**:

```java
@Slf4j
public class ChatMessageReceiver implements IWebSocketMessageReceiver {
    
    @Override
    public void onMessage(@NotNull ServerWebSocket webSocket, @NotNull Object message) {
        try {
            // ... message processing ...
        } catch (ValidationException e) {
            log.warn("Validation failed for message: {}", e.getMessage());
            sendError(webSocket, "Invalid message: " + e.getMessage());
        } catch (IOException e) {
            log.error("I/O error processing message", e);
            sendError(webSocket, "Server error processing message");
        } catch (Exception e) {
            log.error("Unexpected error in message handler", e);
            sendError(webSocket, "Internal server error");
        }
    }
    
    private void sendError(@NotNull ServerWebSocket webSocket, @NotNull String message) {
        try {
            ErrorResponse err = new ErrorResponse("error", message);
            webSocket.writeTextMessage(err.toJson());
        } catch (Exception e) {
            log.error("Failed to send error response", e);
        }
    }
}
```

### 5.2 Logging Standards

**Follow SLF4J levels** (see [RULES.md](./RULES.md) § 5.3):

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatMessageReceiver implements IWebSocketMessageReceiver {
    
    @Override
    public void onMessage(@NotNull ServerWebSocket webSocket, @NotNull Object message) {
        log.debug("Received message from {}: type={}", webSocket.remoteAddress(), 
                  message.getClass().getSimpleName());
        
        if (message instanceof ChatMessage) {
            log.info("Processing chat message: id={}", ((ChatMessage)message).id());
        } else {
            log.warn("Unknown message type: {}", message.getClass().getName());
        }
    }
}
```

**Inject SLF4J Logger** using Lombok:
```java
@Slf4j  // Generates: private static final Logger log = LoggerFactory.getLogger(...)
public class ChatMessageReceiver implements IWebSocketMessageReceiver {
    // Use: log.debug(...), log.info(...), log.warn(...), log.error(...)
}
```

---

### 5.1 Async/Mutiny Composition

**Use Smallrye Mutiny for non-blocking async flows**:

```java
import io.smallrye.mutiny.Uni;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AsyncChatHandler implements IWebSocketMessageReceiver {
    
    private final ChatService chatService;  // Returns Uni<ChatMessage>
    
    @Inject
    public AsyncChatHandler(ChatService chatService) {
        this.chatService = chatService;
    }
    
    @Override
    public void onMessage(@NotNull ServerWebSocket webSocket, @NotNull Object message) {
        if (message instanceof ChatMessage chatMsg) {
            // Chain async operations
            chatService.saveMessage(chatMsg)
                .onItem().invoke(saved -> log.info("Message saved: {}", saved.id()))
                .onItem().transformToUni(saved -> chatService.notifyGroup(saved))
                .onItem().invoke(notified -> log.debug("Group notified"))
                .onFailure().invoke(err -> log.error("Error processing message", err))
                .subscribe().with(
                    v -> { /* success */ },
                    err -> sendError(webSocket, "Processing failed")
                );
        }
    }
}
```

**Key Principles**:
- ✅ Never block the event loop: use `Uni<T>` for all async I/O
- ✅ Chain operations with `.onItem().transformToUni()` or `.flatMap()`
- ✅ Handle errors with `.onFailure().invoke()` or `.onFailure().recoverWithUni()`
- ✅ Subscribe with `.subscribe().with(onSuccess, onError)` to trigger execution

---

**Use Java records (Java 25)** for type-safe message payloads:

```java
// Base message interface
public interface WebSocketMessage {
    String action();
    long timestamp();
}

// Chat message
public record ChatMessage(
    String action,
    long timestamp,
    String userId,
    String text,
    @Nullable String groupId
) implements WebSocketMessage {
    public ChatMessage(String userId, String text) {
        this(
            "chat",
            System.currentTimeMillis(),
            userId,
            text,
            null
        );
    }
}

// Presence update
public record PresenceUpdate(
    String action,
    long timestamp,
    String userId,
    String status  // online, away, offline
) implements WebSocketMessage {
    public PresenceUpdate(String userId, String status) {
        this(
            "presence",
            System.currentTimeMillis(),
            userId,
            status
        );
    }
}
```

### 5.4 Route Messages by Type

**Dispatch to type-specific handlers based on message action**:

```java
@Slf4j
public class PolymorphicMessageRouter implements IWebSocketMessageReceiver {
    
    private final ChatMessageHandler chatHandler;
    private final PresenceHandler presenceHandler;
    
    @Inject
    public PolymorphicMessageRouter(ChatMessageHandler chatHandler, PresenceHandler presenceHandler) {
        this.chatHandler = chatHandler;
        this.presenceHandler = presenceHandler;
    }
    
    @Override
    public void onMessage(@NotNull ServerWebSocket webSocket, @NotNull Object message) {
        if (message instanceof ChatMessage chatMsg) {
            log.debug("Routing chat message: {}", chatMsg.action());
            chatHandler.handle(webSocket, chatMsg);
        } else if (message instanceof PresenceUpdate presenceMsg) {
            log.debug("Routing presence update: {}", presenceMsg.action());
            presenceHandler.handle(webSocket, presenceMsg);
        } else {
            log.warn("Unknown message type: {}", message.getClass().getSimpleName());
        }
    }
}

// Handler implementation
@Slf4j
public class ChatMessageHandler {
    private final ChatService chatService;
    private final GuicedWebSocket guicedWebSocket;
    
    @Inject
    public ChatMessageHandler(ChatService chatService, GuicedWebSocket guicedWebSocket) {
        this.chatService = chatService;
        this.guicedWebSocket = guicedWebSocket;
    }
    
    public void handle(@NotNull ServerWebSocket webSocket, @NotNull ChatMessage msg) {
        try {
            // Validate and save
            chatService.saveMessage(msg);
            
            // Broadcast to group
            guicedWebSocket.broadcastMessage(msg.groupId(), toJson(msg));
            
            // Acknowledge
            webSocket.writeTextMessage(toJson(new AckResponse("chat", msg.id())));
        } catch (ValidationException e) {
            log.warn("Invalid chat message: {}", e.getMessage());
            sendError(webSocket, "Invalid message");
        }
    }
}
```

---

## Building with Maven

### 6.1 Compile the Project

```bash
mvn clean compile
```

**Expected output**:
- Compiles src/main/java and src/test/java with Java 25
- Processes annotations (Lombok, annotation processors)
- Generates classes to target/classes/

### 6.2 Run Tests

```bash
mvn clean test
```

**Expected output**:
- Runs JUnit 5 tests in src/test/java/
- Reports coverage via Jacoco (if configured)

### 6.3 Package JAR

```bash
mvn clean package
```

**Expected output**:
- Creates target/guiced-vertx-sockets-2.0.0-SNAPSHOT.jar
- Flattens POM via flatten-maven-plugin
- Outputs dependency-reduced-pom.xml

### 6.4 Deploy to Local Repo / Sonatype

```bash
mvn clean deploy
```

**Note**: Requires GitHub secrets (USERNAME, USER_TOKEN, SONA_USERNAME, SONA_PASSWORD) configured in GitHub Actions.

---

## Testing WebSocket Handlers

### 7.1 Unit Test a Message Receiver

```java
import org.junit.jupiter.api.Test;
import io.vertx.core.http.ServerWebSocket;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ChatMessageReceiverTest {
    
    @Test
    void testOnMessageWithValidChat() {
        // Arrange
        ServerWebSocket webSocket = mock(ServerWebSocket.class);
        ChatMessageReceiver receiver = new ChatMessageReceiver(/* ... */);
        ChatMessage msg = new ChatMessage("123", "Hello", "user1");
        
        // Act
        receiver.onMessage(webSocket, msg);
        
        // Assert
        verify(webSocket).writeTextMessage(argThat(
            json -> json.contains("\"type\":\"ack\"")
        ));
    }
    
    @Test
    void testOnMessageWithEmptyText() {
        // Arrange
        ServerWebSocket webSocket = mock(ServerWebSocket.class);
        ChatMessageReceiver receiver = new ChatMessageReceiver(/* ... */);
        ChatMessage msg = new ChatMessage("123", "", "user1");
        
        // Act
        receiver.onMessage(webSocket, msg);
        
        // Assert
        verify(webSocket).writeTextMessage(argThat(
            json -> json.contains("\"type\":\"error\"") && json.contains("empty")
        ));
    }
}
```

### 7.2 Integration Test with Vert.x

```java
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;

@ExtendWith(VertxExtension.class)
class WebSocketIntegrationTest {
    
    @Test
    void testWebSocketConnection(Vertx vertx, VertxTestContext ctx) {
        // Setup Vert.x HTTP server with WebSocket handler
        vertx.createHttpServer()
            .webSocketHandler(ws -> {
                ws.handler(buffer -> {
                    // Echo message back
                    ws.write(buffer);
                });
            })
            .listen(8888, ctx.succeeding(server -> {
                // Client connects
                HttpClient client = vertx.createHttpClient();
                client.webSocket(8888, "localhost", "/ws", ctx.succeeding(ws -> {
                    ws.write("Hello WebSocket", ctx.succeeding(v -> ctx.completeNow()));
                }));
            }));
    }
}
```

---

## Observability & Debugging

### 8.1 Enable Debug Logging

**In logback.xml or logback-test.xml**:
```xml
<logger name="com.guicedee.vertx" level="DEBUG" />
<logger name="com.example.websocket" level="DEBUG" />
<root level="INFO">
    <appender-ref ref="STDOUT" />
</root>
```

### 8.2 Metrics & Tracing (Future Enhancement)

**Track**:
- Messages per second (throughput)
- Handler execution time (latency)
- Group size (scalability)
- Error rate (reliability)

See [rules/generative/platform/observability/README.md](./rules/generative/platform/observability/README.md) for best practices.

### 8.3 Common Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| **Handler not invoked** | Not registered in multibinder | Check WebSocketConfigModule; verify module SPI discovery |
| **NullPointerException on inject** | Field not @Injected | Ensure `@Inject` on constructor or field; check module binding |
| **WebSocket not accepting connections** | Port in use or handler not registered | Check server port; verify VertxSocketHttpWebSocketConfigurator runs |
| **Message lost after send** | Backpressure or buffer overflow | Check webSocket.setWriteQueueMaxSize(); handle backpressure |
| **Resource leak (open connections)** | Scope not exiting | Ensure IOnCallScopeExit is implemented and called; check logs |

---

## References

- [RULES.md](./RULES.md) — Technical constraints (async, CRTP, nullability)
- [GLOSSARY.md](./GLOSSARY.md) — Terminology (SPI, scopes, fluent API)
- [IMPLEMENTATION.md](./IMPLEMENTATION.md) — Code module map
- [rules/generative/backend/guicedee/README.md](./rules/generative/backend/guicedee/README.md) — GuicedEE DI patterns
- [rules/generative/backend/vertx/README.md](./rules/generative/backend/vertx/README.md) — Vert.x async model
- [Vert.x WebSocket Documentation](https://vertx.io/docs/vertx-web/java/#websockets)

---

**Document Status**:
- Created: 2025-11-29
- Last Updated: 2025-11-29
