# RULES — GuicedEE Websockets

This document declares the technical and behavioral constraints for the GuicedEE Websockets library. All code, docs, and CI/CD align to these rules.

## 1. Scope & Identity

**Project**: GuicedEE Websockets (GuicedVertxSockets)  
**Repository**: https://github.com/GuicedEE/GuicedVertxSockets  
**Module**: `com.guicedee.vertx.sockets`  
**License**: Apache 2.0  

**Public API Export**:
```java
module com.guicedee.vertx.sockets {
    exports com.guicedee.vertx.websockets;
    // ... SPI services
}
```

**Artifact Coordinates**: `com.guicedee:guiced-vertx-sockets:2.0.0-SNAPSHOT` (Maven)

---

## 2. Architecture & Tech Stack

### Language & Runtime
- **Java Version**: Java 25 LTS (mandatory)
  - Use records, sealed classes, pattern matching, var inference
  - See [rules/generative/language/java/java-25.rules.md](./rules/generative/backend/guicedee/../../language/java/java-25.rules.md)
- **Build Tool**: Maven (required)
  - pom.xml inherits from `com.guicedee:parent` (version 2.0.0-SNAPSHOT)
  - See [rules/generative/language/java/build-tooling.md](./rules/generative/backend/guicedee/../../language/java/build-tooling.md)
- **Module System**: Java Platform Module System (JPMS) enabled
  - module-info.java required for src/main/java and src/test/java

### Core Frameworks

| Framework | Version | Rules Link | Usage |
|-----------|---------|-----------|-------|
| **Vert.x** | 5.x | [rules/generative/backend/vertx/README.md](./rules/generative/backend/vertx/README.md) | Reactive event-driven HTTP/WebSocket server |
| **GuicedEE (Core)** | 2.0.0 | [rules/generative/backend/guicedee/README.md](./rules/generative/backend/guicedee/README.md) | Dependency injection via Google Guice |
| **GuicedEE (Web)** | 2.0.0 | [rules/generative/backend/guicedee/web/README.md](./rules/generative/backend/guicedee/web/README.md) | Vert.x routing and request lifecycle |
| **GuicedEE (Client)** | 2.0.0 | [rules/generative/backend/guicedee/client/README.md](./rules/generative/backend/guicedee/client/README.md) | ClientScoped DI for request activation |
| **Lombok** | 1.18.x | [rules/generative/structural/lombok/README.md](./rules/generative/structural/lombok/README.md) | Boilerplate reduction only; no @Builder |
| **JSpecify** | 1.x | [rules/generative/backend/jspecify/README.md](./rules/generative/backend/jspecify/README.md) | Nullability annotations (@Nullable/@NotNull) |
| **SLF4J** | 2.x | [rules/generative/structural/logging/README.md](./rules/generative/structural/logging/README.md) | Structured logging facade |

### Testing
- **Framework**: JUnit 5 (Jupiter)
- **Coverage Target**: ≥70% code coverage (measured by Jacoco)
- **Strategy**: Test-driven development (TDD); RULES/GUIDES precede code

### CI/CD
- **Provider**: GitHub Actions
- **Workflow**: [.github/workflows/maven-package.yml](./.github/workflows/maven-package.yml)
- **Trigger**: Push to master, workflow_dispatch
- **Steps**: Compile (Java 25), Test, Package, Deploy to Sonatype Nexus
- **See**: [rules/generative/platform/ci-cd/providers/github-actions.md](./rules/generative/platform/ci-cd/providers/github-actions.md)

---

## 3. API Design & Patterns

### 3.1 Fluent API Strategy: CRTP (Mandatory)

**Constraint**: Use Curiously Recurring Template Pattern (CRTP) for all builder-like APIs. Do NOT use Lombok @Builder.

**Pattern**:
```java
public abstract class SocketConfig<S extends SocketConfig<S>> {
    private int maxFrameSize;

    public S withMaxFrameSize(int size) {
        this.maxFrameSize = size;
        return (S) this;  // CRTP self-type
    }

    @SuppressWarnings("unchecked")
    protected S self() {
        return (S) this;
    }
}

public class ServerSocketConfig extends SocketConfig<ServerSocketConfig> {
    // inherits fluent methods
}
```

**Rationale**: Enables method-chaining for subclasses while maintaining type safety.  
**Reference**: [rules/generative/backend/fluent-api/crtp.rules.md](./rules/generative/backend/fluent-api/crtp.rules.md)

### 3.2 Nullability: JSpecify @Nullable / @NotNull

