# QA Checklist: Signal Synthesis Android App

Last updated: 2026-01-31

This checklist consolidates requirements from all docs in `docs/implementation/` into one QA pass. It is structured as a full-app scaffold: each section states what must exist and how later phases depend on earlier work. Use this to validate end-to-end behavior and catch hidden dependencies (e.g., a later phase assuming settings or models from an earlier phase).

How to use this checklist:
- Run top-to-bottom for full QA.
- Use section headers for targeted checks.
- **Dependencies** are listed explicitly; resolve them before moving forward.
- **Scaffolding** requirements highlight UI/Storage elements that must exist for functional code to be testable.

---

## 0) Preflight & Project Baseline (Phase 0 scaffolding)

- [ ] Project structure exists with `app`, `data`, `domain`, `ui` packages (or modules) and consistent package naming.
- [ ] Android SDK configured (`local.properties` with `sdk.dir`).
- [ ] Base build succeeds (at least `./gradlew testDebugUnitTest`).
- [ ] Required Gradle plugins/deps present (Room, Retrofit/OkHttp, Compose, Coroutines, KSP for Room).
- [ ] Implementation log updated after each phase (`docs/implementation/implementation_log.md`).

**Dependencies:**
- All later phases assume the packages/modules above exist and are referenced consistently.
- UI and data layers assume the dependency graph is wired (e.g., ViewModels can access repository, settings store, etc.).

---

## 1) Core Domain Models & Contracts (Phase 1)

Data models exist and compile:
- [ ] `TradingIntent` enum (Day Trade, Swing, Long Term).
- [ ] `Quote`, `IntradayBar`, `DailyBar`.
- [ ] `CompanyProfile`, `FinancialMetrics`, `SentimentData`.
- [ ] `IntradayStats`, `EodStats`.
- [ ] `TradeSetup` (includes `intent`, `reasons`, `confidence`, `validUntil`).
- [ ] `AnalysisResult` (counts + list of setups + timestamp).
- [ ] Optional: structures for enrichment maps (e.g., symbol -> `IntradayStats`, `EodStats`, `ContextData`) or an equivalent UI-accessible store.

Provider interfaces exist:
- [ ] `QuoteProvider`, `IntradayProvider`, `DailyProvider`, `ProfileProvider`, `MetricsProvider`, `SentimentProvider`.
- [ ] Optional: `SearchProvider` (ticker search), `ScreenerProvider` (discovery).

**Dependencies:**
- Phase 2 repository and provider implementations must depend on these types.
- UI Raw Data and charting assume enrichment data is preserved or re-fetchable by symbol.

---

## 2) Settings, Keys, and Storage Scaffolding (Phase 2 & 3 Support)

**Scaffolding Requirement:** Settings storage must be implemented *before* repository caching and pipeline toggles can be functional.

API key management:
- [ ] Key storage uses encrypted storage (DataStore with Tink or EncryptedSharedPreferences).
- [ ] API Keys screen has fields for Alpaca (Key + Secret), Polygon, Finnhub, FMP, OpenAI, and Gemini.
- [ ] Password manager support is enabled on key input fields (password keyboard type, no autocorrect).
- [ ] Clear/remove keys action exists and updates UI immediately.

App settings (persisted):
- [ ] Settings storage exists (DataStore/SharedPreferences).
- [ ] Required settings fields are present and defaulted:
  - [ ] Risk tolerance (Conservative / Moderate / Aggressive).
  - [ ] Asset Class (Stocks, Forex, Crypto - for future proofing).
  - [ ] Discovery mode (static, screener, custom).
  - [ ] Screener thresholds per risk level (Conservative, Moderate, Aggressive).
  - [ ] Refresh intervals (Quotes, Alerts).
  - [ ] Cache TTLs per data type (Quotes, Intraday, Daily, Profile, Metrics, Sentiment).
  - [ ] `useStagedPipeline` toggle (V1 vs V2).
  - [ ] `useMockDataWhenOffline` toggle.
  - [ ] AI summary prefetch toggle + limits.
  - [ ] Verbose logging toggle (for Log Viewer).
  - [ ] User Model Routing (per-stage model selection).

