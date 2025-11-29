# PROMPT_REFERENCE — GuicedEE Websockets

This document records the selected technology stacks, diagram links, and glossary composition for the GuicedEE Websockets project. **Future prompts must load and honor this file.**

## Project Identity

- **Repository**: https://github.com/GuicedEE/GuicedVertxSockets
- **Organization**: GuicedEE
- **Project Name**: Websockets (GuicedVertxSockets)
- **Description**: Provides reactive Websocket functionality for GuicedEE applications via Vert.x integration
- **License**: Apache 2.0

---

## Selected Technology Stacks

### Language & Runtime
- ✅ **Java 25 LTS** (mandatory)
  - See: [rules/generative/language/java/java-25.rules.md](./rules/generative/language/java/java-25.rules.md)
  - Features: records, sealed classes, pattern matching, var inference
- ✅ **Java Platform Module System (JPMS)** enabled
  - module-info.java in src/main/java and src/test/java

### Build Tool
- ✅ **Maven**
  - POM inherits from `com.guicedee:parent:2.0.0-SNAPSHOT`
  - See: [rules/generative/language/java/build-tooling.md](./rules/generative/language/java/build-tooling.md)

### Core Frameworks

| Framework | Version | Rules | Status |
|-----------|---------|-------|--------|
| **Vert.x** | 5.x | [rules/generative/backend/vertx/README.md](./rules/generative/backend/vertx/README.md) | ✅ Selected |
| **GuicedEE (Core)** | 2.0.0 | [rules/generative/backend/guicedee/README.md](./rules/generative/backend/guicedee/README.md) | ✅ Selected |
| **GuicedEE (Web)** | 2.0.0 | [rules/generative/backend/guicedee/web/README.md](./rules/generative/backend/guicedee/web/README.md) | ✅ Selected |
| **GuicedEE (Client)** | 2.0.0 | [rules/generative/backend/guicedee/client/README.md](./rules/generative/backend/guicedee/client/README.md) | ✅ Selected |
| **Lombok** | 1.18.x | [rules/generative/structural/lombok/README.md](./rules/generative/structural/lombok/README.md) | ✅ Selected (no @Builder) |
| **JSpecify** | 1.x | [rules/generative/backend/jspecify/README.md](./rules/generative/backend/jspecify/README.md) | ✅ Selected |
| **SLF4J** | 2.x | [rules/generative/structural/logging/README.md](./rules/generative/structural/logging/README.md) | ✅ Selected |

### API Design Pattern
- ✅ **Fluent API Strategy**: CRTP (Curiously Recurring Template Pattern)
  - See: [rules/generative/backend/fluent-api/crtp.rules.md](./rules/generative/backend/fluent-api/crtp.rules.md)
  - **Constraint**: Do NOT use Lombok @Builder

### Testing
- ✅ **JUnit 5 (Jupiter)**
- ✅ **Jacoco** (code coverage target: ≥70%)
- ⚠️ **BDD/TDD**: Test-driven development; RULES/GUIDES precede code

### CI/CD
- ✅ **GitHub Actions**
  - Workflow: [.github/workflows/maven-package.yml](./.github/workflows/maven-package.yml)
  - See: [rules/generative/platform/ci-cd/providers/github-actions.md](./rules/generative/platform/ci-cd/providers/github-actions.md)

---

## Glossary Composition (Topic-First)

### Glossary Precedence Policy
**Topic glossaries override root**: For each selected topic below, the topic's GLOSSARY.md defines canonical terms for that scope. The host GLOSSARY.md ([GLOSSARY.md](./GLOSSARY.md)) links to all topic glossaries and provides minimal Prompt Language Alignment mappings.

### Selected Topic Glossaries

| Topic | Glossary Link | Primary Terms | Precedence |
|-------|---------------|----|-----------|
| **Vert.x 5** | [rules/generative/backend/vertx/README.md](./rules/generative/backend/vertx/README.md) | Event loop, Handler, Router, ServerWebSocket, Future | **HIGH** |
| **GuicedEE Core** | [rules/generative/backend/guicedee/README.md](./rules/generative/backend/guicedee/README.md) | Injector, Module, Binding, Multibinder, SPI | **HIGH** |
| **GuicedEE Web** | [rules/generative/backend/guicedee/web/README.md](./rules/generative/backend/guicedee/web/README.md) | Router, RoutingContext, WebSocketHandler, Scope | **HIGH** |
| **GuicedEE Client** | [rules/generative/backend/guicedee/client/README.md](./rules/generative/backend/guicedee/client/README.md) | ClientScoped, RequestScoped, Activation | **HIGH** |
| **Fluent API (CRTP)** | [rules/generative/backend/fluent-api/crtp.rules.md](./rules/generative/backend/fluent-api/crtp.rules.md) | CRTP, Self-Type, Fluent Builder, Method Chaining | **MEDIUM** |
| **JSpecify** | [rules/generative/backend/jspecify/README.md](./rules/generative/backend/jspecify/README.md) | @Nullable, @NotNull, Nullability | **MEDIUM** |
| **Lombok** | [rules/generative/structural/lombok/README.md](./rules/generative/structural/lombok/README.md) | @Data, @Getter, @Setter, @Slf4j | **MEDIUM** |
| **SLF4J** | [rules/generative/structural/logging/README.md](./rules/generative/structural/logging/README.md) | Logger, log level (DEBUG, INFO, WARN, ERROR) | **MEDIUM** |
| **Java 25** | [rules/generative/language/java/java-25.rules.md](./rules/generative/language/java/java-25.rules.md) | Record, Sealed class, Pattern matching, Var | **MEDIUM** |
| **Maven** | [rules/generative/language/java/build-tooling.md](./rules/generative/language/java/build-tooling.md) | POM, Artifact, Coordinate, Plugin | **MEDIUM** |
| **GitHub Actions** | [rules/generative/platform/ci-cd/providers/github-actions.md](./rules/generative/platform/ci-cd/providers/github-actions.md) | Workflow, Job, Step, Secret | **LOW** |

