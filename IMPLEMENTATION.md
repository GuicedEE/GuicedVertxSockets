# IMPLEMENTATION — GuicedEE Websockets

This document describes the current code layout, module structure, and key classes. It serves as a map for developers and links back to RULES/GUIDES.

## Project Structure

```
websockets/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── module-info.java               # JPMS module declaration
│   │   │   └── com/guicedee/vertx/
│   │   │       └── websockets/
│   │   │           ├── GuicedWebSocket.java   # Public API / facade
│   │   │           ├── VertxSocketHttpWebSocketConfigurator.java  # Initializer (IGuicePostStartup)
│   │   │           └── implementations/
│   │   │               └── VertxWebSocketsModule.java  # Guice module (DI config)
│   │   └── resources/
│   │       └── META-INF/services/              # SPI service loader (alternative to JPMS)
│   │
│   └── test/
│       ├── java/
│       │   ├── module-info.java               # Test JPMS module
│       │   └── com/guicedee/vertx/
│       │       └── websockets/
│       │           ├── *Test.java             # Unit tests (mirror main structure)
│       │           └── implementations/
│       │               └── *Test.java
│       └── resources/
│           └── logback-test.xml               # Test logging config
│
├── docs/
│   ├── architecture/                          # C4 diagrams, sequences, ERDs
│   │   ├── README.md
│   │   ├── c4-context.md
│   │   ├── c4-container.md
│   │   ├── c4-component-websocket.md
│   │   ├── sequence-websocket-lifecycle.md
│   │   ├── sequence-message-routing.md
│   │   ├── erd-websocket-model.md
│   │   └── img/                               # (generated; optional)
│   └── PROMPT_REFERENCE.md                    # Stack selections & links
│
├── .github/
│   └── workflows/
│       └── maven-package.yml                  # GitHub Actions CI
│
├── rules/                                     # Git submodule (ai-rules.git)
│   └── generative/                            # Topic-scoped rules
│       ├── backend/
│       │   ├── vertx/
│       │   ├── guicedee/
│       │   ├── fluent-api/
│       │   ├── jspecify/
│       │   └── ...
│       ├── language/
│       │   └── java/
│       ├── platform/
│       │   ├── ci-cd/
│       │   └── observability/
│       └── ...
│
├── pom.xml                                    # Maven build file
├── README.md                                  # Quick start
├── PACT.md                                    # Vision & agreement
├── RULES.md                                   # Technical constraints
├── GLOSSARY.md                                # Topic-first terminology
├── GUIDES.md                                  # How-to guides
├── IMPLEMENTATION.md                          # This file
├── LICENSE                                    # Apache 2.0
└── .gitmodules                                # Submodule declaration
```

---

## Module Declarations

### Main Module (JPMS)

**File**: `src/main/java/module-info.java`

```java
module com.guicedee.vertx.sockets {
    // Exports
    exports com.guicedee.vertx.websockets;

    // Dependencies (transitive)
    requires transitive com.guicedee.vertx.web;

    // SPI services used
    uses com.guicedee.client.services.websocket.IWebSocketMessageReceiver;
    uses com.guicedee.client.services.websocket.GuicedWebSocketOnAddToGroup;
    uses com.guicedee.client.services.websocket.GuicedWebSocketOnRemoveFromGroup;
    uses com.guicedee.client.services.websocket.GuicedWebSocketOnPublish;
    uses com.guicedee.client.services.lifecycle.IOnCallScopeEnter;
    uses com.guicedee.client.services.lifecycle.IOnCallScopeExit;

    // SPI services provided
    provides com.guicedee.client.services.lifecycle.IGuiceModule 
        with com.guicedee.vertx.websockets.implementations.VertxWebSocketsModule;
    provides com.guicedee.client.services.lifecycle.IGuicePostStartup 
        with com.guicedee.vertx.websockets.VertxSocketHttpWebSocketConfigurator;

    // Opens for reflection-based DI (Guice)
    opens com.guicedee.vertx.websockets;
    opens com.guicedee.vertx.websockets.implementations to com.google.guice;

    // Optional compile-time dependencies
    requires static lombok;
}
```

**Key Points**:
- Exports only `com.guicedee.vertx.websockets` (public API)
- Declares SPI services and uses
- Opens packages to Guice for reflection
- `requires transitive` ensures dependency chains are correct

### Test Module (JPMS)

**File**: `src/test/java/module-info.java`

```java
module com.guicedee.vertx.sockets.tests {
    requires com.guicedee.vertx.sockets;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.engine;
    // ... other test dependencies ...
}
```

---

## Core Classes

### 1. GuicedWebSocket (Public API)

**File**: `src/main/java/com/guicedee/vertx/websockets/GuicedWebSocket.java`

**Purpose**: Public facade or utility class for application-level WebSocket operations.

**Responsibilities**:
- Provide static/instance methods for common WebSocket tasks (e.g., broadcast to group)
- Hide internal Vert.x/GuicedEE complexity from application code
- Fluent API (CRTP) for configuration (if applicable)

