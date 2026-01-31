# QA Checklist: Signal Synthesis Android App

Last updated: 2026-01-31

This checklist consolidates requirements from all docs in `docs/implementation/` into one QA pass. It is structured as a full-app scaffold: each section states what must exist and how later phases depend on earlier work. Use this to validate end-to-end behavior and catch hidden dependencies (e.g., a later phase assuming settings or models from an earlier phase).

How to use this checklist:
- Run top-to-bottom for full QA.
- Use section headers for targeted checks.
- Dependencies are listed explicitly; resolve them before moving forward.

---

## 0) Preflight & Project Baseline (Phase 0 scaffolding)

- [ ] Project structure exists with `app`, `data`, `domain`, `ui` packages (or modules) and consistent package naming.
- [ ] Android SDK configured (`local.properties` with `sdk.dir`).
- [ ] Base build succeeds (at least `./gradlew testDebugUnitTest`).
- [ ] Required Gradle plugins/deps present (Room, Retrofit/OkHttp, Compose, Coroutines, etc.).
- [ ] Implementation log updated after each phase (`docs/implementation/implementation_log.md`) if used.

Dependencies:
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
- [ ] Optional providers: `EarningsProvider` (if earnings are separate), `SearchProvider` (ticker search), `ScreenerProvider` (discovery).

Dependencies:
- Phase 2 repository and provider implementations must depend on these types.
- UI Raw Data and charting assume enrichment data is preserved or re-fetchable by symbol.

---

## 2) Settings, Keys, and Storage Scaffolding

API key management:
- [ ] Key storage uses encrypted storage (Keystore/EncryptedSharedPreferences).
- [ ] API Keys screen has fields for provider keys + OpenAI + Gemini.
- [ ] Password manager support is enabled on key input fields (password keyboard type, no autocorrect).
- [ ] Clear/remove keys action exists and updates UI immediately.

App settings (persisted):
- [ ] Settings storage exists (DataStore/SharedPreferences).
- [ ] Required settings fields are present and defaulted:
  - [ ] Risk tolerance (Conservative / Moderate / Aggressive).
  - [ ] Discovery mode (static, screener, custom).
  - [ ] Screener thresholds per risk level.
  - [ ] Cache TTLs per data type.
  - [ ] Alert check interval.
  - [ ] Analysis refresh interval (if applicable).
  - [ ] `useStagedPipeline` toggle.
  - [ ] AI summary prefetch toggle + limits (if implemented).
  - [ ] Verbose logging toggle (if log viewer supports it).
  - [ ] Model routing or provider selection state (OpenAI vs Gemini).

Dependencies:
- Repository caching and alerts rely on cache TTLs and intervals.
- Pipeline selection relies on `useStagedPipeline` and model routing.
- Discovery and filtering rely on risk tolerance and screener thresholds.

---

## 3) Providers, Repository, Caching, Retry, Logging (Phase 2)

Provider implementations:
- [ ] Provider clients exist for Alpaca, Polygon, Finnhub, FMP (and Twelve Data if reintroduced).
- [ ] Provider priority list is explicit and deterministic.
- [ ] Provider selection is key-aware and ordered by priority.
- [ ] Mock provider is used when no keys exist.

Repository behavior:
- [ ] All provider calls route through repository (single decision point).
- [ ] Repository caches by data type (quotes, intraday, daily, profiles, metrics, sentiment).
- [ ] Cache TTLs are configurable via settings.
- [ ] `clearAllCaches()` exists and is wired to Settings UI.

Retry + rate limit handling:
- [ ] Retry logic exists with exponential backoff for transient errors.
- [ ] HTTP 429 handling waits and retries without consuming attempt quota.
- [ ] Retry configuration is injectable/testable.

Logging + crash reporting:
- [ ] `Logger` wrapper exists and is used in providers, repository, and ViewModels.
- [ ] Exceptions are logged with stack traces; no silent catch blocks.
- [ ] CrashReporter interface exists (even if stubbed).
- [ ] API keys are never logged.

Dependencies:
- Pipeline and UI assume repository returns empty maps on total failure (not crashes).
- Activity log and usage tracking depend on provider/repository logging hooks.

