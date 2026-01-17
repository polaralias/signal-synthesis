# Implementation Guide: Signal Synthesis Android App

This document translates the existing MCP server plan into an
implementation-focused guide for building the Android app. It is based
on `docs/implementation/product_vision.md` and keeps parity with the
current MCP pipeline while mapping each part to mobile architecture,
data flow, and code structure.

## 1. Goals and Non-Goals

### Goals
- Port the MCP server's core analysis pipeline to a local, on-device
  Android app written in Kotlin.
- Preserve logic parity: discovery -> filtering -> enrichment ->
  ranking -> results.
- Allow users to bring their own API keys for data providers.
- Establish AI as a foundational element, using LLM reasoning to
  transform API data into meaningful insights and decisions.
- Provide background alerts for user-defined signals.

### Non-Goals (initial release)
- Running a server or exposing a JSON-RPC API.
- Full real-time streaming data in the first iteration.
- Automated trade execution or brokerage integration.

## 2. High-Level Architecture

### App Layers
- **UI layer (Jetpack Compose):** screens, navigation, state
  presentation.
- **ViewModel layer (MVVM):** orchestrates user actions, triggers
  pipeline, handles loading/error states, exposes StateFlow.
- **Domain layer (Use cases):** pure business logic for each pipeline
  step and indicator calculations.
- **Data layer (Repository + Providers):** API integration, provider
  fallback logic, caching, and storage of API keys.
- **Storage layer:** encrypted preferences for keys and settings;
  optional local database for results and watchlists.

### Core Flow
1. User selects intent (day trade, swing, long-term).
2. ViewModel invokes pipeline.
3. Repository fetches data (quotes, bars, fundamentals, sentiment).
4. Domain logic computes indicators, filters, ranks.
5. UI displays results, prioritizing the AI-synthesized summary over raw data.
6. Background worker runs periodic checks for alerts.

## 3. Data Model Mapping

Define Kotlin data classes that mirror the MCP server types.
Recommended models (minimum set):

- `Quote`
  - `symbol: String`
  - `price: Double`
  - `volume: Long`
  - `timestamp: Instant`

- `IntradayBar`
  - `time: Instant`
  - `open: Double`
  - `high: Double`
  - `low: Double`
  - `close: Double`
  - `volume: Long`

- `DailyBar`
  - `date: LocalDate`
  - `open: Double`
  - `high: Double`
  - `low: Double`
  - `close: Double`
  - `volume: Long`

- `CompanyProfile`
  - `name: String`
  - `sector: String?`
  - `industry: String?`
  - `description: String?`

- `FinancialMetrics`
  - `marketCap: Long?`
  - `peRatio: Double?`
  - `eps: Double?`

- `SentimentData`
  - `score: Double?`
  - `label: String?`

- `IntradayStats`
  - `vwap: Double?`
  - `rsi14: Double?`
  - `atr14: Double?`

- `EodStats`
  - `sma50: Double?`
  - `sma200: Double?`

- `TradeSetup`
  - `symbol: String`
  - `setupType: String` ("High Probability" or "Speculative")
  - `triggerPrice: Double`
  - `stopLoss: Double`
  - `targetPrice: Double`
  - `confidence: Double`
  - `reasons: List<String>`
  - `validUntil: Instant`
  - `intent: TradingIntent`

## 4. Provider Integration

### Provider Selection
Mimic the MCP server's provider fallback:
1. Build a list of available providers based on which API keys are set.
2. Try providers in priority order (e.g., Alpaca, Polygon, Finnhub,
   Financial Modeling Prep).
3. If a provider fails, continue to the next.
4. If all fail, return mock or cached data where available.

### Provider Interfaces
Create a common interface for each data type:
- `QuoteProvider`
  - `getQuotes(symbols: List<String>): Map<String, Quote>`
- `IntradayProvider`
  - `getIntraday(symbol: String, days: Int): List<IntradayBar>`
- `DailyProvider`
  - `getDaily(symbol: String, days: Int): List<DailyBar>`
- `ProfileProvider`
  - `getProfile(symbol: String): CompanyProfile?`
- `MetricsProvider`
  - `getMetrics(symbol: String): FinancialMetrics?`
- `SentimentProvider`
  - `getSentiment(symbol: String): SentimentData?`

