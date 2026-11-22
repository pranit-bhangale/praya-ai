# Praya AI — Personal AI Agent & Architecture

**Praya** is a personal AI assistant built for **Pranit Bhangale** utilizing **Spring Boot**, **Spring AI**, **Java 25 (Virtual Threads)**, and **Project Reactor**.

## Architecture & Technical Documentation

The complete architectural documentation is available in the [`docs/`](file:///P:/praya-ai/docs) directory:

- [**High-Level Design (HLD)**](file:///P:/praya-ai/docs/HLD.md): System vision, component architecture, threat modeling, progressive disclosure pattern, and reactive data flow.
- [**Low-Level Design (LLD)**](file:///P:/praya-ai/docs/LLD.md): Detailed package structure, class diagrams, concurrency algorithms, two-tier tool schemas, and REST/SSE contracts.

## Key Capabilities
- **Two-Tier Progressive Tooling**: Separates high-level summaries from deep architectural specifications to eliminate LLM hallucinations.
- **Enterprise Guardrails**: Multi-phase regex and keyword guardrails preventing prompt injection, jailbreaks, PII/secret leaks, and system prompt leakage.
- **Layered Rate Limiting**: Declarative IP protection (20 req/60s) and conversation session pacing (5 req/15s) using fair sliding-window locks.
- **Reactive Streaming**: Token-level Server-Sent Events (SSE) with live tool lifecycle indicators (`STARTED`, `COMPLETED`).