**Constraint**: All public method parameters and return types must be marked with JSpecify nullability annotations.

```java
// Correct
@Nullable String getMessage() { /* ... */ }
void processMessage(@NotNull String msg) { /* ... */ }

// Incorrect
@Nonnull String getMessage() { /* ... */ }  // Wrong annotation
String getMessage() { /* ... */ }            // Missing annotation on public API
```

**Reference**: [rules/generative/backend/jspecify/README.md](./rules/generative/backend/jspecify/README.md)

### 3.3 SPI (Service Provider Interface)

All extensibility points are declared as interfaces in `module-info.java` and in submodule `com.guicedee.client.services.websocket` (client DI scope).

**SPI Contracts**:
- `IWebSocketMessageReceiver` — Routes decoded messages to handlers
- `GuicedWebSocketOnAddToGroup` — Lifecycle hook when connection joins a group
- `GuicedWebSocketOnRemoveFromGroup` — Lifecycle hook when connection leaves a group
- `GuicedWebSocketOnPublish` — Lifecycle hook when group publishes a message

**Implementation Pattern**:
```java
public class MyMessageReceiver implements IWebSocketMessageReceiver {
    @Override
    public void onMessage(@NotNull ServerWebSocket webSocket, @NotNull Object message) {
        // application logic
    }
}
```

Discover and inject via GuicedEE SPI: `Multibinder.newSetBinder(binder, IWebSocketMessageReceiver.class)`

### 3.4 Async / Reactive Composition

**Constraint**: All blocking operations must be offloaded from the Vert.x event loop.

- Use `vertx.executeBlocking()` or dedicated worker pools for I/O
- Leverage `Future<T>` and composition combinators (`compose()`, `map()`, `flatMap()`)
- Never block on event-loop threads

**Reference**: [rules/generative/backend/vertx/README.md](./rules/generative/backend/vertx/README.md)

---

## 4. Code Structure & Module Layout

### Directory Tree
```
websockets/
  docs/
    architecture/              # C4, sequences, ERDs (Mermaid/PlantUML sources)
      README.md
      c4-context.md
      c4-container.md
      c4-component-websocket.md
      sequence-websocket-lifecycle.md
      erd-websocket-model.md
      img/                      # (generated; do not commit)
    PROMPT_REFERENCE.md         # Stack selections & diagram links
  src/
    main/
      java/
        module-info.java        # JPMS module declaration
        com/guicedee/vertx/
          websockets/           # Public API
            GuicedWebSocket.java
            VertxSocketHttpWebSocketConfigurator.java
          websockets/implementations/  # Internal SPI providers
            VertxWebSocketsModule.java
      resources/
        META-INF/
          services/             # SPI service loader metadata
    test/
      java/
        module-info.java
        com/guicedee/vertx/
          websockets/           # Tests mirror main structure
          websockets/implementations/
      resources/
        logback-test.xml        # Test logging config
  .github/
    workflows/
      maven-package.yml         # GitHub Actions shared workflow
  rules/                        # Git submodule (rules/generative/...)
  .gitmodules                   # Submodule declaration
  pom.xml                       # Maven build file
  README.md                     # Quick start + links to PACT/RULES/GUIDES
  PACT.md                       # Vision & stakeholder agreement
  GLOSSARY.md                   # Topic-first terminology index
  RULES.md                      # This file
  GUIDES.md                     # How-to guides & design patterns
  IMPLEMENTATION.md             # Module map & back-links
  LICENSE                       # Apache 2.0
```

### Exports & Visibility

**Public (Exported in module-info.java)**:
- `com.guicedee.vertx.websockets.*` — Application-facing APIs

**Internal (Not exported)**:
- `com.guicedee.vertx.websockets.implementations` — SPI providers; DI configuration
- Opened to `com.google.guice` for reflection-based injection

---

## 5. Code Quality & Conventions

### 5.1 Naming & Casing

| Element | Convention | Example |
|---------|-----------|---------|
| Classes | PascalCase | `VertxSocketHttpWebSocketConfigurator` |
| Methods | camelCase | `onMessage()`, `withMaxFrameSize()` |
| Constants | UPPER_SNAKE_CASE | `MAX_FRAME_SIZE`, `DEFAULT_TIMEOUT_MS` |
| Interfaces | PascalCase, no "I" prefix (per Java conventions; GuicedEE uses "I" for SPI) | `IWebSocketMessageReceiver` (SPI) |
| Packages | lowercase.dot.separated | `com.guicedee.vertx.websockets` |
| Variables | camelCase | `webSocket`, `messageHandler` |

