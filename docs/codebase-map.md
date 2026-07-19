# Signal Synthesis Codebase Map

Last reviewed: 2026-05-24

This document is a first-pass map of the repository.

Rules used for this pass:

- Documents are treated as unverified hints.
- Code is treated as intent, not guaranteed truth.
- Tests/build results are useful signals, not proof of product correctness.

## 1. What this project appears to be

From the code, this repository is a single-module Android application that tries to function as an AI-assisted market analysis tool for retail trading ideas.

Probable product goal:

- Let a user supply their own market-data API keys and optional LLM keys.
- Discover candidate tickers for a selected trading intent.
- Filter and enrich those tickers with quotes, intraday data, daily data, fundamentals, sentiment, and RSS/news context.
- Rank the results into trade setups.
- Optionally run LLM stages over those setups to shortlist, revise decisions, synthesise news/fundamentals, and generate deeper explanations.
- Persist results locally, allow watchlists/history, and run background market alerts.

The code strongly suggests that the app started as a local Android translation of an earlier MCP/server project, then expanded into a broader AI-routing and research surface.

## 2. High-level architecture

Top-level runtime shape:

- `app/`: only app module, Android/Compose.
- `docs/`: working notes, archived plans, and provider research.
- `scripts/`: small local setup/helper script(s).

Main code packages:

- `ui/`: screens, navigation, state, chart/detail presentation, settings flows.
- `domain/`: indicators, models, provider interfaces, AI abstractions, orchestration use cases.
- `data/`: provider clients, repositories, storage, database, alerts worker, RSS plumbing.
- `util/`: logging, notifications, crash reporting, JSON extraction.

Boot path:

- `MainActivity` creates Room, storage helpers, `ProviderFactory`, `WorkManager`, and the single `AnalysisViewModel`.
- `SignalSynthesisApp` owns app navigation and exposes the main surfaces.
- `AnalysisViewModel` is the real application coordinator and currently carries a very large amount of orchestration logic.

## 3. User-facing app surfaces

The app is not just an "analysis screen". It currently exposes multiple product surfaces:

- Dashboard
- Analysis
- Results
- Setup detail
- Watchlist
- History
- Market alerts
- API keys
- Settings
- Logs / shortlist harness

Implication:

- This is already beyond a prototype with one happy path.
- The ViewModel and settings model have become the de facto application boundary.

## 4. Core domain map

### 4.1 Core entities

Important model families:

- `TradingIntent`: day trade / swing / long-term framing.
- `TradeSetup`: ranked candidate with trigger/stop/target/confidence and enrichment fields.
- `AnalysisResult`: a full run output, including setups and optional staged/AI artefacts.
- `Quote`, `IntradayBar`, `DailyBar`, `IntradayStats`, `EodStats`.
- `CompanyProfile`, `FinancialMetrics`, `SentimentData`.
- `RssDigest`, `DecisionUpdate`, `FundamentalsNewsSynthesis`, `DeepDive`.

### 4.2 Active analysis pipeline

The active orchestration path is `RunAnalysisV2UseCase`:

- Larger staged pipeline with LLM gating.
- Adds AI shortlist stage before full enrichment.
- Adds AI decision update after ranking.
- Adds RSS digest construction.
- Adds AI fundamentals/news synthesis.

Interpretation:

- The staged pipeline is now the only supported product execution path.
- The deterministic discovery/filter/rank substrate still exists inside V2.

### 4.3 Candidate discovery modes

Discovery appears to support:

- Static curated universe.
- Screener-based discovery.
- Custom user-provided tickers.

The exact quality of the discovery universe is still unverified and should be treated as a follow-up research container.

## 5. Data acquisition and fallback map

### 5.1 Market-data providers

Code-backed providers currently include:

- Alpaca
- Polygon/Massive
- Finnhub
- Financial Modelling Prep
- Twelve Data
- Mock provider

Provider selection is capability-specific rather than global:

- Quotes/intraday/daily prefer more market-data-oriented providers.
- Profiles/metrics/sentiment prefer fundamental-data-oriented providers.
- Screener uses a separate priority order.
- Search has its own provider order.

`MarketDataRepository` is the operational centre here:

- It owns provider fallback.
- It owns in-memory TTL caches.
- It emits progress messages.
- It blacklists providers after 403 responses and now also cools down providers that return 429 rate-limit responses, using `Retry-After` when available.
- It aggregates partial profile/metrics data across providers in some cases.

Interpretation:

- This is one of the most important "real" subsystems in the project.
- It is also one of the highest-risk areas for hidden behaviour drift, because correctness depends on live third-party APIs.

### 5.2 AI / LLM providers

The AI side is much broader than the README suggests.

Configured provider enum includes:

- OpenAI
- Anthropic
- Gemini
- MiniMax
- OpenRouter
- Together
- Groq
- DeepSeek
- SiliconFlow
- Ollama
- LocalAI
- vLLM
- TGI
- SGLang
- Custom endpoint

The code includes:

- Per-provider API format metadata.
- Model enums and alias normalisation.
- Stage-based routing through `StageModelRouter`.
- Separate stage runners for OpenAI, Anthropic, Gemini, and generic OpenAI-compatible providers.
- Deep-dive provider logic.

Interpretation:

- The project scope expanded from "AI explanation" into "multi-provider AI routing platform inside an Android app".
- That is likely the largest scope multiplier in the repo.

## 6. Persistence and local state

### 6.1 Room database

Room stores:

- Watchlist entries
- Historical analysis results
- Cached AI summaries
- RSS feed state/items

The app database is version `3` with schema export disabled.

### 6.2 Preference-backed settings/storage