**Key Methods** (example):
```java
public class GuicedWebSocket {
    /**
     * Broadcasts a message to a named group.
     * @param groupName the group identifier
     * @param message the payload to broadcast
     */
    public static void broadcastToGroup(String groupName, Object message) { /* ... */ }
    
    /**
     * Adds a WebSocket connection to a group.
     * @param webSocket the connection to add
     * @param groupName the group identifier
     */
    public static void addToGroup(ServerWebSocket webSocket, String groupName) { /* ... */ }
}
```

**Usage**:
```java
// In a message handler:
GuicedWebSocket.broadcastToGroup("chat:lobby", new ChatMessage("Hello", userId));
GuicedWebSocket.addToGroup(webSocket, "chat:lobby");
```

---

### 2. VertxSocketHttpWebSocketConfigurator (Initializer)

**File**: `src/main/java/com/guicedee/vertx/websockets/VertxSocketHttpWebSocketConfigurator.java`

**Purpose**: Initializes WebSocket integration at application startup.

**Implements**: 
- `IGuicePostStartup` — Called during GuicedEE DI initialization
- `VertxHttpServerConfigurator` — Vert.x integration hook
- `VertxHttpServerOptionsConfigurator` — Vert.x server options (if needed)

**Responsibilities**:
1. Retrieve Vert.x `HttpServer` and `Router` from DI
2. Register WebSocket handler callback with Vert.x routing
3. Inject `VertxWebSocketsModule` bindings (SPI handlers, lifecycle hooks)
4. Configure server options (max frame size, idle timeout, etc.)

**Key Methods** (pseudo-code):
```java
@Slf4j
public class VertxSocketHttpWebSocketConfigurator implements IGuicePostStartup {
    
    private final Injector injector;
    private final HttpServer server;
    private final Router router;
    
    @Inject
    public VertxSocketHttpWebSocketConfigurator(Injector injector, HttpServer server, Router router) {
        this.injector = injector;
        this.server = server;
        this.router = router;
    }
    
    @Override
    public void onStartup() {
        log.info("Configuring WebSocket handler");
        
        // Register WebSocket handler
        router.route("/ws/*").handler(this::handleWebSocket);
    }
    
    private void handleWebSocket(RoutingContext ctx) {
        if (ctx.request().headers().contains("upgrade", "websocket")) {
            ctx.request().toWebSocket()
                .onSuccess(ws -> handleNewWebSocket(ws, ctx))
                .onFailure(throwable -> ctx.fail(400));
        }
    }
    
    private void handleNewWebSocket(ServerWebSocket ws, RoutingContext ctx) {
        // Attach message handler, scope lifecycle hooks, etc.
        ws.handler(buffer -> {
            // Decode and dispatch to SPI handlers
            // ... via guice injector ...
        });
        
        ws.closeHandler(v -> {
            // Fire cleanup hooks
            // ... via guice injector ...
        });
    }
}
```

---

### 3. VertxWebSocketsModule (DI Configuration)

**File**: `src/main/java/com/guicedee/vertx/websockets/implementations/VertxWebSocketsModule.java`

**Purpose**: Configures all Guice bindings for WebSocket functionality.

**Implements**: `IGuiceModule` (SPI for GuicedEE module discovery)

**Responsibilities**:
1. Define core bindings (e.g., WebSocket event bus, handlers registry)
2. Create multibinders for SPI extension points
3. Configure scopes (request scope, client scope)
4. Register default implementations or placeholders

**Key Method** (pseudo-code):
```java
public class VertxWebSocketsModule extends AbstractModule {
    
    @Override
    protected void configure() {
        // Bind core WebSocket services
        bind(WebSocketRouter.class).in(Singleton.class);
        bind(WebSocketMessageDecoder.class).in(Singleton.class);
        
        // Create multibinders for SPI extension points
        Multibinder<IWebSocketMessageReceiver> msgBinder = 
            Multibinder.newSetBinder(binder(), IWebSocketMessageReceiver.class);
        // Applications bind their own handlers
        
        Multibinder<GuicedWebSocketOnAddToGroup> addBinder = 
            Multibinder.newSetBinder(binder(), GuicedWebSocketOnAddToGroup.class);
        
        Multibinder<GuicedWebSocketOnRemoveFromGroup> removeBinder = 
            Multibinder.newSetBinder(binder(), GuicedWebSocketOnRemoveFromGroup.class);
        
        Multibinder<GuicedWebSocketOnPublish> pubBinder = 
            Multibinder.newSetBinder(binder(), GuicedWebSocketOnPublish.class);
    }
    
    @Provides
    @Singleton
    EventBus provideEventBus(Vertx vertx) {
        return vertx.eventBus();
    }
}
```

---

## SPI (Service Provider Interface)

### IWebSocketMessageReceiver

**Package**: `com.guicedee.client.services.websocket` (GuicedEE client SPI)

**Method**:
```java
public interface IWebSocketMessageReceiver {
    /**
     * Called when a WebSocket message is received.
     * 
     * @param webSocket the connection
     * @param message the decoded message object
     */
    void onMessage(ServerWebSocket webSocket, Object message);
}
```