### 5.2 Javadoc & Comments

**Mandatory**:
- All public classes, methods, and fields must have Javadoc
- Describe intent, parameters (@param), return value (@return), exceptions (@throws)
- Link to RULES/GUIDES via markdown links in Javadoc (`{@link}` for cross-references)

**Example**:
```java
/**
 * Configures WebSocket options for the Vert.x HTTP server.
 * 
 * <p>Must be bound in the GuicedEE injector module and will be invoked
 * during {@link IGuicePostStartup} to apply settings before the server starts.
 *
 * @see GuicedWebSocketOnAddToGroup
 * @see VertxWebSocketsModule
 */
public class VertxSocketHttpWebSocketConfigurator implements IGuicePostStartup, ... {
    // ...
}
```

### 5.3 Logging

**Constraint**: Use SLF4J (not System.out or Vert.x Logger directly).

```java
private static final Logger log = LoggerFactory.getLogger(MyClass.class);

log.debug("WebSocket connected: {}", webSocket.uri());
log.warn("Message dropped (queue full): {}", messageId);
log.error("Unexpected error in message handler", exception);
```

**Log Levels**:
- `ERROR`: Unrecoverable failures affecting application correctness
- `WARN`: Degraded conditions; recovered or retried
- `INFO`: Milestone events (startup, shutdown, major state changes)
- `DEBUG`: Detailed flow tracing; message send/receive
- `TRACE`: Very detailed diagnostics; loop-internal logic (rarely used)

**Reference**: [rules/generative/structural/logging/README.md](./rules/generative/structural/logging/README.md)

### 5.4 Exception Handling

**Constraint**: Declare checked exceptions in method signatures; use unchecked exceptions for programming errors.

```java
// Correct: checked exception declared
@NotNull ServerWebSocket setupWebSocket() throws IOException {
    // ...
}

// Unchecked for programming bugs
if (message == null) {
    throw new IllegalArgumentException("message must not be null");
}
```

### 5.5 Lombok Usage

**Allowed**:
- `@Data` — Generates getters, setters, equals, hashCode, toString
- `@Getter`, `@Setter` — Selective generation
- `@RequiredArgsConstructor` — Constructor for final fields
- `@AllArgsConstructor` — Full constructor
- `@Slf4j` — Injects SLF4J logger (recommended for logging)

**Forbidden**:
- `@Builder` — Use CRTP instead (see 3.1)
- `@Singular` — Incompatible with CRTP

**Configuration** (lombok.config in project root):
```properties
lombok.addLombokGeneratedAnnotation = true
```

---

## 6. Forward-Only Change Policy

**Constraint**: All changes are forward-only. Deprecated code is removed, not stubbed.

### Removal Process
1. **Identify** legacy code/API not aligned with RULES
2. **Document** removal in MIGRATION.md (create if needed) with:
   - Old name/class/method
   - Reason for removal
   - Replacement (if applicable)
   - Deprecation timeline (if phased)
3. **Refactor** all internal and test usages
4. **Remove** the code in a single commit
5. **Update** GUIDES.md/IMPLEMENTATION.md to link to new API

### Breaking Change Policy
- Follows SemVer 2.0: breaking changes → major version bump
- Never introduce new unstable APIs under `@Deprecated` for long periods
- PACT.md tracks version milestones and deprecation policy per major release

**Example Removal**:
```
Old: com.guicedee.vertx.websockets.LegacySocketConfig
Reason: Replaced by VertxSocketHttpWebSocketConfigurator (3.1 CRTP fluent API)
Replacement: VertxSocketHttpWebSocketConfigurator withMaxFrameSize(int)
Timeline: Removed in 2.0.0 (breaking change)
```

---

## 7. Documentation Standards

### Document Modularity Policy

**Constraint**: Documentation is modular. No monolithic docs; instead:
- Topic-scoped GLOSSARY files in the Rules Repository take precedence
- RULES, GUIDES, IMPLEMENTATION cross-link to topic files
- Project-specific docs live outside rules/ submodule

### Docs-as-Code

All architectural artifacts are version-controlled, human-readable sources:
- **C4 Diagrams**: Mermaid/PlantUML in `docs/architecture/*.md`
- **Sequences**: Mermaid/PlantUML in `docs/architecture/sequence-*.md`
- **ERDs**: Mermaid/PlantUML in `docs/architecture/erd-*.md`

No hand-drawn images or unsourced PDFs.

### Linking Convention