---

## 4) Discovery & Screener (Phase 4 + Phase 3 notes)

Discovery sources:
- [ ] Static fallback list exists for all intents.
- [ ] Screener integration exists for at least two providers.
- [ ] Custom tickers can be added via search UI (list-based, not comma-separated input).
- [ ] Discovery mode is user-selectable (static, screener, custom).
- [ ] Blocklist support exists (if in settings or UI).

Risk tolerance integration:
- [ ] Risk tolerance influences discovery and tradeability filtering (minPrice varies by risk).
- [ ] Aggressive mode can include sub-$1 symbols; conservative mode is stricter.
- [ ] Screener thresholds are configurable per risk level.

Transparency:
- [ ] Discovery + screener activity is logged in activity log (sanitized).

Dependencies:
- Discovery relies on settings (risk tolerance, discovery mode, thresholds).
- Custom ticker search requires a search provider or local symbol list.

---

## 5) Indicators & Enrichment (Phase 3)

Indicator calculations:
- [ ] VWAP, RSI-14, ATR-14, SMA-50, SMA-200 are correct and unit-tested.
- [ ] Intraday stats computed from 1-2 day bars.
- [ ] EOD stats computed from ~200 daily bars.

Context enrichment:
- [ ] Profile/metrics/sentiment fetched and associated per symbol.
- [ ] Earnings date captured and surfaced (for swing/long-term) if provider supports it.

Data availability for UI:
- [ ] Enrichment results are stored in a structure available to UI (e.g., in `AnalysisResult` or a persisted store).

Dependencies:
- Ranking uses `IntradayStats` + `EodStats` + `SentimentData`.
- Detail UI (Raw Data, charts) depends on these values being preserved or re-fetchable.

---

## 6) Pipeline Orchestration (V1 Single-Pass)

Core pipeline flow:
- [ ] `discoverCandidates` -> `filterTradeable` -> `getQuotes` -> `enrichIntraday` -> `enrichContext` -> `enrichEod` -> `rankSetups`.
- [ ] Candidate discovery supports static lists and custom tickers.
- [ ] Tradeability filter respects `minPrice` and volume > 0.
- [ ] Ranking logic matches MCP rules (VWAP, RSI, SMA200, sentiment).
- [ ] Confidence scoring clamp and setup type thresholds are correct.
- [ ] Validity windows are intent-aware (short for day trade).
- [ ] `AnalysisResult` includes counts and timestamp; setups are sorted by confidence.

Dependencies:
- UI assumes the pipeline returns `AnalysisResult` and is resilient to missing data.
- V2 staged pipeline assumes V1 logic parity as fallback.

---

## 7) Staged Pipeline (V2) + LLM Shortlist + RSS (Refactor Plan)

Staged pipeline selection:
- [ ] `useStagedPipeline` toggle switches V1 vs V2 execution.
- [ ] V2 requires non-empty LLM key before execution (clear error if missing).

Shortlist stage:
- [ ] LLM shortlist prompt uses `SHORTLIST_PROMPT` and returns valid JSON.
- [ ] `requested_enrichment` controls which symbols receive intraday/EOD/context enrichment.
- [ ] Shortlisted symbols always intersect with tradeable symbols.

RSS ingestion & digest:
- [ ] RSS feeds fetched with conditional GET (ETag/Last-Modified).
- [ ] Items persisted to Room; old items cleaned up.
- [ ] Digest uses time window + max items per ticker.
- [ ] Ticker matching recognizes cashtags and bare tickers.

Deep Dive:
- [ ] Deep Dive is user-triggered only.
- [ ] Tools mode is enabled only for Deep Dive.
- [ ] Sources from tool calls are merged with JSON sources in output.
- [ ] Gemini deep-dive uses `ToolsMode.GOOGLE_SEARCH` when selected.

Known gaps to verify/resolve (must be explicit if still missing):
- [ ] Decision update stage exists and is wired after enrichment.
- [ ] Fundamentals/news synthesis stage exists and is wired.
- [ ] `RunAnalysisV2UseCase.execute` uses `llmKey` meaningfully or parameter is removed.
- [ ] Deep Dive provider classes are either used or removed.

