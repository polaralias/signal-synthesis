# AnalysisViewModel And Pipeline Map

Last reviewed: 2026-05-23

This is a follow-up to `docs/codebase-map.md`.

Scope of this document:

- Decompose `AnalysisViewModel` by responsibility.
- Compare the retired V1 shape with the active `RunAnalysisV2UseCase` path where that history still explains current architecture.
- Identify what is implemented, what is coupled, and what is lightly tested.

## 1. Executive read

Two conclusions matter most:

1. `AnalysisViewModel` is the operational center of the app, not just a presentation-model layer.
2. The active staged pipeline is not a minor extension of the retired V1 shape. It is a different product mode built around LLM gating, targeted enrichment, RSS resolution, and post-ranking AI revision.

That means future stabilization work should treat:

- `AnalysisViewModel` as a system boundary problem.
- legacy V1 references as historical architecture context rather than a live product choice.

## 2. AnalysisViewModel size and shape

Current size signal:

- `app/src/main/java/com/polaralias/signalsynthesis/ui/AnalysisViewModel.kt` is about 1,559 lines.

Function surface signal:

- It contains dozens of public entry points and private helpers.
- It owns boot-time initialization, user mutations, async orchestration, cache invalidation, AI calls, persistence wiring, alert scheduling, and RSS interactions.

Practical interpretation:

- This is a coordinator, service locator, orchestration layer, and view-model combined.
- It is the single highest-leverage maintenance risk in the repo.

## 3. Responsibility decomposition

The file breaks into these real responsibility clusters.

### 3.1 Bootstrapping and live observers

Owned here:

- Initial loading of keys
- Initial loading of alert settings
- Watchlist observation
- History observation
- App settings observation
- Usage observation
- Provider blacklist observation

Relevant methods:

- `init`
- `refreshKeys`
- `refreshAlerts`
- `observeWatchlist`
- `observeHistory`
- `observeAppSettings`
- `observeDailyUsage`
- `observeProviderBlacklist`

Meaning:

- The ViewModel is responsible for hydrating most of the app shell.

### 3.2 Analysis execution orchestration

Owned here:

- Validation before run
- V2 prerequisite validation and execution kickoff
- Repository creation/caching
- Progress updates
- Post-run persistence
- Alert target generation
- Notification triggering
- AI summary prefetch kickoff

Relevant methods:

- `runAnalysis`
- `cancelAnalysis`
- `togglePause`
- `getRepository`
- `buildSynthesisUseCase`

Meaning:

- The app’s most important business flow is wired directly in the ViewModel, not behind a single orchestration service.

### 3.3 LLM-triggered user actions

Owned here:

- Per-setup AI summary request
- Deep dive request
- Threshold suggestion
- Screener suggestion
- Full settings suggestion
- Shortlist harness

Relevant methods:

- `requestAiSummary`
- `requestDeepDive`
- `suggestThresholdsWithAi`
- `suggestScreenerWithAi`
- `suggestSettingsWithAi`
- `runShortlistHarness`

Meaning:

- The ViewModel is also the AI application layer.

### 3.4 Settings mutation and invalidation rules

Owned here:

- Updating app settings
- Updating per-stage model routing
- RSS settings mutation
- Determining when AI summaries/deep dives are invalidated

Relevant methods:

- `updateAppSettings`
- `updateStageConfig`
- `toggleRssTopic`
- `toggleRssTickerSource`
- `updateRssUseTickerFeedsForFinalStage`
- `updateRssApplyExpandedToAll`
- `resetRssDefaults`
- `shouldInvalidateAiOutputs`

Meaning:

- This file contains policy, not just UI state handling.

### 3.5 Alert, watchlist, and blocklist management

Owned here:

- Alert enable/disable
- Alert symbol removal
- Blocklist changes
- Watchlist toggles
- History clearing

Relevant methods:

- `updateAlertsEnabled`
- `removeAlert`
- `addToBlocklist`
- `removeFromBlocklist`
- `toggleWatchlist`
- `clearHistory`

Meaning:

- This is also the local operations/admin layer for user-maintained lists.

### 3.6 Market-data side interactions

Owned here:

- Ticker search
- Chart data fetch
- Cache clearing

Relevant methods:

- `searchTickers`
- `requestChartData`
- `clearCaches`

Meaning:

- The ViewModel still directly brokers repository reads for secondary surfaces.

### 3.7 AI routing and model policy helpers

