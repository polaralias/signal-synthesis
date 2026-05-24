# Product Contract

Last reviewed: 2026-05-24

This document defines the final product-state contract for Signal Synthesis.

## Product Position

Signal Synthesis supports one analysis product:

- a staged LLM synthesis pipeline for market screening and trade setup generation

This is the only supported product pipeline in canonical product docs.

## Supported Pipeline

The supported pipeline is a staged flow that:

1. discovers candidate symbols
2. filters tradeable names
3. fetches lightweight quote context
4. performs LLM shortlist gating
5. applies targeted enrichment
6. ranks candidate setups
7. performs a final decision update
8. resolves RSS/news context when needed
9. produces fundamentals and news synthesis

## Supported Product Capabilities

The product contract includes:

- bring-your-own market-data providers
- bring-your-own LLM providers
- provider fallback with provider cooldown on access-denied and rate-limit responses
- provider-aware OpenAI, Gemini, and Anthropic model setup that discovers models from the saved key and chooses curated current defaults from the models that key can actually access
- shortlist-driven selective enrichment
- keep/drop decision revision before final output
- RSS/news-assisted synthesis
- persisted analysis history and watchlist support
- background alerting

## Product Language

Use this language in active product docs:

- `staged LLM synthesis pipeline`
- `shortlist gate`
- `decision update`
- `RSS digest`
- `fundamentals/news synthesis`

Do not describe the product as supporting multiple long-term analysis pipelines.

## Support Boundary

Canonical product docs should describe only the staged LLM synthesis product.

If the codebase still contains legacy analysis-path code, settings, or references:

- treat that as implementation drift or convergence work
- document it only in evidence, architecture, or convergence docs
- do not present it as supported product behavior

## Verification Posture

Current support status for the staged LLM synthesis product:

- `verified working`

That means:

- the staged product is implemented
- the claimed path has deterministic automated coverage
- at least one documented real-provider staged verification scenario exists

See:

- `docs/v2-verification-matrix.md`
- `docs/v2-proof-gap.md`
- `docs/v2-manual-real-provider-verification-2026-05-23.md`

## Related Canonical Docs

Read next:

1. `README.md`
2. `GLOSSARY.md`
3. `AGENTS.md`
4. `docs/README.md`
5. `docs/decisions/`