Implement each provider with Retrofit/OkHttp, mapping responses to the
data models.

### Caching
Implement a short-lived in-memory cache to avoid redundant requests:
- Quotes: cache for a few seconds.
- Intraday data: cache for 1-5 minutes.
- Profiles/metrics: cache for a day.

Use a simple map keyed by request signature with timestamp-based
expiration. Keep the cache inside the repository to avoid UI coupling.

## 5. Pipeline Steps (Domain Logic)

Each step should be a pure function or use case class to keep logic
testable and composable.

### 5.1 Candidate Discovery
`discoverCandidates(intent: TradingIntent): List<String>`
- Start with a static list if no screener is available.
- Allow future replacement with a smart screener.
- Default set can be a curated list of high-liquidity symbols.

### 5.2 Tradeability Filter
`filterTradeable(symbols: List<String>): List<String>`
- Fetch quotes for symbols.
- Keep symbols where:
  - `price >= 1.0`
  - `volume > 0`
- Return filtered list.

### 5.3 Intraday Enrichment
`enrichIntraday(symbols: List<String>): Map<String, IntradayStats>`
- Fetch recent intraday bars (1-2 days).
- Compute:
  - VWAP
  - RSI 14
  - ATR 14
- Store computed stats per symbol.

#### Indicator formulas
- **VWAP:** sum(price * volume) / sum(volume)
- **RSI 14:** standard 14-period RSI on close prices
- **ATR 14:** average of true range over 14 periods

### 5.4 Context Enrichment
`enrichContext(symbols: List<String>): ContextData`
- Fetch:
  - Profile
  - Financial metrics
  - Sentiment
- Combine into a single structure per symbol.

### 5.5 End-of-Day Enrichment
`enrichEod(symbols: List<String>): Map<String, EodStats>`
- Fetch daily bars (e.g., 200 days).
- Compute:
  - SMA 50
  - SMA 200

### 5.6 Ranking and Scoring
`rankSetups(symbols, intradayStats, eodStats, sentiment, quotes)`
- Score based on heuristic rules (parity with MCP server):
  - Price above VWAP: +1
  - RSI < 30: +1
  - RSI > 70: -0.5
  - Price above SMA200: +1
  - Sentiment score > 0.2: +1
- Confidence = clamp(score / maxScore, min 0.1, max 1.0)
- Setup type:
  - Score > 2.0 -> High Probability
  - Otherwise Speculative
- Suggested levels:
  - Trigger: current price
  - Stop loss: price * 0.98
  - Target: price * 1.05
- Validity:
  - 30 minutes for day trade
  - Longer for swing/long-term (optional, configurable)

Return setups sorted by confidence descending.

## 6. App Orchestration

### ViewModel
Implement an `AnalysisViewModel` that:
- Validates API keys before running analysis.
- Exposes `StateFlow` for loading, error, results.
- Launches the pipeline in `viewModelScope`.

Suggested `StateFlow` shape:
- `isLoading: Boolean`
- `errorMessage: String?`
- `results: List<TradeSetup>`
- `lastRunAt: Instant?`

### Use Case Composition
Implement a `RunAnalysisUseCase` to sequence steps:
1. `discoverCandidates`
2. `filterTradeable`
3. `enrichIntraday`
4. `enrichContext`
5. `enrichEod` (if intent is swing or long-term)
6. `rankSetups`

Return a single `AnalysisResult` object with counts and setups.

## 7. UI Implementation Notes

### Screens
- **Setup / API Keys**
  - Form for provider keys and LLM key.
  - Save keys to encrypted storage.
  - Validate and surface missing keys.

- **Main Analysis**
  - Intent picker (chips or segmented control).
  - Run button.
  - Loading indicator.
  - Summary of results.

- **Results List**
  - Cards with symbol, confidence, label.
  - Primary content: Short AI summary snippet.
  - Color or icon mapping for confidence and type.

- **Setup Details**
  - **Primary View:** AI-synthesized analysis, reasoning, and risk assessment.
  - **Raw Data View (Toggle):** Full metrics, indicators, sentiment, score components.

- **Alerts**
  - Toggle alerts on/off.
  - Choose conditions and thresholds.
  - Select symbols to monitor.

