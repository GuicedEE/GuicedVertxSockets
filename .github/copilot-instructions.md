## Copilot Workspace Instructions — GuicedEE Websockets

**Pinned constraints**
- Follow `RULES.md` sections 4 (Behavioral/Quality), 5 (Technical/Structure), and 6 (Forward-Only). Do not introduce backward-compat stubs.
- Respect Document Modularity Policy: keep topic-scoped docs (RULES, GUIDES, IMPLEMENTATION, GLOSSARY, PACT) linked; no monolithic docs.
- Language/tooling: Java 25 LTS, Maven, JPMS; JSpecify nullability on public APIs; Log4j2 logging; CRTP fluent APIs (no Lombok `@Builder`).
- Vert.x event loop must stay non-blocking; offload blocking calls.
- Generated artifacts are read-only; never edit compiled outputs.

**Stage gating**
- Docs-first. Complete architecture/rules docs before code changes unless user explicitly waives. Blanket approval applies for this run.
