---
type: "Delivery Plan"
title: "V2 Convergence Plan"
description: "Documents V2 Convergence Plan for the signal-synthesis repository."
timestamp: 2026-07-28T21:55:36Z
authority: canonical
verification: untested
owner: polaralias
tags:
  - signal-synthesis
  - delivery-plan
navigation:
  role: supporting
  order: 100
---
# V2 Convergence Plan

Last reviewed: 2026-05-24

This document records the closeout state of the `V1` to staged-pipeline convergence effort.

The main convergence work is now complete for the supported product contract. Remaining items are observability and maintainability improvements, not product-contract blockers.

## 1. Executive read

The repository already has the right end-state result envelope for the staged pipeline.

The remaining divergence is no longer in execution-path ownership. It is concentrated in:

- broader control-plane concentration inside `AnalysisViewModel`
- remaining provider-specific and observability work beyond the now-verified staged product path

That means the convergence problem is closed at product-contract level and remains open only as bounded architecture polish.

## 2. Confirmed current-state facts

These facts are directly evidenced in code.

### 2.1 Standard analysis now routes through one orchestrator

`AnalysisViewModel.runAnalysis()` now routes standard analysis through `RunAnalysisV2UseCase`.

Relevant files:

- `app/src/main/java/com/polaralias/signalsynthesis/ui/AnalysisViewModel.kt`

### 2.2 The old staged toggle has been removed from active settings

Implication:

- the repository contract now says V2 is the intended product path
- production execution no longer treats V2 as opt-in
- active settings no longer model V2 as an optional product mode

### 2.3 The result contract already favours V2

`AnalysisResult` already supports the V2 artefact surface:

- `globalNotes`
- `rssDigest`
- `decisionUpdate`
- `fundamentalsNewsSynthesis`

Relevant file:

- `app/src/main/java/com/polaralias/signalsynthesis/domain/model/AnalysisResult.kt`

Implication:

- convergence does not require redesigning the top-level result object
- it mainly requires making V2 the single authoritative execution path

### 2.4 V2 still rests on a large deterministic substrate

The active staged orchestrator still uses the same foundational deterministic use cases for:

- candidate discovery
- tradeability filtering
- quote fetch
- ranking

V2 adds:

- shortlist gating
- targeted enrichment
- decision update
- RSS resolution and digesting
- fundamentals/news synthesis

Relevant file:

- `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RunAnalysisV2UseCase.kt`

Implication:

- the right convergence target is not “delete the deterministic core”
- it is “keep the deterministic core as V2 substrate without preserving a competing V1 product path”

## 3. Convergence target

The target end state is:

- one analysis execution path
- one current product description
- one settings model that does not frame V2 as optional
- one verification posture centred on the staged pipeline

Practical interpretation:

- V2 should own the user-facing analysis flow
- deterministic discovery/filtering/enrichment/ranking should remain as V2 substrate where still useful
- V1 should stop existing as a user-selectable competing pipeline

## 4. Main divergence points to remove

### 4.1 Settings and persistence

Completed in the current tranche:

- the V1/V2 mode toggle has been removed from active settings
- standard analysis no longer depends on a persisted branch selector

Remaining convergence:

- keep secondary docs from reintroducing dual-pipeline framing

### 4.2 Control-plane concentration in `AnalysisViewModel`

Completed in the current tranche:

- `runAnalysis()` no longer branches between V1 and V2
- V2 prerequisite handling is now the default analysis path contract
- graceful missing-key messaging is preserved

Remaining convergence:

- reduce the amount of policy and post-run side effects still concentrated in `AnalysisViewModel`
- current tranche note: provider-model discovery/alignment and analysis-completion side effects now live in dedicated services, shrinking the `runAnalysis()` control-plane surface without changing the product path

### 4.3 Legacy V1 orchestrator ownership

Completed in the current tranche:

- `RunAnalysisUseCase` is no longer part of standard analysis execution
- the legacy V1 orchestrator has been retired from production code

Remaining convergence:

- preserve V1 references only where historical context still helps explain current architecture

The important outcome is architectural truth, not whether the code is deleted in one step.

### 4.4 Documentation framing

Current divergence:

- active docs now align with the staged-only contract
- historical notes still describe the older split, but they live under archive or explicit architecture context

Required convergence:

- archive material should remain clearly historical
- public narrative should continue to lead with the staged pipeline while preserving honest status language

## 5. Recommended work sequence

### Phase 1: Make V2 the explicit default contract in code

- remove or neutralise `useStagedPipeline` as a product choice
- route `runAnalysis()` through V2 by default
- preserve current deterministic substrate inside V2 where needed

Status:

- completed in code

Acceptance signal:

- there is no user-facing “which pipeline” decision for standard analysis

### Phase 2: Extract or consolidate shared deterministic stages

- continue identifying seams inside the V2 deterministic substrate
- extract shared deterministic behaviour where duplication blocks maintainability
- keep reducing dead legacy references in tests and docs

Acceptance signal:

- one authoritative orchestrator remains for normal analysis execution

### Phase 3: Tighten documentation and compatibility notes

- confirm no active docs still present the old toggle as current behaviour
- document any compatibility assumptions only where historical context still matters
- ensure stored config still loads without corrupting app state

Acceptance signal:

- active knowledge surfaces no longer imply a dual-pipeline product

Status:

- completed in the current tranche

### Phase 4: Tighten observability of V2 artefacts

- make shortlist, decision update, RSS needs, and synthesis artefacts easier to inspect
- keep enough visibility for debugging and verification after V1 removal

Acceptance signal:

- agents and maintainers can understand why V2 selected, dropped, or expanded symbols without relying on code archaeology

## 6. Risks that convergence must not hide

### 6.1 Convergence still must not outrun proof

The old V1 execution path is no longer a production fallback, so evidence requirements now sit entirely on V2.

Implication:

- convergence should be paired with proof-building, not treated as documentation cleanup alone

### 6.2 The real seam is the control plane

`AnalysisViewModel` still owns:

- execution-path selection
- prerequisite checks
- post-run handling
- AI side-actions
- RSS and settings policy

Implication:

- V1 removal without control-plane cleanup can leave the repository with a single pipeline but still weak boundaries

### 6.3 Historical docs can outlive code truth

Archive and evidence notes can continue to mention the old toggle and V1 path long after code removal.

Implication:

- active docs need explicit maintenance so historical notes do not look current

## 7. Convergence acceptance criteria

Treat V1→staged-pipeline convergence as complete only when all are true:

- no standard analysis execution path branches between V1 and V2
- `AppSettings` no longer models V2 as optional product mode
- no active documentation surface still describes `RunAnalysisUseCase` as current product architecture
- canonical docs describe one product pipeline
- V2 has enough automated and manual proof to remain the only credible execution path

## 8. What this document does not claim

This document does not claim that:

- V2 is already fully verified
- V1 can be removed immediately with no risk
- the current ViewModel/control-plane shape is already settled

Those are separate proof and architecture concerns.

## 9. Best next implementation move

If this plan is used as a build sequence, the highest-leverage next implementation task is:

- improve artefact observability around shortlist, decision update, RSS selection, and final synthesis without widening support claims

The repository now executes only the staged path for standard analysis; the next payoff is making its decisions easier to inspect while the repo is `verified working` at product-path level.

## Repository knowledge

- [Documentation map](knowledge/documentation-map.md) — RKE-managed reading order and relationship hub.
