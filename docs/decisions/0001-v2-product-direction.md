# Decision 0001: Single Staged Pipeline Product Contract

Date: 2026-05-23

## Status

Accepted

## Decision

The repository's product contract supports one staged LLM synthesis pipeline.

Canonical product docs should describe only that supported staged pipeline.

If the codebase still contains legacy alternative analysis-path code or settings, treat them as implementation drift or convergence work, not as supported product behavior.

## Meaning

- Agents should treat the staged LLM synthesis pipeline as the default implementation target.
- README and active product docs should lead with the single staged-pipeline contract.
- Work that removes product/doc drift toward that single-pipeline contract is aligned with repository direction.
- Claims that the product supports multiple long-term analysis pipelines are incorrect unless this decision is replaced.

## Why

The repository needs a documentation harness that reflects the final product state rather than preserving legacy product framing.

Without an explicit decision, agents would continue to rediscover or debate:

- whether product docs should describe more than one supported pipeline
- whether legacy analysis-path references belong in the active product contract
- whether current implementation drift should leak back into product-facing documentation

This decision resolves that ambiguity.
