# Phased Implementation Plan (Agent Guide)

This document breaks implementation into phases with goals, inputs,
tasks, outputs, and acceptance checks. Agents should complete phases in
order and update `docs/implementation/implementation_log.md` after each
milestone.

## Phase 0: Project Baseline

### Goals
- Confirm repo structure and create initial scaffolding.
- Establish shared conventions for data models and modules.

### Inputs
- `docs/implementation/implementation_guide.md`
- `docs/implementation/product_vision.md`

### Tasks
- Review existing Android project structure (or create it if missing).
- Define package naming and module boundaries (app, data, domain).
- Create placeholder directories and README notes if needed.

### Outputs
- Clean project skeleton with packages for `data`, `domain`, `ui`.
- Brief notes on structure in a README or project doc.

### Acceptance Checks
- Repo contains directories and package names consistent with the
  planned architecture.
- No build errors for the base project (if applicable).

## Phase 1: Data Models and Contracts

### Goals
- Define the Kotlin data classes and enums used across the app.
- Establish provider interfaces for consistent data fetching.

### Inputs
- Data model list from `docs/implementation/implementation_guide.md`.

### Tasks
- Create `TradingIntent` enum.
- Create data classes: `Quote`, `IntradayBar`, `DailyBar`,
  `CompanyProfile`, `FinancialMetrics`, `SentimentData`, `IntradayStats`,
  `EodStats`, `TradeSetup`, `AnalysisResult`.
- Define provider interfaces (`QuoteProvider`, `IntradayProvider`,
  `DailyProvider`, `ProfileProvider`, `MetricsProvider`,
  `SentimentProvider`).

### Outputs
- A consistent set of data models and provider interfaces in `domain`
  or `data` packages.

### Acceptance Checks
- Models compile and are referenced from a single module to prevent
  duplication.
- Provider interfaces are stable and reflect all needed endpoints.

## Phase 2: Provider Implementations and Repository

### Goals
- Implement provider clients and fallback logic.
- Add cache utilities for data reuse.

### Inputs
- Provider interfaces from Phase 1.
- API keys and provider priority list.

### Tasks
- Implement Retrofit/OkHttp clients for each provider.
- Map API responses to internal models.
- Implement repository that:
  - Selects available providers based on keys.
  - Executes calls in priority order.
  - Falls back on errors.
  - Caches short-lived data (quotes, intraday).

### Outputs
- Data layer with provider implementations and repository.
- Cache utility with TTL support.

### Acceptance Checks
- Repository can return mock or test data in isolation.
- Fallback logic and cache TTL behavior are validated with unit tests
  or a simple harness.

## Phase 3: Indicator and Enrichment Logic

### Goals
- Port technical indicators and enrichment calculations.

### Inputs
- Indicator definitions and formulas.

### Tasks
- Implement indicator functions:
  - VWAP
  - RSI (14)
  - ATR (14)
  - SMA (50, 200)
- Implement enrichment steps:
  - Intraday stats calculation.
  - EOD stats calculation.
  - Context enrichment mapping.

### Outputs
- Domain-level functions for indicators and enrichment.

### Acceptance Checks
- Unit tests validate indicator calculations against sample data.
- Enrichment functions produce expected stats without provider calls.

## Phase 4: Pipeline Orchestration

### Goals
- Implement the full analysis pipeline in Kotlin.

### Inputs
- Repository and domain logic from prior phases.

### Tasks
- Implement `discoverCandidates`.
- Implement `filterTradeable`.
- Compose steps in a `RunAnalysisUseCase`.
- Implement ranking/scoring and build `TradeSetup` list.
- Ensure results are sorted and include counts.

### Outputs
- A working pipeline with a single entry point (use case).

### Acceptance Checks
- Pipeline runs with mock data end-to-end.
- Ranking logic matches MCP server rules.

## Phase 5: ViewModel and UI

### Goals
- Create UI screens and hook them to the pipeline.

### Inputs
- ViewModel state design.
- Pipeline use case.

### Tasks
- Implement `AnalysisViewModel` with `StateFlow`.
- Build Compose screens:
  - API key setup
  - Main analysis
  - Results list
  - Setup detail
  - Settings
- Add navigation between screens.

### Outputs
- UI that can trigger analysis and display results.

### Acceptance Checks
- Run button triggers analysis and shows loading state.
- Results render correctly with sample data.

## Phase 6: Alerts and Background Work

### Goals
- Implement background monitoring and notifications.

### Inputs
- WorkManager scheduling plan.

### Tasks
- Implement `MarketAlertWorker`.
- Define alert conditions and thresholds.
- Configure notification channel and deep links.
- Add UI toggle for enabling alerts.

### Outputs
- Background checks and notifications.

### Acceptance Checks
- WorkManager schedules and runs worker on device/emulator.
- Sample alerts display correct notification content.

## Phase 7: AI Reasoning & Foundation

### Goals
- Implement the foundational AI layer that synthesizes market data.
- Ensure the default view prioritizes AI insights when keys are available.

### Inputs
- LLM API key storage and updated prompting strategy (analyst persona).

### Tasks
- Implement LLM client with structured prompting (Synthesis/Decision).
- Integrate AI calls into the analysis flow (e.g., `synthesizeSetupWithAI`).
- Update UI to display AI reasoning as the primary content in details/lists.

### Outputs
- Core AI synthesis module and updated UI presentation.

### Acceptance Checks
- AI analysis runs automatically on setup view if key is present.
- UI displays the synthesized summary prominently.
- Fallback to raw data view is available.

## Phase 8: Hardening and Testing

### Goals
- Improve reliability and coverage.

### Tasks
- Add unit tests for indicators and ranking.
- Add integration tests for repository fallback.
- Add UI tests for ViewModel state.
- Verify error handling (missing keys, provider errors).

### Outputs
- Test coverage for core logic and pipeline.

### Acceptance Checks
- Tests pass locally.
- Errors are surfaced clearly in UI.

