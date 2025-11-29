# AI Assistant Workspace Rules

Pin these constraints before generating code or docs:

- Follow `RULES.md` sections 4 (Code Quality & Conventions), 5 (Code Structure & Module Layout), and 6 (Forward-Only Change Policy).
- Honor the Document Modularity Policy: keep topic-scoped files, avoid monoliths, and link RULES ↔ GUIDES ↔ IMPLEMENTATION ↔ GLOSSARY ↔ PACT.
- Enforce Forward-Only: remove legacy APIs instead of stubbing; document removals in migration notes when applicable.
- Language/tooling: Java 25 LTS, Maven, JPMS; use JSpecify for nullability, SLF4J for logging, CRTP (no Lombok `@Builder`).
- Scope discipline: non-blocking Vert.x event loop; use DI scopes via GuicedEE.

All AI outputs must comply with these rules and avoid modifying generated artifacts or compiled outputs.***