SharedPreferences-backed stores handle:

- App settings
- Alert settings
- API keys / LLM keys
- Custom tickers
- Blocklist

Verified note:

- API and LLM keys are stored via `EncryptedSharedPreferences` in `ApiKeyStore`.
- This confirms the encrypted-storage claim at the repository level, though it does not by itself prove the broader operational security posture of the app.

### 6.3 View state

`AnalysisUiState` is effectively an application snapshot and includes:

- Analysis progress and results
- Alerts/watchlist/history
- Settings and provider state
- AI summaries
- Deep dives
- RSS preview state
- Usage and provider blacklist state

Interpretation:

- The app has grown into a state-heavy single-ViewModel architecture.
- Future maintainability risk is concentrated here.

## 7. Background work and notifications

There is a real background alert loop:

- `MarketAlertWorker` pulls saved symbols and targets.
- It fetches quotes and intraday bars.
- It calculates VWAP and RSI thresholds.
- It emits local notifications for dip/overbought/oversold/target conditions.
- It can reschedule itself for high-frequency operation.

Implication:

- Alerts are not just a UI placeholder.
- They are a distinct product surface with their own operational assumptions, API cost footprint, and notification-cooldown rules.

## 8. RSS / news subsystem

RSS is no longer incidental.

The codebase contains:

- RSS feed catalog/defaults/assets
- RSS state persistence in Room
- RSS client/parser/DAO
- RSS feed resolver logic tied to ticker source and stage
- RSS digest building in the staged analysis pipeline
- RSS settings and preview flows in the UI

Interpretation:

- News ingestion is now part of the analysis model, not an afterthought.
- This deserves its own dedicated audit later because it affects both result quality and product narrative.

## 9. Documentation map and trust level

Useful active documents:

- `README.md`: current canonical entrypoint for product posture and reading order.
- `docs/archive/product_vision.md`: likely captures original direction from MCP-server conversion.
- `docs/archive/refactor-implementation-plan.md`: contains explicit discussion of gaps and mismatches.
- `docs/LLMProviders.md`: large provider research dump; useful context, not a trustworthy representation of what is implemented correctly.

Working conclusion:

- The canonical documents are now aligned with the staged-pipeline contract.
- Archive and research documents remain useful as archaeology, not as source of truth.

## 10. Initial state assessment

### 10.1 What seems solid

- Single-module Android project structure is coherent.
- Core analysis concept is consistent across README, archived docs, and executable code.
- There is a meaningful unit-test suite footprint.
- `./gradlew testDebugUnitTest` completed successfully on 2026-05-16 after warm-up.
- Provider fallback, caching, alerting, persistence, and UI surfaces are materially implemented.

### 10.2 What seems risky

- `AnalysisViewModel` is very large and appears to own too many responsibilities.
- Product scope expanded far beyond the original simple app framing.
- Multi-provider LLM support is ambitious enough that documentation drift is almost guaranteed.
- External-provider correctness is unverified in this pass.
- Some claims in docs/README are stale or incomplete.
- A passing unit suite does not validate live API behaviour, staging quality, prompt quality, or UI correctness.

### 10.3 What this is now

This is now a publish-ready public repository baseline.

Why that is now supportable:

- The repo now has a canonical documentation spine for the staged product contract.
- Deterministic staged-path tests, live-provider evidence, and connected Android tests exist.
- Active docs clearly separate canonical truth, evidence, and archive material.

Remaining caveats:

- `AnalysisViewModel` is still a concentrated control plane.
- some policy and completion side effects have now been split out into dedicated services, but the main analysis orchestration still lives in `AnalysisViewModel`
- Provider-specific staged verification breadth is now established for OpenAI, Gemini, and Anthropic, including full live market-data staged runs for all three.

## 11. Most likely "true" end goal

The strongest code-backed interpretation of the end goal is:

"A local-first Android market-analysis workbench that lets a user bring their own data-provider and LLM credentials, run a configurable screening pipeline, receive AI-assisted trade setup synthesis, review supporting context/news, and monitor results through watchlists and background alerts."

A shorter public-facing version would probably be:

"An Android app for AI-assisted market screening and trade setup synthesis using bring-your-own APIs."

## 12. Recommended research containers for follow-up

This repo is small enough that the main map is now known, but the following areas should be audited next as separate containers:

1. `AnalysisViewModel` decomposition map
   - Identify responsibility clusters, side effects, and hidden state transitions.

2. Analysis pipeline truth table
- Trace remaining convergence drift between the staged product contract and the current control plane/settings schema.

3. Provider capability audit
   - Verify each market-data provider implementation against current API docs and actual request shapes.

4. LLM routing audit
   - Confirm which providers/models are real, current, tested, and actually reachable from the app.

5. Persistence/security audit
   - Verify API key encryption, migration behaviour, and failure cases.

6. RSS subsystem audit
   - Confirm feed defaults, topic taxonomy, ticker-source resolution, and digest quality.

7. UI surface audit
   - Screen-by-screen inventory of what is genuinely production-ready versus merely present.

8. Test strategy audit
   - Distinguish pure unit tests, repository tests, serialisation tests, and missing integration coverage.

## 13. Practical repo narrative, if you had to explain it today

If forced to describe the codebase without overselling it:

"Signal Synthesis is an Android app for staged LLM-assisted market screening. It combines provider-fallback market data retrieval, a staged analysis pipeline with deterministic substrate stages, local persistence for watchlists/history/summaries/RSS state, and background alerting. The repository now has a publish-ready documentation and verification baseline, with remaining work concentrated in deeper architecture polish and broader provider evidence."