Dependencies:
- V2 requires RSS/Room setup before enabling the staged pipeline.
- Shortlist stage assumes prompts are centralized and stable.

---

## 8) AI Integration & Prompting (Phase 7 + Phase 3 additions)

LLM providers and configuration:
- [ ] Only OpenAI and Gemini are supported.
- [ ] Provider/model selection is persisted and validated.
- [ ] UI exposes exactly three controls: reasoning depth, output length, verbosity (OpenAI only).
- [ ] Gemini disables verbosity control with clear tooltip.
- [ ] Reasoning tiers enforce model support (e.g., `xhigh` only for GPT-5.2 variants).

Prompting & outputs:
- [ ] Prompts are centralized (e.g., `AiPrompts.kt`).
- [ ] Synthesis prompts require indicator names + exact values in output.
- [ ] AI summary aligns with indicators and references values used in the decision.
- [ ] Raw data remains available; AI output is the default view.

AI summary caching & prefetch:
- [ ] Top N setups are prefetch-synthesized (if enabled).
- [ ] Summaries are cached locally with TTL and invalidated if setup changes.
- [ ] Prefetch progress appears in Results UI when active.

Dependencies:
- AI screens assume LLM key storage exists and is secured.
- UI assumes AI synthesis output is structured (summary, risks, verdict).

---

## 9) UI & Navigation (Phase 5 + Phase 2/3/4 additions)

Screens & navigation:
- [ ] Dashboard is the start destination (if implemented).
- [ ] Analysis screen includes intent selection, run button, and error states.
- [ ] Results screen shows setup list with confidence labels and intent tags.
- [ ] Setup detail screen shows AI summary by default with Raw Data toggle.
- [ ] API Keys screen supports password manager autofill.
- [ ] Settings screen includes all feature toggles and thresholds.
- [ ] Alerts screen/section supports enabling/disabling and thresholds.
- [ ] Log Viewer screen shows sanitized activity history (not raw Android logs).
- [ ] Watchlist and History screens exist if Room persistence is enabled.

UI data requirements:
- [ ] Raw Data view shows RSI, ATR, VWAP, SMA values, fundamentals, sentiment, reasons, validity, and earnings date (if present).
- [ ] Each metric includes a short static explanation (what it means and why it matters).
- [ ] AI summary aligns with indicators and references values used for the decision.

Charts:
- [ ] Intraday or daily price chart is displayed in setup detail (if charting is enabled).

Intent context:
- [ ] TradeSetup intent is visible on results cards and watchlist entries.

Custom ticker labeling:
- [ ] User-added tickers are visually marked as "User Added" in all UI lists.

Dependencies:
- Results/Detail screens assume enriched data exists or is re-fetchable.
- Dashboard and settings assume a working repository + settings store.

---

## 10) Alerts & Background Work (Phase 6)

WorkManager:
- [ ] Background worker runs at configured interval and respects constraints.
- [ ] Alerts can be enabled/disabled from Settings or Alerts UI.
- [ ] Notifications are posted to "Market Alerts" channel with deep links.

Alert logic:
- [ ] VWAP/RSI/target/stop-loss triggers are supported.
- [ ] Alert conditions are based on latest quotes or minimal intraday bars.
- [ ] Worker does not spam (de-duplication or cooldown logic exists).

Settings scaffolding:
- [ ] Alert thresholds section explains each threshold and includes AI rationale.
- [ ] Alert frequency setting warns about API quota impact when decreased.

Dependencies:
- Alert frequency and thresholds must be configurable in settings.
- Watchlist/analysis history persistence should provide symbols to monitor.

---

## 11) Logging, Transparency, and Log Viewer

Logging infrastructure:
- [ ] `Logger` and `CrashReporter` used across providers, repository, and ViewModels.
- [ ] Exceptions are never silently swallowed; errors are logged with stack traces.
- [ ] API keys are never logged.

