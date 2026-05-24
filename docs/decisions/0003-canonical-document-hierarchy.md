# Decision 0003: Canonical Documentation Hierarchy

Date: 2026-05-23

## Status

Accepted

## Decision

The active source-of-truth hierarchy for repository understanding is:

1. `docs/product-contract.md`
2. `README.md`
3. `GLOSSARY.md`
4. `AGENTS.md`
5. `docs/README.md`
6. active decision notes under `docs/decisions/`
7. evidence and implementation-state docs under `docs/`
8. code and tests as implementation evidence

`AGENTS.md` defines the operating contract for agent-led development and should be read before major changes.

Repository entrypoint documents may omit themselves from their own local "read next" lists, but they should not contradict this hierarchy.

## Meaning

- Agents should not rely on archive material as active contract.
- Durable repository truth should be captured in these active surfaces, not left in chat.
- When code and docs disagree, treat the disagreement as something to resolve, not something to smooth over silently.

## Why

The repository now separates:

- product contract
- operating contract
- evidence and implementation state

That sharper hierarchy reduces rediscovery and prevents implementation drift from leaking back into product-facing docs.
