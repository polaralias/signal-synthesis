# Decision 0005: First TDD Proof Slice

Date: 2026-05-23

## Status

Accepted

## Decision

The first TDD tranche for staged-pipeline development should start with a deterministic `RunAnalysisV2UseCase` contract suite.

This suite should use fixed repository data, fake stage-router outputs, and deterministic RSS inputs to prove the V2 orchestration contract end to end before broader convergence work begins.

The next tranche should not start by removing the V1/V2 branch in `AnalysisViewModel`.

## Meaning

- The first public interface under TDD is `RunAnalysisV2UseCase`.
- The first behaviors to lock down are shortlist normalization, targeted enrichment selection, decision-update filtering, RSS expansion handling, and final `AnalysisResult` artifact publication.
- Direct tests for `ShortlistCandidatesUseCase`, `UpdateDecisionsUseCase`, `SynthesizeFundamentalsAndNewsUseCase`, and `EnrichContextUseCase` should follow the orchestrator suite, not replace it.
- V1→V2 convergence remains important, but it should be sequenced after proof-building establishes a stronger V2 baseline.

## Why

The repository knowledge base already establishes that:

- V2 is the supported product path
- at the start of this tranche, V2 remained `verified limited`
- the largest proof gap is missing deterministic end-to-end coverage for `RunAnalysisV2UseCase`
- V1 is still the better-evidenced baseline in some areas

Starting with the V2 orchestrator contract suite reduces ambiguity about the first test surface, strengthens the current product path before structural refactors, and lowers the risk of removing the better-evidenced fallback too early.
