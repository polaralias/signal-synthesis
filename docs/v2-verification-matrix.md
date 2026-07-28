---
type: "Validation Evidence"
title: "V2 Verification Matrix"
description: "Documents V2 Verification Matrix for the signal-synthesis repository."
timestamp: 2026-07-28T21:55:36Z
authority: evidence
verification: verified-limited
owner: polaralias
tags:
  - signal-synthesis
  - validation-evidence
navigation:
  role: reference
  order: 200
---
# V2 Verification Matrix

Last reviewed: 2026-05-24

This document defines the staged pipeline in verification terms.

Goal:

- Turn `RunAnalysisV2UseCase` from "implemented code" into "auditable behaviour".
- Identify what each stage needs, what it emits, how it fails, and what evidence currently exists.

Primary code path:

- `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RunAnalysisV2UseCase.kt`

Supporting documents:

- [`codebase-map.md`](codebase-map.md)
- [`analysis-viewmodel-pipeline-map.md`](analysis-viewmodel-pipeline-map.md)

## 1. Executive read

The V2 pipeline now meets the repository threshold for `verified working`.

Current state in one sentence:

- The early deterministic market-data stages are directly evidenced.
- The staged AI gates, orchestrator, V2-first control plane, connected Android smoke path, and live-provider staged scenarios are directly evidenced.

Residual caveats remain around:

- shortlist gating
- decision update pruning
- RSS feed selection correctness
- fundamentals/news synthesis quality
- end-to-end artefact visibility

## 2. Pipeline outline

V2 stages:

1. Discover candidates
2. Filter tradeable
3. Fetch quotes
4. LLM shortlist gate
5. Targeted intraday enrichment
6. Targeted context enrichment
7. Targeted EOD enrichment
8. Rank setups
9. LLM decision update
10. RSS feed resolution and digest build
11. LLM fundamentals/news synthesis

## 3. Stage matrix

| Stage | Code | Inputs | External dependencies | Output | Failure posture | Current evidence |
|---|---|---|---|---|---|---|
| 1. Discover candidates | `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/DiscoverCandidatesUseCase.kt` | intent, risk, assetClass, discoveryMode, customTickers, screenerThresholds | Repository screener only when `SCREENER` mode is selected | `Map<String, TickerSource>` | Screener exceptions are caught and collapsed to empty screener additions; static lists still work | Direct unit test exists in `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/DiscoverCandidatesUseCaseTest.kt` |
| 2. Filter tradeable | `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/FilterTradeableUseCase.kt` | discovered symbols, minPrice | Quote providers via repository | `List<String>` | Any repository failure returns empty list | Direct unit test exists in `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/FilterTradeableUseCaseTest.kt` |
| 3. Fetch quotes | `app/src/main/java/com/polaralias/signalsynthesis/data/repository/MarketDataRepository.kt` | tradeable symbols | Quote providers, caches, blacklist state | `Map<String, Quote>` | Partial success allowed; missing quotes simply stay missing | Indirectly exercised by repository tests and the deterministic `RunAnalysisV2UseCase` contract suite |
| 4. LLM shortlist gate | `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/ShortlistCandidatesUseCase.kt` | tradeable symbols, quotes, intent, risk, maxShortlist | Stage router, LLM provider, JSON extraction | `ShortlistPlan` | Exceptions or invalid JSON produce empty shortlist plan | Direct unit coverage exists in `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/ShortlistCandidatesUseCaseTest.kt` |
| 5. Targeted intraday enrichment | `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/EnrichIntradayUseCase.kt` | shortlist-derived target symbols | Intraday providers and indicator calculators | `Map<String, IntradayStats>` | Per-symbol failures are swallowed and skipped | Direct unit test exists in `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/EnrichIntradayUseCaseTest.kt` |
| 6. Targeted context enrichment | `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/EnrichContextUseCase.kt` | shortlist-derived context targets | Profile, metrics, sentiment providers | `Map<String, SymbolContext>` | Per-subcall and per-symbol failures are swallowed; partial context is allowed | Direct unit coverage exists in `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/EnrichContextUseCaseTest.kt` |
| 7. Targeted EOD enrichment | `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/EnrichEodUseCase.kt` | shortlist-derived EOD targets | Daily providers and SMA calculator | `Map<String, EodStats>` | Per-symbol failures are swallowed and skipped | Direct unit test exists in `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/EnrichEodUseCaseTest.kt` |
| 8. Rank setups | `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RankSetupsUseCase.kt` | shortlisted symbols, quotes, intraday, eod, context, intent | None beyond supplied data | `List<TradeSetup>` | Missing quote drops symbol; other enrichment fields are optional | Direct unit test exists in `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/RankSetupsUseCaseTest.kt` |
| 9. LLM decision update | `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/UpdateDecisionsUseCase.kt` | ranked setups, intent, risk, maxKeep | Stage router, LLM provider, JSON extraction | `DecisionUpdate` | Exceptions or invalid JSON produce empty decision update; pipeline continues | Direct unit coverage exists in `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/UpdateDecisionsUseCaseTest.kt` |
| 10. RSS resolution and digest build | `app/src/main/java/com/polaralias/signalsynthesis/domain/rss/RssFeedResolver.kt`, `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/BuildRssDigestUseCase.kt` | setups, ticker source, rssNeeded flags, app RSS settings, feed catalogue | RSS feed URLs, RSS client, RSS DAO | `RssFeedResolution` and optional `RssDigest` | Digest errors are caught in V2 and downgraded to `null` digest | Direct tests exist for resolver and digest matching |
| 11. LLM fundamentals/news synthesis | `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/SynthesizeFundamentalsAndNewsUseCase.kt` | final setups, optional digest, intent, risk | Stage router, LLM provider, JSON extraction | `FundamentalsNewsSynthesis` | Exceptions or invalid JSON produce empty object or `null` in caller; pipeline still returns `AnalysisResult` | Direct unit coverage exists in `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/SynthesizeFundamentalsAndNewsUseCaseTest.kt` |