Owned here:

- Stage routing defaults
- Provider/model normalization
- Token sizing
- Missing-key checks
- Cache key generation for AI summaries

Relevant methods:

- `effectiveRouting`
- `resolveModelForProvider`
- `tokensForLength`
- `missingLlmProvidersForStages`
- `hasKeyForProvider`
- `hasConfiguredLlmAccess`
- `buildAiSummaryCacheKey`

Meaning:

- The ViewModel contains provider policy and AI routing behavior that would normally live deeper in a service/configuration layer.

### 3.8 Parsing and transformation helpers

Owned here:

- Parsing AI settings suggestion payloads
- Parsing threshold and screener suggestion JSON
- Risk normalization
- Text explanation composition

Relevant methods:

- `parseAiSettingsSuggestion`
- `parseAiThresholdSuggestion`
- `parseAiScreenerSuggestion`
- `normalizeRiskTolerance`
- `buildSettingsSuggestionExplanation`

Meaning:

- Response interpretation logic is mixed into orchestration.

## 4. What this coupling implies

The coupling is not abstract; it creates concrete operational risks:

- Settings changes can invalidate AI outputs because routing and RSS policy live here.
- Alert behavior depends on analysis output post-processing in the same file.
- Some persistence writes happen after analysis completion in the same coroutine as result publication.
- A future bug in AI settings or RSS policy can destabilize the main analysis path because the boundaries are weak.

This does not mean the code is bad. It means the control plane is concentrated.

## 5. Pipeline comparison: retired V1 shape vs active V2

The old and current orchestrators are related, but they were not equivalent.

### 5.1 Shared foundation

The retired V1 shape and the active V2 pipeline share the same early substrate:

- Candidate discovery
- Tradeability filtering
- Quote fetch
- Ranking based on `RankSetupsUseCase`
- Same repository/provider fallback mechanisms
- Same result envelope shape: `AnalysisResult`

Shared files:

- `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RunAnalysisV2UseCase.kt`
- `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/DiscoverCandidatesUseCase.kt`
- `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RankSetupsUseCase.kt`

### 5.2 Retired V1 flow

V1 stages:

1. Discover candidates
2. Filter tradeable
3. Fetch quotes
4. Enrich intraday
5. Enrich context
6. Enrich EOD when not day-trading
7. Rank setups

Characteristics:

- All tradeable symbols are enriched.
- No LLM is required.
- No RSS digest is constructed.
- No post-ranking AI pruning occurs.
- No `globalNotes`, `decisionUpdate`, or `fundamentalsNewsSynthesis` content is added.

Interpretation:

- This is historical architecture context for understanding convergence, not an active product path.

### 5.3 V2 flow

V2 stages:

1. Discover candidates
2. Filter tradeable
3. Fetch quotes
4. LLM shortlist gate
5. Targeted intraday enrichment
6. Targeted context enrichment
7. Targeted EOD enrichment
8. Rank shortlisted setups
9. LLM decision update
10. RSS resolution and digest build
11. LLM fundamentals/news synthesis

Characteristics:

- Uses quotes-only first, then asks the LLM which names deserve deeper enrichment.
- Enrichment is selective rather than universal.
- Shortlist items can request enrichment types individually.
- Decision update can keep/drop setups and annotate them with setup bias and review flags.
- RSS is resolved based on ticker source and stage policy.
- Final result can contain global notes, decision update, RSS digest, and synthesis output.

Interpretation:

- V2 is an AI-mediated pipeline, not just AI commentary attached to V1.

## 6. Key behavioral difference: targeted enrichment

This is the most important structural difference.

V1:

- Enriches all tradeable symbols the same way.

V2:

- Gets only lightweight quote data first.
- Sends quotes and intent/risk to `ShortlistCandidatesUseCase`.
- Uses returned `requestedEnrichment` fields to decide which shortlisted names get:
  - intraday
  - context/fundamentals/sentiment
  - EOD

Relevant files:

- `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/ShortlistCandidatesUseCase.kt`
- `app/src/main/java/com/polaralias/signalsynthesis/domain/model/ShortlistPlan.kt`

Implication:

- V2’s promise is lower API cost and more selective analysis.
- V2’s risk is that a weak shortlist stage can hide good candidates before enrichment ever happens.

## 7. Key behavioral difference: post-ranking revision

After ranking, V2 does not trust the heuristic rank output as final.

It sends setups into `UpdateDecisionsUseCase`, which can:

- keep symbols
- drop symbols
- add setup bias
- add must-review flags
- indicate RSS need
- indicate expanded RSS need and rationale

Relevant file:

- `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/UpdateDecisionsUseCase.kt`
- `app/src/main/java/com/polaralias/signalsynthesis/domain/model/DecisionUpdate.kt`

Implication:

- In V2, ranking is intermediate, not final.
- That makes explainability and reproducibility weaker unless logs or artifacts are surfaced clearly.

## 8. Key behavioral difference: RSS/news as a pipeline dependency

V1 has no RSS stage.

V2:

- Resolves feeds based on ticker source and stage policy.
- Builds a digest over selected feeds.
- Passes digest content into fundamentals/news synthesis.

Relevant files:

- `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/BuildRssDigestUseCase.kt`
- `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/SynthesizeFundamentalsAndNewsUseCase.kt`

Implication:

- News context is now part of the decision model in V2.
- RSS feed quality now directly affects final synthesis quality.

## 9. Failure behavior comparison

V1 failure posture:

- If core market-data stages fail, analysis fails or yields fewer setups.
- No dependency on LLM provider availability.

V2 failure posture:

- Requires staged LLM provider availability before run.
- If shortlist returns empty, the pipeline can legally terminate with no setups.
- If RSS digest fails, the pipeline logs and continues with `null` digest.
- If fundamentals/news synthesis fails, the pipeline logs and continues with `null` synthesis.
- Decision update and shortlist are structurally more important than final synthesis.

Implication:

- V2 is more graceful in late-stage optional AI failures.
- V2 is more fragile in early-stage LLM gating failures.

## 10. Discovery and ranking truth, as currently implemented

Important reality check:

- Discovery is not "smart" by default.
- Static mode mostly uses curated symbol lists plus risk-based additions/removals.
- Screener mode exists and delegates to repository providers.

Relevant file:

- `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/DiscoverCandidatesUseCase.kt`

Ranking truth:

- Ranking is still heuristic and score-based.
- Signals include VWAP, RSI, SMA-200, sentiment, and an earnings-related penalty.
- Validity horizon varies by trading intent.

Relevant file:

- `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RankSetupsUseCase.kt`

Implication:

- The non-LLM baseline is still a curated-watchlist-plus-heuristics engine.
- The app’s novelty is increasingly in orchestration, routing, and enrichment policy rather than raw quant logic.

## 11. Test coverage signal from this pass

Confirmed test coverage exists for:

- discovery
- tradeability filter
- intraday enrichment
- EOD enrichment
- ranking
- deterministic V2 orchestration
- stage-contract behavior for shortlist, decision update, and fundamentals/news synthesis
- RSS digest matching
- RSS feed resolver
- ViewModel staged-path behavior

Relevant tests:

- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/DiscoverCandidatesUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/FilterTradeableUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/EnrichIntradayUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/EnrichEodUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/RankSetupsUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/RunAnalysisV2UseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/ShortlistCandidatesUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/UpdateDecisionsUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/SynthesizeFundamentalsAndNewsUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/BuildRssDigestUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/rss/RssFeedResolverTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/ui/AnalysisViewModelTest.kt`

Coverage that appears thin or absent from this pass:

- broader architectural decomposition of `AnalysisViewModel` is still unproven
- provider-by-provider staged verification breadth is still limited

Implication:

- The highest-scope path is not the best-tested path.

## 12. What should be investigated next

If the goal is public-repo hardening, the next best follow-up is:

1. ViewModel cut-lines
   - Identify seams where behavior could be extracted later without changing product behavior yet.

2. Documentation and observability cleanup
   - Keep active docs aligned with the staged-only product path.
   - Make staged AI artifacts easier to inspect without relying on logs.

3. AI artifact observability
   - Determine how shortlist, keep/drop decisions, and RSS-driven synthesis should be exposed to users and maintainers.

## 13. Short practical conclusion

If you need a plain description:

`AnalysisViewModel` currently functions as the app’s control plane.

The active product path is an AI-gated analysis workflow that uses LLM stages to decide what to enrich, what to keep, what RSS context to collect, and how to summarize final setups.

That means the repository’s hardest problems are no longer "how do we compute RSI?" They are:

- "How do we finish removing dead compatibility state and stale architecture narrative?"
- "How do we verify staged AI behavior?"
- "How do we reduce control-plane coupling without breaking working paths?"