### Host Glossary
- **Location**: [GLOSSARY.md](./GLOSSARY.md)
- **Purpose**: Acts as index to all topic glossaries; minimal duplication; links to topic files for all definitions
- **Prompt Language Alignment**: None enforced at root level currently; all terms link to topic scope

---

## Architecture Diagrams (C4 Model & Flows)

### Index
- **Location**: [docs/architecture/README.md](./docs/architecture/README.md)

### C4 Diagrams (Context, Container, Component)

| Level | File | Source | Status |
|-------|------|--------|--------|
| **Level 1 (Context)** | [docs/architecture/c4-context.md](./docs/architecture/c4-context.md) | Mermaid | ✅ Complete |
| **Level 2 (Container)** | [docs/architecture/c4-container.md](./docs/architecture/c4-container.md) | Mermaid | ✅ Complete |
| **Level 3 (Component — WebSocket)** | [docs/architecture/c4-component-websocket.md](./docs/architecture/c4-component-websocket.md) | Mermaid | ✅ Complete |

### Supplementary Diagrams (Sequences, ERD)

| Diagram | File | Source | Purpose |
|---------|------|--------|---------|
| **WebSocket Connection Lifecycle** | [docs/architecture/sequence-websocket-lifecycle.md](./docs/architecture/sequence-websocket-lifecycle.md) | Mermaid | Connection setup, scope lifecycle, cleanup |
| **Message Routing & Dispatch** | [docs/architecture/sequence-message-routing.md](./docs/architecture/sequence-message-routing.md) | Mermaid | Message decode, handler dispatch, responses |
| **Domain Model (ERD)** | [docs/architecture/erd-websocket-model.md](./docs/architecture/erd-websocket-model.md) | Mermaid | WebSocket, Message, Group, Handler entities |

### Rendering
- **Format**: Mermaid in Markdown (rendered natively by GitHub)
- **Images**: Optional PNG/SVG in `docs/architecture/img/` (not authoritative)
- **Source of Truth**: Mermaid/PlantUML code in `.md` files (version-controlled)

---

## Documentation Artifacts

### Project-Specific Docs (Host Repository)

| Document | Location | Purpose |
|----------|----------|---------|
| **Vision & Scope** | [PACT.md](./PACT.md) | Stakeholder agreement, success criteria, roadmap |
| **Technical Rules** | [RULES.md](./RULES.md) | Constraints, API design, code quality, forward-only policy |
| **Terminology** | [GLOSSARY.md](./GLOSSARY.md) | Topic-first index, links to topic glossaries, prompt alignment |
| **How-To Guides** | [GUIDES.md](./GUIDES.md) | Implementing handlers, managing groups, testing, troubleshooting |
| **Code Layout** | [IMPLEMENTATION.md](./IMPLEMENTATION.md) | Module structure, core classes, SPI integration, scopes |
| **Architecture** | [docs/architecture/](./docs/architecture/) | C4 diagrams, sequences, ERD, domain model |
| **Quick Start** | [README.md](./README.md) | Dependency, links to PACT/RULES/GUIDES, build instructions |

### Enterprise Rules (Submodule)
- **Location**: [rules/](./rules/) (Git submodule: ai-rules.git)
- **Scope**: Topic-scoped RULES and GUIDES for backend, language, platform

---

## Version History & Approvals

### Current State
- **Created**: 2025-11-29
- **Stage**: Stage 1 (Architecture & Foundations) Complete; Stage 2+ Pending
- **Approval Status**:
  - ✅ Blanket approval granted (per PROMPT_ADOPT_EXISTING_PROJECT.md § 0)
  - No stage gates required; proceed with Stage 2+

### Milestones
| Stage | Name | Status | Date |
|-------|------|--------|------|
| **1** | Architecture & Foundations | ✅ Complete | 2025-11-29 |
| **2** | Guides & Design Validation | ⏳ In Progress | — |
| **3** | Implementation Plan | ⏳ Pending | — |
| **4** | Code & Scaffolding | ⏳ Pending | — |

---

## Forward-Only Change Policy

**Constraint**: All changes to this project follow the forward-only model (RULES.md § 6).