## 4. Cross-stage preconditions

These conditions must be true before V2 is meaningfully testable:

- The app has at least one usable market-data provider or mock mode.
- The app has LLM credentials for every stage used in routing.
- The selected models/providers can actually produce JSON matching the expected schemas.
- RSS catalog/defaults are coherent with current feed URLs.

Actual stage defaults come from:

- `app/src/main/java/com/polaralias/signalsynthesis/domain/ai/StageModelConfig.kt`

Important default routing fact:

- `SHORTLIST`, `DECISION_UPDATE`, and `FUNDAMENTALS_NEWS_SYNTHESIS` now default to OpenAI `gpt-5.4`, with OpenAI, Gemini, and Anthropic key setup constrained by provider model discovery when the saved key cannot access the curated default.

## 5. Residual questions by stage

These are no longer blocking the staged-product `verified working` claim, but they still matter for deeper provider breadth and user-facing quality work.

### Stage 1: Discover candidates

- Does `SCREENER` mode actually produce usable universes across supported providers?
- Are asset-class specific symbol formats valid for downstream providers?
- Are custom tickers normalised consistently with provider expectations?

### Stage 2: Filter tradeable

- Are symbols with missing quotes being dropped for the right reason?
- Are forex and metals quote semantics compatible with the same tradeability thresholds?

### Stage 3: Fetch quotes

- Do fallback providers return comparable price/volume semantics?
- Is partial quote availability acceptable, or does it silently bias the shortlist universe?

### Stage 4: LLM shortlist gate

- Does the prompt reliably return schema-valid JSON?
- Does the LLM respect `maxShortlist`?
- Are `requested_enrichment` fields stable enough to drive targeted enrichment safely?
- Does the gate systematically over-filter or under-filter certain symbol types?

### Stages 5-7: Targeted enrichment

- Does V2 lose important signals because shortlist omitted a needed enrichment type?
- Are partial enrichments reflected clearly in downstream reasoning?
- Does missing context data materially distort rank or decision-update outputs?

### Stage 8: Rank setups

- Are ranking heuristics still sensible after selective enrichment?
- Are confidence scores comparable between partially enriched and fully enriched setups?

### Stage 9: LLM decision update

- Does keep/drop behaviour improve result quality or just add noise?
- Are `must_review`, `rss_needed`, and `expanded_rss_needed` fields trustworthy enough to drive later stages?
- Are users shown enough evidence to understand why a setup disappeared?

### Stage 10: RSS resolution and digest

- Do resolved feed sets match the intended policy for custom vs predefined symbols?
- Are ticker-template feeds producing enough relevant headlines?
- Does headline matching create false positives for short tickers?

### Stage 11: Fundamentals/news synthesis

