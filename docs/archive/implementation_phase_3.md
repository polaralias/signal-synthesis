# Next Steps for Improving the Trading Companion App

## Introducing Graphs for Signals

To enhance the individual signal view, the app should display price
charts alongside the text analysis. Currently, the app already fetches
historical intraday and daily data for each symbol (e.g. 5-minute bars
for 2
days)[\[1\]](app/src/main/java/com/polaralias/signalsynthesis/data/provider/fmp/FmpMarketDataProvider.kt#L46-L54)[\[2\]](app/src/main/java/com/polaralias/signalsynthesis/data/provider/polygon/PolygonMarketDataProvider.kt#L44-L53).
We can leverage this data to render charts client-side using Jetpack
Compose. One approach is to use a Compose Canvas or a charting library
(such as MPAndroidChart or a Compose-based chart lib) to plot the
**intraday price series** (or daily series for longer-term signals).
This would allow users to visually see trends and patterns that
correspond to the AI's analysis. Since the data is already available via
the `MarketDataRepository.getIntraday()` and `getDaily()` calls, we can
simply pass those data points into a composable chart when the user
opens a signal detail. By avoiding AI image generation and using actual
price data, the charts will be accurate and up-to-date.

In implementation, we might create a `ChartView` composable that takes a
list of `IntradayBar` or `DailyBar` points and renders a line chart or
candlesticks. The chart can be interactive (scroll/zoom) or static. The
key is to integrate it into the **signal detail screen** below the AI
summary. This aligns with our goal of richer visualization in the detail
view. Notably, the data for these charts is already being fetched in the
pipeline (the EnrichIntraday/EOD steps), so we just need to cache or
pass it to the UI. If needed, we could store the fetched time-series in
the `AnalysisResult` or retrieve on-demand when the detail opens. Given
our Compose UI, a real-time chart can be drawn without much overhead.

## Enhanced Data in the Signal Detail View

Currently, when viewing a trade setup's details, the UI mainly shows the
AI's synthesized summary and basic levels (trigger, stop,
target)[\[3\]](app/src/main/java/com/polaralias/signalsynthesis/ui/ResultsScreen.kt#L121-L129).
We should expand this to include the underlying **raw data and
indicators** that informed the decision. This means displaying things
like the calculated RSI, ATR, VWAP values, any fundamental metrics, and
the reasons for the confidence score. The code already computes a list
of `reasons` for each setup (e.g. *"Price above VWAP (X)"*, *"RSI
oversold (28)"*) as part of the ranking
step[\[4\]](app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RankSetupsUseCase.kt#L82-L91)[\[5\]](app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RankSetupsUseCase.kt#L94-L102).
These reasons (and the numeric values) can be shown in a "Technical
Details" section. Additionally, any fundamental data we fetched (company
sector, P/E, EPS) and sentiment score can be listed.

The app's design in the implementation guide anticipated a *"Raw Data
View"* toggle on the detail
screen[\[6\]](docs/implementation/implementation_guide.md#L260-L268).
We should implement this: e.g. a button to switch between the AI Summary
and a Raw Data panel. The Raw Data panel would list: - **Technical
indicators**: RSI-14, ATR-14, VWAP (with actual values). For example,
*"RSI 14: 28 (oversold)"*, *"VWAP: \$123.45"*, etc. - **Fundamentals**:
Market cap, P/E ratio, EPS (if available from the `FinancialMetrics`). -
**Sentiment**: e.g. *"Sentiment: Bullish (score 0.35)"*. - **Validity**:
how long the signal is valid (already in TradeSetup as `validUntil`). -
**Reasons**: The list of reasons from `TradeSetup.reasons` which is
essentially a concise
justification[\[4\]](app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RankSetupsUseCase.kt#L82-L91)[\[5\]](app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RankSetupsUseCase.kt#L94-L102).

Implementing this likely means extending the `SetupDetailScreen`
composable (or equivalent) to display these fields. The data is mostly
already accessible: for instance, in `SynthesizeSetupUseCase` we gather
`profile`, `metrics`, and `sentiment` for the
prompt[\[7\]](app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/SynthesizeSetupUseCase.kt#L17-L25)
-- we can reuse those objects to show fundamentals and sentiment. The
`IntradayStats` (with RSI, ATR, VWAP) is computed in the pipeline and
used for
scoring[\[8\]](app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RankSetupsUseCase.kt#L78-L87)[\[9\]](app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RankSetupsUseCase.kt#L89-L97),
but not stored in `TradeSetup` currently. We might need to attach
`IntradayStats` to each setup or recompute it for the selected symbol
when viewing details. A simple way is to store a map of
symbol→IntradayStats in the `AnalysisResult` so the UI can retrieve it.

Providing this detailed view will make the app more transparent -- users
can verify the numbers behind the AI's recommendation. It complements
the graphs: the chart shows price trends visually, and the data view
shows exact values and indicators.

### Educational explanations for each metric

Every value shown must answer two questions:

1. What does this metric mean?
2. Why does it matter for this trade?

UI structure for each metric:
- Value
- Status (for example Oversold, Neutral, Elevated)
- Info icon or expandable explanation

All explanations must be static and human-written (not AI-generated) so
users see consistent definitions across sessions.

Example (RSI):
- Displayed value: RSI (14): 28 - Oversold
- Explanation: RSI measures the speed and magnitude of recent price
  movements on a 0 to 100 scale. Values below 30 suggest the stock may
  be oversold, meaning selling pressure may be exhausted.
- Relation to decision: An oversold RSI supports a potential bounce if
  confirmed by price action and volume.

Other metrics should follow the same pattern:
- VWAP: Represents the average price traded during the day, weighted by
  volume. Trading above VWAP often signals bullish intraday sentiment.
- ATR: Measures typical price movement. Higher ATR implies greater
  volatility and wider stop requirements.
- Volume: Confirms conviction behind price movement. Low-volume
  breakouts are less reliable.
- Earnings date: Introduces volatility risk due to unpredictable
  fundamental news.

### AI summary alignment with indicators

The AI summary must:
- Name the indicators used.
- State their actual values.
- Explain how those values strengthen or weaken the trade thesis.

Add this instruction to the prompt templates:
"When forming conclusions, explicitly reference the relevant indicators
(for example RSI, VWAP, ATR), state their values, and explain how those
values support or weaken the trade."


## User-Visible Logging of API and LLM Interactions

To help users (and developers) understand the data flow, the app should
expose a log of key operations -- specifically API calls/responses and
LLM queries -- without exposing sensitive info like API keys or internal
system prompts. We can achieve this by leveraging the existing logging
infrastructure. The code uses a `Logger` object that wraps Android's
`Log` and tags messages with our app
name[\[10\]](app/src/main/java/com/polaralias/signalsynthesis/util/Logger.kt#L8-L16).
Throughout the repository, we already log events like provider
success/failure. For example, when trying providers in order, the
repository logs which provider returned valid data or if one
failed[\[11\]](app/src/main/java/com/polaralias/signalsynthesis/data/repository/MarketDataRepository.kt#L122-L130).
Also, the `RetryHelper` logs retries for network
calls[\[12\]](app/src/main/java/com/polaralias/signalsynthesis/data/provider/RetryHelper.kt#L36-L44)[\[13\]](app/src/main/java/com/polaralias/signalsynthesis/data/provider/RetryHelper.kt#L38-L46).
We can collect these logs and display them in-app.

**Implementation approach:** We could maintain an in-memory log buffer
or write logs to a file that the app can read. Each time `Logger`
records something (via `Log.i`, `Log.w`, etc.), we append it to a list.
We then create a **Log Viewer screen** (perhaps accessible from a
Developer Settings or via a gesture) where the user can scroll through
recent logs. Key events to include: - **Provider selection and
results:** e.g. "*AlpacaMarketDataProvider returned valid results*" or
"*FinnhubMarketDataProvider returned
empty*"[\[11\]](app/src/main/java/com/polaralias/signalsynthesis/data/repository/MarketDataRepository.kt#L122-L130),
so users see which data source was used for quotes, etc. - **LLM
calls:** We should add new log lines before and after calling the LLM.
For example: log the prompt being sent (we can omit or mask the system
role text). We might log something like "*LLM request for XYZ: prompt
with indicators and context sent to the selected model*". After receiving, log "*LLM
response for XYZ received successfully*" or any error if occurs. This
requires adding a couple of `Logger.d` calls in
`SynthesizeSetupUseCase.execute` (around the call to
`llmClient.generate`[\[14\]](app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/SynthesizeSetupUseCase.kt#L22-L25)).
We will **not** include the API key or any secret in these logs. The
system prompt ("You are a senior trading analyst...") can be considered
an internal implementation detail -- we can choose not to display that
exact line to the user to avoid confusion. Instead, we focus on the
content of the user prompt (which includes the setup details and
instruction to output JSON).

We must ensure no credentials are logged. The current code avoids
logging API keys (e.g., `ApiKeyStore.saveKeys` trims and doesn't log the
values, and our Logger doesn't print
them)[\[15\]](app/src/main/java/com/polaralias/signalsynthesis/data/storage/ApiKeyStore.kt#L44-L53)[\[16\]](app/src/main/java/com/polaralias/signalsynthesis/data/storage/ApiKeyStore.kt#L58-L66).
So we're safe as long as we continue that practice.

By implementing this logging screen, advanced users can debug issues
(like why data might be missing -- e.g., a certain provider failed or
returned no data) and see how the AI arrived at outputs. This
transparency can build trust. We'll likely include a toggle to
enable/disable verbose logging in settings (to avoid performance impact
or clutter for casual users).

## Incorporating Earnings Dates and Expanded Data in Analysis

For longer-term analyses (swing trades, long-term holds), including
upcoming **earnings dates** can improve decision quality. Currently, we
don't fetch earnings dates, but our data providers do offer this info.
For instance, the Financial Modeling Prep API returns an
`earningsAnnouncement` date as part of the quote
data[\[17\]](app/src/main/java/com/polaralias/signalsynthesis/data/provider/fmp/FmpService.kt#L82-L88).
We should start pulling this in the context enrichment phase.

**How to implement:** In the `enrichContext` step (or a new step), call
an API to get the next earnings date for each symbol. FMP's `quote`
already provides the next earnings announcement if
upcoming[\[17\]](app/src/main/java/com/polaralias/signalsynthesis/data/provider/fmp/FmpService.kt#L82-L88),
so we can parse that. Alternatively, Finnhub has an earnings calendar
endpoint (not currently implemented in our code, but available via
Finnhub API). We can add an `EarningsProvider` interface or simply
extend `ProfileProvider`/`MetricsProvider` to also fetch earnings info.
E.g., using FMP: after getting the profile, also capture
`earningsAnnouncement` from the `FmpQuote`.

Once retrieved, this earnings date can be fed into the LLM prompt and
displayed to the user: - **In AI prompt:** Append a line in the context
like `“Next earnings date: 2024-11-05”` if the date is near (within the
next quarter). The prompt to the LLM might then mention something like
upcoming earnings as part of "Context". This way the AI can consider it
-- e.g., it might warn *"Earnings report is due soon, adding
uncertainty."* Including this in the prompt ensures the AI's summary is
more comprehensive. - **Citation of results:** To maintain credibility,
we can have the AI include a note of the source or we can display an
attribution in the UI. For example, if the AI says "Company XYZ reports
earnings next week," we can show a footnote like "(Source: FMP)". Since
our UI is not text-based like a document, a simple approach is to have a
label in the detail view: *"Earnings Date from FMP: Nov 5, 2024"*. This
makes it clear where that info came from.

Additionally, **expanded data sources** might include things like
dividend dates, analyst ratings, etc., if available. But earnings is a
high-impact piece of data for longer-term trades, so that's a priority.
Technically, adding this means updating our data model (e.g., extend
`CompanyProfile` or create a new `UpcomingEarnings` model). However,
since FMP's quote already has it, we can quick integrate: when we fetch
quotes (in `MarketDataRepository.getQuotes`), we could store the
earnings date in a map for later use. A cleaner approach: extend
`CompanyProfile` to have an `earningsDate` field and fill it in
`getProfile` if available. In FMP's `getProfile` call, we could
cross-reference the quote data.

Finally, ensure the AI summary references this if relevant. The prompt
might be adjusted to explicitly ask the AI to consider earnings if
present. For example: *"Context: ... Earnings: Nov 5, 2024 (next
week)"*. The LLM will likely mention it in risks or summary. The result
is a more **holistic synthesis** of technical and fundamental timing
factors, which is valuable for swing/long-term intents.

## Clarifying Signal Intent in Watchlist/Dashboard

The watchlist or results dashboard currently lists the signals (trade
setups) with their symbol, confidence, and type but does **not indicate
the trading intent** (whether it came from a Day Trade scan, Swing, or
Long-Term)[\[18\]](app/src/main/java/com/polaralias/signalsynthesis/ui/ResultsScreen.kt#L113-L121).
This can be confusing if a user runs multiple analyses with different
intents -- e.g., they might have AAPL from a day-trade scan and AAPL
from a long-term scan, which have different implications. We should
surface the intent context in the UI.

**Solution:** We can simply display the `TradeSetup.intent` field
alongside each item. For example, on each result card, add a small label
or color code for intent: - Text label like "(Day Trade)" or "(Swing)"
under the symbol name. - Or use an icon or initial (e.g., "D", "S", "L"
badges) with a legend. For clarity, text is fine: *"AAPL -- Day Trade"*
vs *"AAPL -- Long Term"*. In code, this is easy since `TradeSetup`
already carries the
`intent`[\[19\]](app/src/main/java/com/polaralias/signalsynthesis/domain/model/TradeSetup.kt#L11-L15).

In the `SetupCard` composable, we can append `setup.intent.name` to the
title or subtitle. For example: change line `Text(setup.symbol, …)` to
include intent: `Text("${setup.symbol} (${setup.intent})", …)` or add
another Text below symbol. This way, as soon as the results appear,
users know which strategy each setup is for.

For the **watchlist** (if implemented as saving favorites), we should
also store the intent. If the watchlist DB table doesn't currently have
an intent column, we might add it, or at least when displaying,
cross-reference the last analysis result for that symbol's intent. A
more robust approach: include `intent` in the saved watchlist entries at
creation time.

By doing this, the dashboard will clearly show which *"mode"* generated
each signal. This addresses the ambiguity and ensures users don't mix
up, say, a high-confidence day trade with a high-confidence long-term
trade -- those are very different contexts.

## LLM Model Selection and Control (OpenAI + Gemini Only)

Scope for phase 3 is strictly OpenAI and Google Gemini. Do not expose
temperature, top-p, or other sampling controls in the UI. The app
normalizes configuration into three user-facing controls only:

1. Reasoning depth
2. Output length (token cap)
3. Verbosity (OpenAI only)

### Supported providers and models

OpenAI (Responses API, GPT-5 family):
- GPT-5.2 (supports `xhigh`)
- GPT-5.1 (default balance)
- GPT-5 Mini (cost efficient)
- GPT-5 Nano (ultra fast)
- GPT-5.2 Pro (optional premium tier)

Google Gemini:
- Gemini 2.5 Flash
- Gemini 2.5 Pro
- Gemini 3 Flash
- Gemini 3 Pro

### OpenAI control mapping

Reasoning effort (`reasoning.effort`):
- `none`, `low`, `medium`, `high`
- `xhigh` only on GPT-5.2 and higher-tier variants
- default to `medium`

Verbosity (`text.verbosity`):
- `low`, `medium`, `high`

Output length (`max_output_tokens`):
- numeric cap, exposed as tiers or slider

### Gemini control mapping

Reasoning controls:
- Gemini 3 series uses `thinking_level` (`low`, `medium`, `high`)
- Gemini 2.5 series uses `thinkingBudget` (thinking token budget)

Verbosity:
- no native control, use prompt guidance only
- disable the verbosity control in the UI for Gemini

Output length:
- expose a token cap equivalent to OpenAI

### Provider-aware UI mapping

| Concept         | OpenAI                                   | Gemini                                           |
| -------------- | ---------------------------------------- | ------------------------------------------------ |
| Reasoning depth| `reasoning.effort`                        | Gemini 3 `thinking_level`, Gemini 2.5 `thinkingBudget` |
| Output verbosity| `text.verbosity`                         | Prompt-guided only                               |
| Output length  | `max_output_tokens`                       | Output token cap                                 |

Suggested reasoning tiers:
- Fast: OpenAI `low`, Gemini `low`
- Balanced: OpenAI `medium`, Gemini `medium`
- Deep: OpenAI `high`, Gemini `high`
- Extra: OpenAI `xhigh` only (hidden otherwise)

Suggested verbosity tiers (OpenAI only):
- Low
- Medium (default)
- High

Suggested output size tiers:
- Short: ~300 to 500 tokens
- Standard: ~700 to 900 tokens
- Full: 1000+

### Tiered model strategy

| Tier              | Provider        | Suggested model             | Best for                      |
| ----------------- | --------------- | --------------------------- | ----------------------------- |
| High-accuracy     | OpenAI          | GPT-5.2                     | Deep analysis, long signals   |
| Balanced          | Gemini          | Gemini 2.5 Flash            | Fast, reliable synthesis      |
| Cost-efficient    | OpenAI          | GPT-5 Mini                  | Frequent signals              |
| Ultra-fast        | OpenAI / Gemini | GPT-5 Nano / Gemini 3 Flash | Bulk processing               |
| Premium reasoning | Gemini          | Gemini 3 Pro                | Complex reasoning with Gemini |

### UI enforcement rules

- Show only reasoning levels supported by the selected model.
- Disable `xhigh` unless GPT-5.2 (or GPT-5.2 Pro) is selected.
- Disable verbosity for Gemini models with a tooltip explaining why.
- Always expose output length.
- Validate the selection at selection time, not request time.

### Implementation notes

- Extend `LlmClient` with an OpenAI and Gemini implementation only.
- Add a provider-aware configuration mapper that converts the three UI
  controls into provider-specific parameters.
- Store API keys per provider (OpenAI and Gemini) rather than a single
  global `llmKey`.
- Settings UI should expose Provider and Model, then show only valid
  controls for that model.

## Adjusting Signal Discovery by User Risk Tolerance

The current candidate discovery method returns largely the same
well-known large-cap symbols (Apple, Microsoft, Visa, etc.), which you
observed in testing. This is because we seed the analysis with a
**static list of high-liquidity stocks** for each
intent[\[27\]](app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/DiscoverCandidatesUseCase.kt#L32-L40)[\[28\]](app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/DiscoverCandidatesUseCase.kt#L48-L56).
There is no differentiation for risk profile -- in fact, we currently
filter out low-priced "penny" stocks entirely for all users (any stock
under \$1 is
excluded)[\[29\]](app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/FilterTradeableUseCase.kt#L26-L34).
To address this, we should incorporate user risk tolerance into the
discovery and filtering steps, so that higher-risk settings yield more
speculative, small-cap picks.

**Proposed changes:** 1. **Risk tolerance setting:** Introduce a user
setting (perhaps in the Settings screen or as an "Advanced" option when
choosing intent) for risk level -- e.g. *Conservative, Moderate,
Aggressive*. This can default to Moderate. A high risk tolerance
corresponds to willingness to consider penny stocks or volatile small
caps.

1.  **Discovery logic:** If risk is high (Aggressive), we can expand the
    candidate list to include smaller stocks. This could be done by:
2.  **Using an API-based screener:** Ideally, call a provider's screener
    to fetch candidates that meet certain criteria (e.g., price \< \$5
    but above some pennies, decent volume, high volatility). For
    example, Finnhub has an API for stock symbols and could filter by
    market cap or sector, and Financial Modeling Prep offers screener
    endpoints. This would move us closer to dynamic discovery rather
    than the hardcoded
    list[\[30\]](docs/implementation/product_vision.md#L31-L39).
3.  **Or extend our static lists:** We could maintain an additional list
    of "speculative symbols" and append them when risk is high. For
    instance, add some known mid-cap or popular retail trader stocks
    (meme stocks, biotech startups, etc.). However, a static list of
    penny stocks is hard to maintain and could be risky (they come and
    go).

A compromise: for **Aggressive** mode, perhaps include the day-trade
list plus a few ETFs or indices that track smaller stocks (like *IWM*
for small-cap Russell 2000) and then a handful of actual penny stocks
that are frequently traded. We might also drop the market cap criterion
altogether in this mode.

1.  **Tradeability filter adjustment:** Currently, we enforce
    `price >= $1` for
    all[\[29\]](app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/FilterTradeableUseCase.kt#L26-L34).
    If the user is high-risk, we can lower this threshold. For example,
    allow down to \$0.10 or even include anything \> \$0 (which
    basically means don't filter out penny stocks at all, except maybe
    truly illiquid ones). We should still ensure volume \> 0, of course.
    We can easily pass a parameter to the
    `FilterTradeableUseCase.execute()` to use a different `minPrice` for
    high-risk users. The function signature already has a `minPrice`
    parameter (default
    1.0)[\[31\]](app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/FilterTradeableUseCase.kt#L16-L24),
    so we can call `filterTradeable.execute(symbols, minPrice = 0.1)`
    for aggressive mode. This will keep sub-\$1 stocks in the running.

2.  **Other criteria:** We might also consider using volatility or
    volume as a factor. High-risk traders might want stocks that move a
    lot (higher ATR or daily % changes). If available, we could
    incorporate that. For now, just not excluding them is a big step.

With these changes, a high-risk user's run might surface some low-priced
stocks (e.g., instead of only AAPL, MSFT, they might see stocks like
**XYZ** priced at \$0.75 if it had unusual volume, etc.). Conversely,
for a conservative user, we might *tighten* the list: perhaps only
blue-chip, low-volatility stocks (we could filter out anything with too
small market cap or too high beta). We can implement conservative mode
by doing the opposite -- e.g., skip any stock under maybe \$10 or with
volume below a threshold, and perhaps remove highly volatile tickers
from the initial list.

In summary, introducing risk-based branching in candidate discovery will
make the suggestions more personalized: - **Conservative:** fewer,
larger names (we could even prioritize dividend stocks or ETFs). -
**Aggressive:** include penny stocks and highly volatile picks. Use
provider screener if possible to get current hot small caps.

These adjustments should be documented to the user (perhaps a note in
the UI when selecting risk level: "Aggressive mode may include
low-priced, speculative stocks"). Also, we should monitor performance --
small stocks might have less reliable data from providers, so ensure our
provider fallback can handle ones not traded on major exchanges if any.

## Reintroducing Twelve Data as a Data Provider

Your original vision included Twelve Data as one of the sources, but
currently the app has support only for Alpaca, Polygon, Finnhub, and
FMP[\[32\]](app/src/main/java/com/polaralias/signalsynthesis/data/provider/ApiKeys.kt#L4-L8).
There is no integration of Twelve Data in the code (no API key slot or
provider class for
it)[\[33\]](app/src/main/java/com/polaralias/signalsynthesis/data/provider/ApiKeys.kt#L10-L17).
It appears it was left out during implementation, possibly to reduce
complexity or because the other four providers sufficiently covered the
needed functionality. Each provider we have serves a role (real-time
quotes from Alpaca/Polygon, fundamentals from FMP, etc.), so Twelve Data
may have been seen as overlapping. Additionally, using fewer providers
simplifies testing and reduces the number of API keys the user must
manage.

That said, adding Twelve Data could still be beneficial: - **Redundancy
and fallback:** More providers mean if one fails or has missing data,
another can fill in. Twelve Data could act as an extra fallback in our
`ProviderFactory`. For example, Twelve Data offers real-time prices and
technical indicators; it might also cover some symbols that others miss
or provide another source for validation. - **Global markets or data
coverage:** If we ever expand to non-US markets or need data like
forex/crypto (Twelve Data supports multi-asset), it could help. - **Rate
limits:** Distributing calls across providers can avoid hitting
individual API limits. If, say, Polygon has tight rate limits on free
tier, Twelve Data could share the load for quote retrieval.

**Checking for reasons in documentation:** There's no explicit note in
the implementation docs about removing Twelve Data. It might simply be
time constraints or the fact that the MCP server (which the app is based
on) primarily used the four providers we kept. The MCP's design
mentioned multiple providers including those we
used[\[34\]](docs/implementation/product_vision.md#L5-L12)
(Alpaca, Polygon, Finnhub, FMP) but not Twelve Data explicitly. Perhaps
Twelve Data was considered but not implemented on the server either, or
it was an optional part of the plan that was deferred.

**Recommendation:** If license/cost is not an issue, we should consider
adding Twelve Data support to "better synthesize decisions." This could
mean: - Integrating Twelve Data's API for price quotes, maybe technical
indicator endpoints. For instance, Twelve Data can directly provide
indicator values (RSI, etc.) via API, which could complement our
internal calculations or serve as a cross-check. - Using Twelve Data's
fundamental or earnings data if available. If it offers something unique
(for example, some APIs give analyst ratings or broader financials),
that could enrich the context.

To add it, we'd follow the pattern of the other providers: - Create a
`TwelveDataService` interface with Retrofit for the endpoints we need. -
Create a `TwelveDataMarketDataProvider` implementing our provider
interfaces (`QuoteProvider`, `IntradayProvider`, etc.) similar to
`PolygonMarketDataProvider` or others. - Add an entry for the Twelve
Data API key in `ApiKeys` and the UI (so user can input their Twelve
Data API key). - Update `ProviderFactory` to include TwelveData in the
priority lists. We'd decide its priority order. For example, for
real-time quotes, Twelve Data might be on par with Polygon (both can
provide live prices). We might put it after Polygon but before Finnhub,
or whichever makes sense based on reliability.

If we find no strong need for its data now, it could be an optional
enhancement for later. But given it was in the vision, including it
would align the product with initial plans. At minimum, leaving a slot
for its API key and perhaps not using it heavily yet is also an option
(future-proofing).

In summary, there's no inherent code issue preventing Twelve Data's use
-- it was likely omitted intentionally. We can safely add it to broaden
our data source coverage, which may improve result quality (especially
if one provider lacks a data point, Twelve Data might have it).

## Centralizing AI Prompt Definitions

Currently, the prompts fed to the LLM are defined in-line at various
points in the code. For example, the prompt for synthesizing a trade
setup is constructed with a hardcoded template string inside
`SynthesizeSetupUseCase.buildPrompt()`[\[35\]](app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/SynthesizeSetupUseCase.kt#L52-L60).
Also, the system role message ("You are a senior trading analyst...") is
embedded in `OpenAiLlmClient`
code[\[36\]](app/src/main/java/com/polaralias/signalsynthesis/data/ai/OpenAiLlmClient.kt#L13-L21).
This scattering of prompt text makes it hard to audit or tweak the AI's
behavior.

**Plan to centralize prompts:** We should externalize these prompt
templates into a single location (or a small number of clearly defined
places). A good approach is to create a **Prompt Library** -- e.g., a
Kotlin object or set of constants that store the base prompt strings.
For instance, we might have:

    object Prompts {
        const val SYSTEM_ANALYST = "You are a senior trading analyst. Respond with JSON only."
        const val SETUP_SYNTHESIS_TEMPLATE = """
            Act as a senior trading analyst... 
            Output schema: { "summary": ..., "risks": ..., "verdict": ... }
            Ticker: {symbol}
            Setup: {setupType}
            ...etc...
        """.trimIndent()
    }

Then in `SynthesizeSetupUseCase`, instead of embedding the long string,
we use `Prompts.SETUP_SYNTHESIS_TEMPLATE` and do a `.replace` or use a
templating mechanism to insert the actual values. We could use simple
`String.format` or Kotlin's string interpolation by constructing the
string in one place. Another idea is to keep these in a resource file
(like `strings.xml` or a JSON asset) so they can even be modified
without recompiling (though not easily by end-user, but by us). A single
JSON/YAML config for prompts could list each prompt template with a key.

By centralizing, you (as the developer) can review all AI prompts in one
file. This makes auditing for tone, correctness, or biases easier. Also,
if we want to adjust the style (say, change how the summary is phrased
or add a new field to the output schema), we edit the template in one
spot and it applies everywhere used.

Additionally, having prompts in one place will help ensure consistency.
If we ever add more AI features (e.g., an AI that gives general trading
tips or a different prompt for candidate discovery), we can keep all
these definitions together.

Concretely, steps to do: - Create a new file, e.g. `AiPrompts.kt` in the
project, with constants or functions that yield the prompt strings. -
Replace the literal strings in `OpenAiLlmClient` and
`SynthesizeSetupUseCase` with references to those constants. For
example, the default system prompt string
at[\[37\]](app/src/main/java/com/polaralias/signalsynthesis/data/ai/OpenAiLlmClient.kt#L14-L19)
would become something like:

    content = systemPrompt ?: Prompts.SYSTEM_ANALYST

And the multi-line prompt in `buildPrompt` becomes something that
perhaps uses a template string from `Prompts`. We might still assemble
parts (like the list of context lines) in code, but the overall
structure ("Act as a senior trading analyst. Review the following
setup... Provide: 1. ... 2. ...") lives in one place. - Ensure any other
AI usages (for instance, if in the future we have a prompt for
discovering candidates or for explaining watchlist alerts) also draw
from this central file.

No visual diagram is needed for this; it's purely a code organization
improvement. But we should document this change so future contributors
know to edit the prompts in the one file rather than sprinkling changes
throughout. This "single source of truth" for prompts will make your
audits and updates straightforward -- you can open one file and see
exactly what we're asking the AI to do in all cases.