**Recording Removals**: Any deprecated or removed code is documented in [MIGRATION.md](./MIGRATION.md) (if created).

**Version Tracking**: Follows SemVer 2.0; breaking changes → major version bump.

---

## AI Workspace Configuration

### GitHub Copilot
- **Instructions File**: [.github/copilot-instructions.md](./.github/copilot-instructions.md) (to be created)
- **Constraints**: RULES.md sections 4, 5, 6; Document Modularity; forward-only; stage gates

### AI Assistant (if used)
- **Rules Directory**: `.aiassistant/rules/` (to be created if applicable)
- **Content**: Summary of RULES.md sections 4, 5, 6; Document Modularity; forward-only

### Roo (if used)
- **Policy File**: [ROO_WORKSPACE_POLICY.md](./ROO_WORKSPACE_POLICY.md) (to be created if applicable)
- **Pinning**: RULES.md § 4/5/6 and forward-only constraints

---

## Future Prompts: Loading Instructions

**For any future AI prompt on this repository**:

1. **Load this file first** (`docs/PROMPT_REFERENCE.md`)
2. **Load [RULES.md](./RULES.md)** — architectural constraints and code quality rules
3. **Load [GLOSSARY.md](./GLOSSARY.md)** — terminology and topic-first links
4. **Load [PACT.md](./PACT.md)** — vision and scope (optional, for context)
5. **Reference [docs/architecture/](./docs/architecture/)** — C4 diagrams as needed

**When responding**:
- Use terms from GLOSSARY.md or the topic glossaries it references
- Link to RULES.md for design decisions
- Link to IMPLEMENTATION.md for code layout
- Reference enterprise rules via the Rules Repository submodule (e.g., `rules/generative/backend/guicedee/README.md`)

---

## Checklist: Stage 1 Completion

- [x] PACT.md created (vision, scope, success criteria)
- [x] RULES.md created (technical & behavioral constraints)
- [x] GLOSSARY.md created (topic-first terminology index)
- [x] GUIDES.md created (how-to guides and design patterns)
- [x] IMPLEMENTATION.md created (code layout and module structure)
- [x] C4 Context diagram created (c4-context.md)
- [x] C4 Container diagram created (c4-container.md)
- [x] C4 Component diagram created (c4-component-websocket.md)
- [x] Sequence diagram: Connection lifecycle (sequence-websocket-lifecycle.md)
- [x] Sequence diagram: Message routing (sequence-message-routing.md)
- [x] ERD: Domain model (erd-websocket-model.md)
- [x] docs/architecture/README.md created (diagram index)
- [x] **PROMPT_REFERENCE.md created (this file)**
- [x] Submodule added (rules/) and .gitmodules updated
- [x] Glossary composition plan documented (topic-first precedence)
- [ ] README.md updated (links to PACT/RULES/GUIDES/IMPLEMENTATION)
- [ ] .github/copilot-instructions.md created (Copilot workspace rules)
- [ ] GitHub Actions workflow configured ([.github/workflows/maven-package.yml](./.github/workflows/maven-package.yml))

---

## References

### Core Project Docs
- [PACT.md](./PACT.md)
- [RULES.md](./RULES.md)
- [GLOSSARY.md](./GLOSSARY.md)
- [GUIDES.md](./GUIDES.md)
- [IMPLEMENTATION.md](./IMPLEMENTATION.md)
- [README.md](./README.md)

### Architecture
- [docs/architecture/README.md](./docs/architecture/README.md) — Diagram index
- [docs/architecture/c4-context.md](./docs/architecture/c4-context.md)
- [docs/architecture/c4-container.md](./docs/architecture/c4-container.md)
- [docs/architecture/c4-component-websocket.md](./docs/architecture/c4-component-websocket.md)
- [docs/architecture/sequence-websocket-lifecycle.md](./docs/architecture/sequence-websocket-lifecycle.md)
- [docs/architecture/sequence-message-routing.md](./docs/architecture/sequence-message-routing.md)
- [docs/architecture/erd-websocket-model.md](./docs/architecture/erd-websocket-model.md)

### Enterprise Rules Repository
- [rules/generative/backend/vertx/README.md](./rules/generative/backend/vertx/README.md)
- [rules/generative/backend/guicedee/README.md](./rules/generative/backend/guicedee/README.md)
- [rules/generative/backend/guicedee/web/README.md](./rules/generative/backend/guicedee/web/README.md)
- [rules/generative/backend/guicedee/client/README.md](./rules/generative/backend/guicedee/client/README.md)
- [rules/generative/backend/fluent-api/crtp.rules.md](./rules/generative/backend/fluent-api/crtp.rules.md)
- [rules/generative/backend/jspecify/README.md](./rules/generative/backend/jspecify/README.md)
- [rules/generative/language/java/java-25.rules.md](./rules/generative/language/java/java-25.rules.md)
- [rules/generative/platform/ci-cd/providers/github-actions.md](./rules/generative/platform/ci-cd/providers/github-actions.md)

---

**Document Status**:
- Created: 2025-11-29
- Version: 1.0
- Maintainer: GuicedEE Project Team
