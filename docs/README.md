---
type: "Navigation Guide"
title: "Repository Knowledge Base"
description: "Documents Repository Knowledge Base for the signal-synthesis repository."
timestamp: 2026-07-28T21:55:36Z
authority: canonical
verification: untested
owner: polaralias
tags:
  - signal-synthesis
  - navigation-guide
navigation:
  role: supporting
  order: 100
---
# Repository Knowledge Base

This directory is the canonical repository knowledge surface.

Use this directory to understand:

- the supported product contract
- the current verified state
- implementation drift and convergence work
- what still needs proof

## Reading Order

Start here, then read in this order:

1. [`product-contract.md`](product-contract.md)
2. [`../README.md`](../README.md)
3. [`../GLOSSARY.md`](../GLOSSARY.md)
4. [`../AGENTS.md`](../AGENTS.md)
5. [`decisions/`](decisions/)
6. [`v2-verification-matrix.md`](v2-verification-matrix.md)
7. [`v2-proof-gap.md`](v2-proof-gap.md)
8. [`codebase-map.md`](codebase-map.md)
9. [`analysis-viewmodel-pipeline-map.md`](analysis-viewmodel-pipeline-map.md)
10. [`v2-convergence-plan.md`](v2-convergence-plan.md)
11. [`v2-manual-real-provider-verification-2026-05-23.md`](v2-manual-real-provider-verification-2026-05-23.md)
12. [`public-readiness-verification-2026-05-24.md`](public-readiness-verification-2026-05-24.md)
13. [`anthropic-stage-route-verification-2026-05-24.md`](anthropic-stage-route-verification-2026-05-24.md)

## Document Classes

Canonical contract docs:

- [`product-contract.md`](product-contract.md)
- [`../README.md`](../README.md)
- [`../GLOSSARY.md`](../GLOSSARY.md)
- [`../AGENTS.md`](../AGENTS.md)
- [`decisions/`](decisions/)

Evidence and implementation-state docs:

- [`codebase-map.md`](codebase-map.md)
- [`analysis-viewmodel-pipeline-map.md`](analysis-viewmodel-pipeline-map.md)
- [`v2-verification-matrix.md`](v2-verification-matrix.md)
- [`v2-proof-gap.md`](v2-proof-gap.md)
- [`v2-convergence-plan.md`](v2-convergence-plan.md)
- [`v2-manual-real-provider-verification-2026-05-23.md`](v2-manual-real-provider-verification-2026-05-23.md)
- [`public-readiness-verification-2026-05-24.md`](public-readiness-verification-2026-05-24.md)
- [`anthropic-stage-route-verification-2026-05-24.md`](anthropic-stage-route-verification-2026-05-24.md)

Derived or research docs:

- [`LLMProviders.md`](LLMProviders.md)
  - Treat this as provider research and implementation context.
  - Do not treat it as a verified support contract.

Archived planning and historical notes:

- [`archive/`](archive/)
  - Treat archive documents as historical context, not current contract.
  - Archive material may contain stale claims, resolved concerns, or superseded plans.

## Status Language

Use these labels precisely:

- `verified working`
- `verified limited`
- `known broken`
- `untested`

Distinguish clearly between:

- current observed state
- current verified state
- desired end state
- remaining gap

## Current Truth

At the time of this knowledge-base setup:

- the supported product contract is one staged LLM synthesis pipeline
- canonical product docs should not present legacy alternative pipelines as supported product behaviour
- the repository has a coherent Android app architecture
- the staged LLM synthesis path is now `verified working` at product-path level
- the repository now also has a verified local publish baseline covering build, unit tests, connected Android tests, and a bounded publish-safety/doc-alignment pass
- API key storage is implemented with `EncryptedSharedPreferences`
- OpenAI, Gemini, and Anthropic provider setup now discovers available models per saved key and constrains curated defaults to models that key can actually reach
- archive material remains historical context only, and active docs now align with the staged-pipeline contract

## Where To Continue Next

If the next task is code understanding, start from:

- [`codebase-map.md`](codebase-map.md)

If the next task is architecture or refactor planning, start from:

- [`analysis-viewmodel-pipeline-map.md`](analysis-viewmodel-pipeline-map.md)
- [`v2-convergence-plan.md`](v2-convergence-plan.md)

If the next task is proving the staged path, start from:

- [`v2-verification-matrix.md`](v2-verification-matrix.md)
- [`v2-proof-gap.md`](v2-proof-gap.md)

If the next task is beginning TDD against the staged path, start with:

- [`decisions/0005-first-tdd-proof-slice.md`](decisions/0005-first-tdd-proof-slice.md)
- [`v2-proof-gap.md`](v2-proof-gap.md)
- [`v2-verification-matrix.md`](v2-verification-matrix.md)

## Repository knowledge

- [Documentation map](knowledge/documentation-map.md) — RKE-managed reading order and relationship hub.