**Implementation Contract**:
- Implementations are discovered via SPI (multibinder)
- Each implementation is called independently
- Exceptions are caught and logged; other handlers continue
- No guaranteed order (unless explicitly ordered via binding)

### GuicedWebSocketOnAddToGroup

**Method**:
```java
public interface GuicedWebSocketOnAddToGroup {
    void onAddToGroup(ServerWebSocket webSocket, String groupName);
}
```

**Similar to**: `GuicedWebSocketOnRemoveFromGroup`, `GuicedWebSocketOnPublish`

---

## Dependency Injection Scopes

### Singleton Scope
- **Created**: Once per application lifetime
- **Thread-Safety**: Must be thread-safe (event loop accesses from multiple message handlers)
- **Example**: WebSocketRouter, EventBus, configuration beans

### Request Scope (Per-Message)
- **Created**: Once per message processing
- **Lifetime**: From scope enter (IOnCallScopeEnter) to scope exit (IOnCallScopeExit)
- **Thread-Safety**: Single-threaded per scope (event loop thread)
- **Example**: UserContext, DatabaseConnection, request-specific state

### Client Scope (Per-Connection)
- **Created**: Once per WebSocket connection
- **Lifetime**: From connection open to close
- **Thread-Safety**: Single-threaded per scope (event loop thread)
- **Example**: ConnectionState, user session (if preferred over request scope)

---

## Build Artifacts

### Maven Coordinates

**Group**: `com.guicedee`  
**Artifact**: `guiced-vertx-sockets`  
**Version**: `2.0.0-SNAPSHOT` (inherited from parent POM)  
**Packaging**: `jar`

### Output Files

- `target/guiced-vertx-sockets-2.0.0-SNAPSHOT.jar` — Main JAR
- `dependency-reduced-pom.xml` — Flattened POM (via flatten-maven-plugin)
- `target/classes/` — Compiled classes
- `target/test-classes/` — Compiled tests
- `target/maven-archiver/pom.properties` — Version metadata

### Javadoc

```bash
mvn javadoc:javadoc
```

Output: `target/site/apidocs/`

---

## Integration Points

### With GuicedEE Core
- **Module Discovery**: Via `IGuiceModule` SPI in `module-info.java`
- **DI Injection**: Constructor-based @Inject (preferred)
- **Scopes**: Request/Client scopes via `@RequestScoped` / `@ClientScoped`

### With GuicedEE Web (guiced-vertx-web)
- **Vert.x Router**: Injected HttpServer and Router
- **Request Lifecycle**: Leverages existing Vert.x routing and scope lifecycle
- **Transitive Dependency**: This module depends on guiced-vertx-web

### With Vert.x
- **ServerWebSocket**: Direct API for frame send/receive
- **EventBus**: For group broadcasting and inter-instance messaging
- **Vertx**: Async utilities (executeBlocking, etc.)

### With Application Code
- **SPI Implementation**: Applications implement IWebSocketMessageReceiver
- **Module Binding**: Applications extend AbstractModule and bind handlers
- **SPI Discovery**: GuicedEE auto-discovers via JPMS or META-INF/services

---

## Testing Strategy

### Unit Tests
- Mock ServerWebSocket and Vert.x components
- Test handler logic in isolation
- Verify exception handling and logging

### Integration Tests
- Use Vert.x JUnit5 extension (VertxExtension)
- Spin up real Vert.x HttpServer
- Connect via WebSocket client library
- Verify end-to-end message flow

### Coverage
- Target: ≥70% code coverage (Jacoco)
- Public API: 100% coverage
- Internal implementations: 60%+ coverage

---

## References

### Key Files
- [RULES.md](./RULES.md) — Design constraints (async, CRTP, nullability)
- [GUIDES.md](./GUIDES.md) — How-to guides (create handler, manage groups, etc.)
- [GLOSSARY.md](./GLOSSARY.md) — Terminology (SPI, scopes, fluent API)
- [docs/architecture/README.md](./docs/architecture/README.md) — C4 diagrams

### Enterprise Rules
- [rules/generative/backend/guicedee/README.md](./rules/generative/backend/guicedee/README.md) — GuicedEE DI patterns
- [rules/generative/backend/guicedee/web/README.md](./rules/generative/backend/guicedee/web/README.md) — GuicedEE Web patterns
- [rules/generative/backend/vertx/README.md](./rules/generative/backend/vertx/README.md) — Vert.x async model
- [rules/generative/backend/fluent-api/crtp.rules.md](./rules/generative/backend/fluent-api/crtp.rules.md) — CRTP fluent API
- [rules/generative/backend/jspecify/README.md](./rules/generative/backend/jspecify/README.md) — Nullability annotations

### External Documentation
- [Vert.x WebSocket Docs](https://vertx.io/docs/vertx-web/java/#websockets)
- [Google Guice User Guide](https://github.com/google/guice/wiki)
- [JPMS (Java Module System) Spec](https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html)

---

**Document Status**:
- Created: 2025-11-29
- Last Updated: 2025-11-29
- Maintainer: GuicedEE Project Team