**Dependencies:**
- Repository caching and alerts rely on cache TTLs and intervals.
- Pipeline selection (V2) relies on `useStagedPipeline` and model routing.
- Discovery and filtering rely on risk tolerance and screener thresholds.

---

## 3) Providers, Repository, Caching, Retry, Logging (Phase 2)

Provider implementations:
- [ ] Provider clients exist for Alpaca, Polygon, Finnhub, FMP.
- [ ] Mapping logic converts API responses (e.g., `FmpQuote`) to domain models (`Quote`).
- [ ] Mock provider is used as a fallback or when no keys exist (Mock Mode).

Repository behavior:
- [ ] All provider calls route through `MarketDataRepository` (single decision point).
- [ ] Repository selects providers based on availability (API keys) and priority list.
- [ ] Repository implements `TimedCache` for all data types.
- [ ] `clearAllCaches()` exists and is wired to Settings UI.

Retry + rate limit handling:
- [ ] `RetryHelper` implements exponential backoff for transient errors (SocketTimeout, etc.).
- [ ] HTTP 429 (Rate Limit) handling waits (default 1 min) and retries *without* consuming attempt quota.
- [ ] Retry configuration is injectable/testable.

Logging + crash reporting:
- [ ] `Logger` wrapper exists and is used in providers, repository, and ViewModels.
- [ ] Exceptions are logged with stack traces; no silent catch blocks.
- [ ] API keys/secrets are NEVER logged (sanitized).

**Dependencies:**
- Pipeline and UI assume repository returns empty maps on total failure (not crashes).
- Log viewer depends on repository logging hooks.

---

## 4) Discovery & Screener (Phase 4)

Discovery sources:
- [ ] Static fallback list exists for all intents (Day Trade, Swing, Long Term).
- [ ] Screener integration (e.g., FMP) replaces static list when enabled.
- [ ] Custom tickers can be added via Search UI (list addition, not comma-separated).
- [ ] Blocklist support exists (per-ticker exclusion).

Risk tolerance & Tiered Discovery:
- [ ] Risk tolerance influences discovery:
  - Conservative: Blue chips, low volatility.
  - Aggressive: Includes penny stocks (sub-$1), high volatility.
- [ ] Tradeability filter respects `minPrice` derived from risk (e.g., $1.0 vs $0.10).
- [ ] User-added tickers are visually marked as "User Added" in all UI lists.

**Dependencies:**
- Discovery relies on Settings (risk, discovery mode, screener thresholds).
- Custom ticker search requires `SearchProvider` integration.

---

## 5) Indicators & Enrichment (Phase 3)

Indicator calculations:
- [ ] VWAP, RSI-14, ATR-14, SMA-50, SMA-200 are correct and unit-tested.
- [ ] Intraday stats computed from 1-2 day bars.
- [ ] EOD stats computed from ~200 daily bars.

Context enrichment:
- [ ] Profile/metrics/sentiment fetched and associated per symbol.
- [ ] Earnings date captured and surfaced (crucial for swing/long-term analysis).

Data availability for UI:
- [ ] Enrichment results are stored in `AnalysisResult` or a persisted store for Raw Data view.

**Dependencies:**
- Ranking uses `IntradayStats` + `EodStats` + `SentimentData`.
- Detail UI (Raw Data, charts) depends on these values being preserved.

---

## 6) Pipeline Orchestration (V1 Single-Pass)

Core pipeline flow:
- [ ] `RunAnalysisUseCase` flow: `discover` -> `filterTradeable` -> `getQuotes` -> `enrichIntraday` -> `enrichContext` -> `enrichEod` -> `rankSetups`.
- [ ] Ranking logic matches MCP rules (VWAP, RSI, SMA200, sentiment).
- [ ] Confidence scoring clamp and setup type thresholds are correct.
- [ ] `AnalysisResult` includes counts, sorted setups, and timestamp.

