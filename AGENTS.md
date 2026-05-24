# Agent Development Contract

This repository is intended to support agent-led development without repeated archaeology.

## Canonical Reading Order

Read these in order before making material changes:

1. `docs/product-contract.md`
2. `README.md`
3. `GLOSSARY.md`
4. `AGENTS.md`
5. `docs/README.md`
6. `docs/decisions/`
7. `docs/v2-verification-matrix.md`
8. `docs/v2-proof-gap.md`

## Product Direction

- The supported product contract is one staged LLM synthesis pipeline.
- Active product docs should describe only that supported pipeline.
- Legacy pipeline references belong only in evidence, architecture, or convergence docs when needed to explain current implementation drift.
- Code and docs should converge toward the single supported staged pipeline contract.

## Source Of Truth Rules

- Canonical product and support truth lives in `docs/product-contract.md`, `README.md`, `GLOSSARY.md`, `docs/README.md`, and active decision notes in `docs/decisions/`.
- `docs/archive/` is historical context only. It may contain stale claims, resolved plans, and superseded concerns.
- When archive material disagrees with code or canonical docs, prefer code plus active canonical docs.

## Evidence And Status Rules

Use these labels precisely:

- `verified working`
- `verified limited`
- `known broken`
- `untested`

Agents may upgrade a capability from `verified limited` to `verified working` only when both are true:

- the behavior is covered by meaningful deterministic automated tests for the full path being claimed
- at least one documented manual real-provider scenario exists for stages that depend on external providers or live integrations

## Knowledge Capture Rules

- Do not leave durable repository truth only in chat.
- Capture terminology and concept boundaries in `GLOSSARY.md`.
- Capture durable product and engineering decisions in `docs/decisions/`.
- Update canonical docs when repository positioning or support claims change.
- Do not promote archive documents into active contract by implication.
- Do not reintroduce legacy pipeline language into product-facing docs.

## Agent Autonomy Boundary

Agents are expected to:

- understand and change the repo without rediscovery
- make bounded product-scope decisions within the documented contract
- treat the staged LLM synthesis pipeline as the default implementation target

Agents are not expected to invent support claims beyond the current evidence base.