Activity / transparency log:
- [ ] Log viewer is a curated, sanitized activity feed (not raw Android logs).
- [ ] Shows input/output summaries for API requests and LLM requests.
- [ ] Does not expose secrets or system prompts.
- [ ] Includes discovery/screener activity and LLM activity.

Dependencies:
- Activity logger must be populated by repository + LLM clients.

---

## 12) API Usage Tracking & Quotas

Daily tracking:
- [ ] Usage is categorized (Discovery, Analysis, Fundamentals, Alerts, Search, Other).
- [ ] Daily per-provider counts are stored and displayed with category breakdowns.
- [ ] Daily archives persist up to 30 days and can be manually archived.

Monthly tracking (Phase 4 requirement):
- [ ] Monthly aggregate count is calculated and displayed (derived from daily archives).
- [ ] UI shows "Requests this month" alongside daily usage.
- [ ] Usage tracking includes mock provider calls (clearly labeled as mock mode).

Dependencies:
- Settings screen must render both daily and monthly usage summaries.
- Activity logger should feed usage tracker categories.

---

## 13) Mock Mode

- [ ] Mock Mode banner appears on Analysis, Results, and Dashboard when no API keys are configured.
- [ ] Banner disappears immediately when any valid API key is present.
- [ ] Mock Mode clearly states data is simulated.
- [ ] Mock Mode is logged in transparency log and API usage counts are still recorded.

Dependencies:
- Mock mode relies on provider selection and `hasAnyApiKeys` state.

---

## 14) Persistence (Room + Storage)

Room database (if enabled):
- [ ] Watchlist table persists symbols and intent.
- [ ] Analysis history table stores past results and timestamps.
- [ ] Saved setups can be re-opened without re-running analysis.
- [ ] RSS items + feed state persisted.
- [ ] AI summary cache table exists if prefetch/caching is enabled.

Storage:
- [ ] API keys stored in encrypted storage.
- [ ] App settings stored in SharedPreferences or DataStore.

Dependencies:
- Watchlist UI assumes persistence layer exists.
- History UI assumes analysis results are saved.
- RSS ingestion assumes Room is configured.

---

## 15) Testing & QA Automation

Unit tests:
- [ ] Indicators (VWAP, RSI, ATR, SMA) are covered.
- [ ] Filter tradeable edge cases (price == 1, volume == 0).
- [ ] Ranking logic with synthetic inputs.
- [ ] Timed cache behavior.
- [ ] Repository fallback logic.

Integration tests:
- [ ] Repository provider fallback sequence.
- [ ] Pipeline end-to-end with mock data.
- [ ] Staged pipeline (shortlist -> enrichment -> rank).

UI tests (Compose):
- [ ] Analysis screen intent chips, error states, loading.
- [ ] Results list rendering and detail navigation.
- [ ] Setup detail screen AI vs raw toggle.
- [ ] API keys screen field binding and save.
- [ ] Settings screen toggles, alert frequency, cache clear.

Manual tests:
- [ ] Run analysis with real API keys during market hours.
- [ ] Validate alert notifications and deep links.
- [ ] Validate mock mode flow when no keys are set.
- [ ] Validate log viewer shows sanitized entries only.

---

## 16) Summary of Critical Gaps (Resolve before release)

- [ ] Decision update stage is implemented and integrated (V2 pipeline).
- [ ] Fundamentals/news synthesis stage is implemented and integrated (V2 pipeline).
- [ ] Gemini Deep Dive tooling uses `ToolsMode.GOOGLE_SEARCH` when Gemini is selected.
- [ ] `RunAnalysisV2UseCase` uses `llmKey` or removes it.
- [ ] Monthly API usage summary is visible (Phase 4 requirement).
- [ ] Log viewer displays sanitized activity feed (not Android logs).
- [ ] Custom ticker search is list-based and user-added tickers are labeled everywhere.
- [ ] Settings include alert threshold explanations and AI rationale.
- [ ] Screener tolerance settings exist with "Ask AI for suggestions" and rationale.

---

## 17) Sign-off

- [ ] All sections above pass.
- [ ] Known gaps are resolved or explicitly deferred with rationale.
- [ ] `docs/implementation/implementation_log.md` updated with QA results.