**Dependencies:**
- UI assumes the pipeline returns `AnalysisResult` and handles loading/error states.
- V2 staged pipeline assumes V1 logic parity as fallback.

---

## 7) Staged Pipeline (V2) + LLM Shortlist + RSS (Refactor Plan)

**Scaffolding Requirement:** Room database (RSS tables) and `StageModelRouter` must be initialized before switching to V2.

Staged pipeline selection:
- [ ] `useStagedPipeline` toggle in Settings switches V1 vs V2 execution.
- [ ] V2 requires non-empty LLM key before execution (explicit UI error).

Shortlist stage (LLM Gate):
- [ ] LLM shortlist prompt returns valid JSON (`ShortlistPlan`).
- [ ] `requested_enrichment` (INTRADAY, EOD, FUNDAMENTALS, SENTIMENT) drives targeted enrichment calls.
- [ ] Shortlisted symbols strictly intersect with tradeable symbols.

RSS ingestion & digest:
- [ ] RSS feeds fetched with conditional GET (ETag/Last-Modified).
- [ ] Digest uses ticker matching (cashtags `$AAPL` and bare tickers `\bAAPL\b`).
- [ ] Digest respects time window and max items per ticker limit.

Deep Dive (User Triggered):
- [ ] User-triggered Deep Dive uses `AnalysisStage.DEEP_DIVE`.
- [ ] Tool usage:
  - OpenAI uses `web_search` tool (Responses API).
  - Gemini uses `ToolsMode.GOOGLE_SEARCH`.
- [ ] Grounding sources are merged with JSON sources in the final output.

**Known Gaps (Must be verified):**
- [ ] `UpdateDecisionsUseCase` (Decision Update stage) is implemented and calls happen after enrichment.
- [ ] `SynthesizeFundamentalsAndNewsUseCase` (News synthesis) is implemented.
- [ ] `RunAnalysisV2UseCase` executes each stage in order: Shortlist -> Target Enrichment -> RSS Digest -> Decision Update -> News Synthesis -> Rank.

---

## 8) AI Integration & Prompting (Phase 7 + Phase 8)

LLM providers and configuration:
- [ ] Only OpenAI and Gemini are supported providers.
- [ ] UI exposes exactly three controls: **Reasoning Depth**, **Output Length**, **Verbosity**.
- [ ] Gemini disables verbosity control (not supported) with a tooltip.
- [ ] Model routing allows selecting specific models (e.g., GPT-5.2 for Deep Dive, GPT-5 Mini for Shortlist).

Prompting & Outputs:
- [ ] Prompts are centralized in `AiPrompts.kt` (no hardcoded templates in UseCases).
- [ ] Synthesis prompts require AI to explicitly mention indicator names and values.
- [ ] JSON extraction is robust (handles markdown code blocks or surrounding text).

AI summary caching & prefetch:
- [ ] Top N setups are prefetch-synthesized (if enabled).
- [ ] Progress indicator shows "Processing AI Summary" in Results list.

**Dependencies:**
- AI screens assume LLM key storage exists.
- UI assumes AI synthesis output is structured (summary, risks, verdict).

---

## 9) UI & Navigation (Phase 5)

Screens & navigation:
- [ ] Dashboard: Summary of active alerts, recent analysis, and mock mode banner.
- [ ] Analysis: Intent chips, discovery mode selection, and "Run" button.
- [ ] Results: Setup list with confidence labels, intent tags, and "Processing" states.
- [ ] Setup Detail: AI Summary primary view with "Show Raw Data" toggle.
- [ ] Raw Data View: Lists RSI, ATR, VWAP, SMA values with static educational explanations.
- [ ] Settings: Comprehensive list of toggles, thresholds, and key edits.
- [ ] Log Viewer: Sanitized activity feed (Requests/Responses), not raw Android logs.