- **Settings**
  - Manage keys.
  - Configure refresh interval.
  - Clear cached data.

### Navigation
Use Compose Navigation. Model each screen as a destination; pass
`symbol` or `setupId` for detail navigation.

## 8. AI Foundation & Reasoning

### Scope

AI is a core component. The app is designed to reason on data before
presentation. While the app can function in a "raw mode" without an LLM key,
the intended and default experience relies on the LLM to synthesize
complex signals into actionable intelligence.

### Integration Strategy
- Use a Retrofit or OkHttp client for the LLM API.
- Store the LLM key in encrypted storage.
- **Synthesis Prompting:** Instead of just "explaining", the prompt should
  ask the model to act as a senior analyst:
  - Input: Full technical set (indicators, price levels), fundamental data,
    and news sentiment.
  - Output: A structured decision summary, risk factors, and a clear "Why"
    narrative.

### Example Prompt
```
Act as a senior trading analyst. Review the following technical setup:
Ticker: XYZ
Setup: High Probability (Score 2.5)
Indicators: Price > VWAP, RSI 28 (Oversold), SMA200 Support.
Context: Tech Sector, Bullish Sentiment.

Provide:
1. A concise synthesis of the opportunity.
2. Key risk factors.
3. A final verdict.
```

### UI Flow
- **Default:** When a user views a setup, the app fetches the AI analysis automatically (if key is present).
- **Presentation:** The AI response is shown as the main content.
- **Interaction:** Users can tap "View Raw Data" to see the underlying numbers.

## 9. Alerts and Background Work

### WorkManager
Use WorkManager to run periodic checks:
- Default schedule: every 15 minutes during market hours.
- Use constraints to avoid running on low battery if needed.

### Alert Logic
Define conditions such as:
- Price dips below VWAP by a percentage.
- RSI crosses below 30 or above 70.
- Price hits target or stop loss.

### Worker Flow
1. Load the watchlist or last analysis symbols.
2. Fetch fresh quotes or minimal intraday data.
3. Evaluate alert conditions.
4. If triggered, post a notification.

### Notification Channel
Create a dedicated channel: "Market Alerts".
Notifications should deep link into the relevant detail screen.

## 10. Storage and Security

### API Keys
- Use `EncryptedSharedPreferences` or Android Keystore.
- Avoid logging keys.
- Provide a clear "Clear keys" action in Settings.

### Results Persistence
Optional but recommended:
- Store the last analysis results and timestamps.
- Allows detail view without re-running analysis.
- Supports alert comparisons to prior values.

## 11. Error Handling and Resilience

- Provider errors: fallback to next provider.
- Network timeouts: retry with exponential backoff for critical calls.
- Partial data: allow ranking even if some context data is missing.
- UI: show precise error states (missing keys, rate limits, outages).

## 12. Testing Plan

### Unit Tests
- Indicator calculations (RSI, ATR, VWAP, SMA).
- Ranking logic with synthetic inputs.
- Tradeability filter edge cases (price == 1, volume == 0).

### Integration Tests
- Repository calling a mocked provider and fallback logic.
- End-to-end pipeline for a small symbol set.

### UI Tests
- ViewModel state transitions and UI state binding.
- Error handling: missing keys, provider failure.

## 13. Implementation Checklist

- Data classes and enums defined.
- Provider interfaces and Retrofit clients built.
- Repository with fallback and caching.
- Indicator calculations ported to Kotlin.
- Use cases for each pipeline step.
- ViewModel orchestration and state flow.
- Compose UI screens and navigation.
- LLM integration (optional).
- WorkManager alerts.
- Tests for indicators and ranking.

## 14. Mapping to MCP Server Components

This table clarifies how MCP server pieces map to Android components.

- **Router + provider fallback** -> Repository with provider ordering.
- **Tools (discover/filter/enrich/rank)** -> Use cases / domain functions.
- **/mcp JSON-RPC API** -> direct ViewModel method calls.
- **In-memory cache** -> repository cache maps with TTL.
- **Auth + tokens** -> local key storage only.

## 15. Future Enhancements

- Replace static discovery with a screener.
- Add MACD and Bollinger Bands indicators.
- Improve ranking with fundamental weighting.
- Add chart visualizations in detail screen.
- Optional watchlist sync across devices.