- Does synthesis meaningfully use the digest, or mostly restate setup data?
- Are ranked review outputs stable across providers/models?
- Does the output actually improve decision usefulness for the user?

## 6. Evidence inventory

What the repo currently proves reasonably well:

- Deterministic candidate discovery works at a basic level.
- Tradeability filtering works at a basic level.
- Intraday and EOD enrichment logic exist and have unit coverage.
- Context enrichment continuation behaviour is directly covered.
- Ranking logic exists and has unit coverage.
- Shortlist, decision-update, and fundamentals/news synthesis contract behaviour now have direct unit coverage.
- One deterministic `RunAnalysisV2UseCase` suite proves stable end-to-end artefact generation for the staged path.
- RSS digest matching and RSS feed resolution have direct tests.
- `AnalysisViewModel` now has direct staged-path checks for missing-key gating, progress ordering, and staged artefact publication.
- One documented live-provider staged verification run exists in `docs/v2-manual-real-provider-verification-2026-05-23.md`.

What the repo currently does not prove well:

- Quality of LLM-driven pruning.
- Stability of targeted enrichment requests across real model outputs.
- Utility of final fundamentals/news synthesis in real usage.
- Uniform success across every possible external provider combination beyond the currently verified Gemini, OpenAI, and Anthropic staged routes.

## 7. Minimum verification plan for public readiness

This is the minimum credible proof set that now backs the staged-product claim.

### A. Contract tests

Completed in the current tranche:

- `ShortlistCandidatesUseCase` with valid JSON response
- `ShortlistCandidatesUseCase` with malformed response
- `UpdateDecisionsUseCase` with keep/drop cases
- `UpdateDecisionsUseCase` with malformed response
- `SynthesizeFundamentalsAndNewsUseCase` with digest and without digest
- `RunAnalysisV2UseCase` happy path using fake stage router and fake repository

### B. End-to-end deterministic harness

Completed in the current tranche using:

- fixed candidate set
- fixed quotes
- fixed enrichment data
- fake LLM responses for shortlist, decision update, and synthesis
- fixed RSS headlines

The current harness proves:

- selected enrichment targets are honoured
- dropped symbols really disappear
- RSS expansion flags influence feed selection
- final `AnalysisResult` contains all expected artefacts

### C. Manual verification script

Completed in the current tranche:

- documented live-provider staged scenario captured in `docs/v2-manual-real-provider-verification-2026-05-23.md`
- market-data providers exercised with real keys
- Gemini-routed, OpenAI-routed, and Anthropic-routed shortlist, decision update, and fundamentals/news synthesis exercised with real keys
- resolved RSS feeds and digest matches captured as durable repo evidence

Residual caveat:

- the staged path is now directly evidenced with full live market-data runs for Gemini, OpenAI, and Anthropic
- wider external-provider combinatorics and output-quality questions still remain outside the minimum `verified working` claim

## 8. Suggested acceptance criteria by stage

If you want hard gates, these are pragmatic ones.

- Stage 1 passes when static/custom/screener modes all return expected candidate shapes.
- Stage 2 passes when invalid price/volume symbols are consistently removed.
- Stage 4 passes when 100% of fixture runs return schema-valid shortlist JSON.
- Stages 5-7 pass when targeted enrichment requests produce exactly the intended subsets.
- Stage 9 passes when keep/drop logic is reproducible against fixed fake LLM outputs.
- Stage 10 passes when RSS resolution matches policy for at least custom, predefined, and expanded-RSS cases.
- Stage 11 passes when synthesis returns schema-valid JSON with and without digest input.
- Full V2 passes when one deterministic test run produces stable `AnalysisResult` artefacts end to end.

## 9. Repository positioning implication

Right now, V2 is best described as:

"Verified working for the staged product path, with direct automated coverage across staged orchestration and stage contracts plus documented live-provider runs for Gemini, OpenAI, and Anthropic."

It is not yet best described as:

"Uniformly verified across every configured external provider combination."

That difference matters for public GitHub presentation.

## 10. Best next move

If the next pass should continue straight from here, the highest-value task is:

- improve user-visible observability of shortlist, decision-update, RSS, and synthesis artefacts

The repo now has the minimum proof threshold for `verified working`; the next payoff is making that staged behaviour easier to inspect and debug.

## Repository knowledge

- [Documentation map](knowledge/documentation-map.md) — RKE-managed reading order and relationship hub.