Charts:
- [ ] Interactive price chart (Intraday/Daily) in Setup Detail using cached enrichment data.

Intent Context:
- [ ] All lists/cards show the intent (Day Trade, Swing, Long Term) for the setup.

**Dependencies:**
- Background tasks (Alerts) require WorkManager initialization.
- Log Viewer requires `ActivityLogger` to be wired into Repository/LLM runners.

---

## 10) Alerts & Background Work (Phase 6)

WorkManager:
- [ ] `MarketAlertWorker` runs at configured interval (default 15 mins).
- [ ] Alert frequency setting warns about API quota impact when decreased.
- [ ] Notifications post to "Market Alerts" channel with deep links to Setup Detail.

Alert logic:
- [ ] Supports conditions: VWAP cross, RSI thresholds, and Price targets.
- [ ] Alert cooldown/de-duplication prevents spamming.

**Dependencies:**
- Alert settings must be configurable (Thresholds, Frequency).
- Persistence layer (Watchlist) provides symbols for monitoring.

---

## 11) Logging, Transparency, and Log Viewer

Activity Log Sanitization:
- [ ] Log Viewer displays a curated list of activity (e.g., "Alpaca Quote Fetched", "LLM Shortlist Generated").
- [ ] Details view for each log entry shows inputs/outputs (JSON) but masks API Keys and User IDs.
- [ ] Does not reveal system prompts or internal routing logic.

**Dependencies:**
- Repository and LLM Runners must report events to the `ActivityLogger`.

---

## 12) API Usage Tracking & Quotas

Daily & Monthly tracking:
- [ ] Requests categorized by type (Discovery, Analysis, Fundamentals, Alerts, Deep Dive).
- [ ] Monthly aggregate is displayed in Settings to warn of provider limits.
- [ ] Mock provider calls are tracked but labeled "Mock".

**Dependencies:**
- Repository must increment usage counters on every network call.

---

## 13) Mock Mode

- [ ] Mock Mode banner appears on Dashboard and Analysis screens when no keys are present.
- [ ] Provider selection logic prioritizes MockProvider if `hasAnyApiKeys` is false.
- [ ] Banner links directly to API Keys setup.

---

## 14) Persistence (Room + Storage)

Room Database:
- [ ] `Watchlist` table (Symbol, Intent).
- [ ] `AnalysisHistory` table (JSON blob of results, Timestamp).
- [ ] `RssItems` + `RssFeedStates` tables.
- [ ] `AiSummaryCache` table (Symbol, Model, PromptHash, Summary).

---

## 15) Testing & QA Automation

- [ ] **Unit Tests**: Coverage for Indicators, Filter logic, Ranking, and JSON Parsers.
- [ ] **Integration Tests**: Repository fallback sequence, Staged pipeline execution with mock LLM.
- [ ] **UI Tests**: Compose tests for Analysis chips, Screen navigation, and Error Dialogs.
- [ ] **Manual Testing**: Validate Rate Limit (429) delay, ETag (304) RSS fetching, and Deep Link navigation.

---

## 16) Summary of Critical Gaps (Resolve before release)

- [ ] **Decision Update Stage**: Missing `UpdateDecisionsUseCase` and pipeline integration.
- [ ] **News Synthesis Stage**: Missing `SynthesizeFundamentalsAndNewsUseCase`.
- [ ] **Gemini Deep Dive Tools**: `ToolsMode.GOOGLE_SEARCH` must be default for Gemini Deep Dives.
- [ ] **V2 LLM Key**: `RunAnalysisV2UseCase` should use provided `llmKey` parameter or remove it.
- [ ] **Log Viewer Sanitization**: Ensure no raw Android system logs are visible.
- [ ] **Custom Tickers**: Search must be list-based and labeled "User Added" in Detail view.

---

## 17) Sign-off

- [ ] All sections above pass.
- [ ] Known gaps are resolved or explicitly deferred.
- [ ] `docs/implementation/implementation_log.md` updated.
