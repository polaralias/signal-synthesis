# V2 Proof Gap

Last reviewed: 2026-05-24

This document turns the V2 verification matrix into an execution-facing proof gap for agent-led development.

Use it to answer one question:

"What residual caveats remain now that V2 has enough proof for a `verified working` staged-product claim?"

## 1. Executive read

V2 is implemented and executable, and the original blocking proof gap has now been closed for the staged product claim.

The biggest remaining caveats are now:

- broader external-provider combination breadth beyond the currently verified Gemini, OpenAI, and Anthropic staged routes
- broader proof around real-world RSS quality and user-facing artifact observability

## 2. Current evidence, confirmed

The repository currently has direct tests for:

- candidate discovery
- tradeability filtering
- intraday enrichment
- context enrichment continuation behavior
- EOD enrichment
- ranking
- shortlist stage contract behavior
- decision-update contract behavior
- fundamentals/news synthesis contract behavior
- deterministic `RunAnalysisV2UseCase` orchestration
- RSS feed resolution
- RSS digest matching
- `AnalysisViewModel` smoke behavior
- staged-path missing-LLM-key guard behavior
- staged-path progress ordering and staged artifact publication behavior

The repository also now has a documented live-provider staged verification note:

- `docs/v2-manual-real-provider-verification-2026-05-23.md`

Relevant files:

- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/DiscoverCandidatesUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/FilterTradeableUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/EnrichIntradayUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/EnrichContextUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/EnrichEodUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/RankSetupsUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/ShortlistCandidatesUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/UpdateDecisionsUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/SynthesizeFundamentalsAndNewsUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/RunAnalysisV2UseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/BuildRssDigestUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/rss/RssFeedResolverTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/ui/AnalysisViewModelTest.kt`

## 3. Current harness reality

The repository still contains `IntegrationTestHarness`, and it remains only a shortlist demo.

That is no longer the only end-to-end proof surface, because the repo now also has a deterministic `RunAnalysisV2UseCase` contract suite.

Why it is not enough:

- it only exercises shortlist behavior
- it uses random quote values
- it does not assert outcomes
- it does not cover decision update, RSS, or synthesis
- it does not produce a stable reproducible artifact

Relevant file:

- `app/src/test/java/com/polaralias/signalsynthesis/util/IntegrationTestHarness.kt`

Implication:

- the repository has a useful manual demo hook
- deterministic end-to-end proof now exists in automated tests
- the manual demo hook still does not replace a documented live-provider verification note

## 4. Minimum proof set required by current repository policy

By repository policy, `verified working` requires both:

1. meaningful deterministic automated coverage for the claimed path
2. a documented manual real-provider scenario for externally dependent stages

That means V2 cannot be upgraded on tests alone.

## 5. Concrete proof backlog

### 5.1 Stage contract tests

Completed in the current tranche:

- `ShortlistCandidatesUseCase`
  - valid schema-compliant JSON
  - malformed response fallback
  - shortlist clamping at `maxShortlist`

- `UpdateDecisionsUseCase`
  - keep path
  - drop path
  - malformed response fallback
  - symbol normalization

- `SynthesizeFundamentalsAndNewsUseCase`
  - digest present
  - digest absent
  - malformed response fallback

- `EnrichContextUseCase`
  - partial failures for profile/metrics/sentiment
  - per-symbol continuation behavior

Remaining value in this area is incremental rather than foundational.

### 5.2 Deterministic V2 orchestrator test

Completed in the current tranche:

- fixed discovered symbols
- fixed quotes
- fixed enrichment payloads
- fake shortlist response
- fake decision-update response
- fixed RSS headlines
- fake synthesis response

The current suite asserts:

- shortlisted symbols are correctly normalized and filtered to the tradeable universe
- requested enrichment subsets are honored
- setups dropped by decision update disappear
- RSS expansion flags affect feed resolution inputs
- final `AnalysisResult` includes expected V2 artifacts

### 5.3 Control-plane tests

Completed in the current tranche:

- missing LLM provider keys block staged analysis with the expected message
- V2 progress stages appear in order across the staged path
- V2 result publication preserves `decisionUpdate`, `rssDigest`, and `fundamentalsNewsSynthesis`
- staged-path completion still saves history and publishes the results navigation event

Remaining value in this area is now architectural rather than proof-foundational:

- making staged artifacts easier to inspect remains a future architecture/observability step

### 5.5 Local Android verification baseline

Completed in the current tranche:

- debug assembly succeeds locally
- JVM unit tests succeed locally
- connected Android tests now succeed on the local `Medium_Phone_API_36` emulator

This does not widen the staged-product contract by itself, but it closes the previous "device/emulator verification still missing" caveat for the local publish baseline.

### 5.4 Manual real-provider verification artifact

Completed in the current tranche:

- documented real-provider staged scenarios now exist at `docs/v2-manual-real-provider-verification-2026-05-23.md`
- successful runs now cover real market-data providers plus Gemini-routed, OpenAI-routed, and Anthropic-routed LLM stages
- the note also records the intermediate OpenAI failures that were resolved by increasing client transport timeouts and the Anthropic timeout hardening needed for stable live reruns

## 6. Acceptance criteria for upgrading V2

V2 was ready to move to `verified working` once all were true:

- dedicated automated tests exist for the LLM-driven stage use cases
- at least one deterministic `RunAnalysisV2UseCase` test proves stable end-to-end artifact generation
- `AnalysisViewModel` tests cover the V2-first execution contract materially better than smoke level
- one documented manual real-provider verification note exists using the existing template
- canonical docs are updated to reflect the stronger evidence level

## 7. Best next proof move

The single highest-value next proof task is:

- deepen provider-specific evidence beyond the currently verified Gemini, OpenAI, and Anthropic scenarios

Why this now:

- the blocking live-provider evidence gap is now closed
- the next uncertainty is breadth across combinations and output quality, not the base staged-product claim
- additional provider-specific runs would narrow caveats without changing the current truthful product status

TDD kickoff decision:

- begin development with the deterministic `RunAnalysisV2UseCase` contract suite as the first proof-building slice
- do not start the next tranche by removing the V1/V2 branch in `AnalysisViewModel`
- add direct stage-use-case tests after the orchestrator harness establishes the end-to-end contract

## 8. What this document does not claim

This document does not claim that:

- current V2 outputs are low quality
- live providers are broken
- the existing demo harness has no value

It only claims that the staged product claim is now strong enough for `verified working`, while broader provider-specific and observability questions remain.