All links are relative from the document's perspective:
```markdown
[RULES.md](./RULES.md)
[Vert.x rules](./rules/generative/backend/vertx/README.md)
[Java 25](./rules/generative/language/java/java-25.rules.md)
```

---

## 8. Testing Strategy

### Test Organization
- Tests mirror source structure: `src/test/java/com/guicedee/vertx/websockets/`
- Test class naming: `{ClassName}Test` or `{ClassName}Spec` (prefer Test)
- One test class per source class; one test method per behavior

### Test Annotations & Lifecycle
- Use JUnit 5: `@Test`, `@BeforeEach`, `@AfterEach`, `@ParameterizedTest`
- Avoid JUnit 4 `@Before`, `@After`

### Assertion Style
```java
// Preferred: AssertJ (fluent assertions)
import static org.assertj.core.api.Assertions.*;

@Test
void shouldHandleWebSocketMessage() {
    assertThat(result)
        .isNotNull()
        .hasFieldOrPropertyWithValue("status", "connected");
}

// Acceptable: JUnit assertions
assertEquals("connected", result.status);
assertNotNull(result);
```

### Coverage
- Minimum 70% line coverage (Jacoco)
- 100% coverage for public API surface (methods, classes)
- Internal implementations need 60%+ coverage

---

## 9. Release & Versioning

### Semantic Versioning (SemVer 2.0)
Format: `MAJOR.MINOR.PATCH-PRERELEASE+BUILD`

- **MAJOR**: Breaking API changes
- **MINOR**: New functionality, backward-compatible
- **PATCH**: Bug fixes, no API changes
- **PRERELEASE**: `-SNAPSHOT` during development; `-RC1`, `-alpha` for release candidates
- **BUILD**: Metadata only (Git SHA, timestamp); not used for version precedence

### Current Version
`2.0.0-SNAPSHOT` (inherited from parent pom; Java 25 LTS baseline)

### Release Checklist
- [ ] All tests passing; coverage ≥70%
- [ ] RULES, GUIDES, IMPLEMENTATION updated and linked
- [ ] PACT.md and GLOSSARY.md reviewed and current
- [ ] CHANGELOG.md entry added (if tracked)
- [ ] GitHub release created with notes
- [ ] Maven Central (Sonatype Nexus) deployment successful

---

## 10. References & Links

### Core Rules
- [PACT.md](./PACT.md) — Vision & agreement
- [GLOSSARY.md](./GLOSSARY.md) — Topic-first terminology (this file)
- [GUIDES.md](./GUIDES.md) — How-to guides & design patterns
- [IMPLEMENTATION.md](./IMPLEMENTATION.md) — Code layout & module structure

### Enterprise Rules (Submodule)
- [rules/generative/backend/vertx/README.md](./rules/generative/backend/vertx/README.md)
- [rules/generative/backend/guicedee/README.md](./rules/generative/backend/guicedee/README.md)
- [rules/generative/backend/guicedee/web/README.md](./rules/generative/backend/guicedee/web/README.md)
- [rules/generative/backend/guicedee/client/README.md](./rules/generative/backend/guicedee/client/README.md)
- [rules/generative/backend/fluent-api/crtp.rules.md](./rules/generative/backend/fluent-api/crtp.rules.md)
- [rules/generative/backend/jspecify/README.md](./rules/generative/backend/jspecify/README.md)
- [rules/generative/language/java/java-25.rules.md](./rules/generative/language/java/java-25.rules.md)
- [rules/generative/language/java/build-tooling.md](./rules/generative/language/java/build-tooling.md)
- [rules/generative/structural/logging/README.md](./rules/generative/structural/logging/README.md)
- [rules/generative/platform/ci-cd/providers/github-actions.md](./rules/generative/platform/ci-cd/providers/github-actions.md)
- [rules/RULES.md](./rules/RULES.md) — Enterprise RULES (sections 4, 5, 6)

### Architecture & Diagrams
- [docs/architecture/README.md](./docs/architecture/README.md)
- [docs/PROMPT_REFERENCE.md](./docs/PROMPT_REFERENCE.md)

### Related Projects
- [GuicedEE/GuicedInjection](https://github.com/GuicedEE/GuicedInjection)
- [GuicedEE/GuicedVertxWeb](https://github.com/GuicedEE/GuicedVertxWeb)
- [Vert.x Documentation](https://vertx.io/docs)

---

**Document Status**:
- Created: 2025-11-29
- Last Updated: 2025-11-29
- Maintainer: GuicedEE Project Team
