# RSS Feed Curation + Ticker-Specific Plan

## Goals
- Replace user-entered RSS URLs with a curated, versioned feed catalog based on financial_resource.
- Add ticker-specific RSS feeds for (a) final-stage symbols and (b) user-entered tickers.
- Centralize feed selection logic so both the staged pipeline and Deep Dive use the same rules.

## Current State (repo summary)
- `AnalysisViewModel` uses a hardcoded `DEFAULT_RSS_FEEDS` list (Yahoo/MarketWatch/Benzinga).
- Users can add custom feeds in Settings; URLs are stored in `AppSettings.rssFeeds` and validated via `VerifyRssFeedUseCase`.
- `RunAnalysisV2UseCase` Stage 8 builds an RSS digest from `rssFeeds` passed in.
- `requestDeepDive()` also builds an RSS digest using `DEFAULT_RSS_FEEDS` only.
- There is no curated RSS catalog yet; feed URLs are either hardcoded defaults or user-entered.

## Proposed Approach
### 1) Introduce a curated RSS catalog
- Convert `financial_resource-master/financial_resource/rss.db` into a static catalog (JSON or Kotlin object).
- Store entries as:
  - `source_id`, `source_label`
  - `topic_id`, `topic_label`
  - `url`
  - `is_ticker_template` and `ticker_template` (when topic == "ticker")
- Keep the catalog in `app/src/main/assets/rss_feeds.json` (or `app/src/main/java/.../RssFeedCatalog.kt`).

### 2) Define feed selection policy
Create a small policy layer to decide which feeds are active at runtime, with strict duplicate-omission to keep fetches/latency down.

**Recommended data model**
- `RssFeedCatalog` (static catalog)
- `RssFeedSelection` (settings-driven selection)
- `RssFeedPolicy` or `RssFeedResolver` (builds feed URLs)

**Selection behavior**
- **Core feeds**: always on (global list of curated feeds).
- **Expanded feeds**: applied per ticker only when the LLM flags them as needed.
- **Per-provider topic selection**:
  - Defaults are pre-selected in Settings (core topics + expanded topics).
  - Users can browse/search provider topics (from the catalog) and toggle them over time.
- **Ticker-specific feeds**:
  - Always include for user-entered tickers (`TickerSource.CUSTOM`), ticker-only.
  - Include for final-stage symbols only when `rssNeeded == true`.
- **Strict duplicate-omission**:
  - De-dup URLs before fetch.
  - De-dup across feeds by GUID/hash at ingest (already in `BuildRssDigestUseCase`).
  - Optional: If two feeds map to the same canonical domain/topic, keep only the primary.
- **Limits**:
  - Cap ticker feeds per symbol (e.g., 2–3 sources).
  - Cap expanded feeds per ticker.

### 3) Update pipeline integration
- Add a reusable `RssFeedResolver` invoked by:
  - `RunAnalysisV2UseCase` Stage 8 (RSS digest)
  - `AnalysisViewModel.requestDeepDive()`
- Keep `BuildRssDigestUseCase` as-is; it should receive a resolved feed list.

### 4) Update settings + UI
Replace user-entered URLs with curated source/topic toggles.
- **AppSettings**: replace `rssFeeds: List<String>` with:
  - `rssEnabledTopics: Set<String>` (topics keyed by `source_id:topic_id`)
  - `rssTickerSources: Set<String>` (e.g., Yahoo, SeekingAlpha, Nasdaq)
  - `rssUseTickerFeedsForFinalStage: Boolean`
- Defaults should be set in settings (core topics enabled; expanded topics enabled but gated per ticker).
- **SettingsScreen**:
  - Remove “Add feed URL” field.
  - Show curated providers with expandable topic lists (search/filter) + an “Enable ticker feeds” section.
  - Include “Reset to defaults” for RSS topics.

### 5) Ticker-specific sources (from financial_resource)
financial_resource marks the following ticker templates:
- `Nasdaq`: `https://www.nasdaq.com/feed/rssoutbound?symbol={}`
- `Seeking Alpha`: `https://seekingalpha.com/api/sa/combined/{}.xml`
- `Yahoo Finance`: `https://feeds.finance.yahoo.com/rss/2.0/headline?s={}&region=US&lang=en-US`
- `Reddit`: `https://www.reddit.com/r/{}/.rss`

Default ticker-feed list recommendation: Yahoo + SeekingAlpha (optionally Nasdaq). Reddit is noisy and can be opt-in.

## Suggested Curated Feed Defaults (core + expanded)
Use only catalog feeds (no ad-hoc URLs). Core runs always; expanded is per ticker.

**Core**
- Reuters (top news)
- CNBC (top news, business, earnings)
- MarketWatch (top stories)
- Yahoo Finance (top news)

**Expanded**
- Seeking Alpha (top stories)
- Nasdaq (market news)
- WSJ (markets / business)
- FT (markets / companies)
- Fortune (markets / business)
- Zacks (top stories / research)
- CNNMoney (business)

## Expanded-needed signal (LLM-driven)
To support expanded feeds on a per-ticker basis:
- Add a **new structured output** field from the Decision Update stage:
  - `expandedRssNeeded: boolean` (default false)
  - `expandedRssReason: string` (optional)
- Update the Decision Update prompt so `expandedRssNeeded = true` when **additional, broader news context is likely to change the setup**. Suggested triggers:
  - Day-trade or short-horizon setups with high intraday volatility.
  - Known or suspected near-term catalysts (earnings, FDA, regulatory actions, product launches, M&A rumors).
  - Thin liquidity or unusual price action where extra confirmation is needed.
  - Heavy technical dependence (setup relies on very recent flow/coverage).
  - Macro-sensitive names during active macro events (rates, CPI, Fed).
- Keep it false when: the setup is slow-moving, news-insensitive, or the standard core feeds already cover the expected catalysts.
- Use this flag to include expanded feeds for that ticker only.
- UI: show a small notice per ticker (“Expanded feeds used”) and offer a toggle to apply expanded feeds to all tickers **before Deep Dive / Web Search**.

## financial_resource reference excerpts (why the catalog exists)
financial_resource reads its feed list from `rss.db` and supports a “ticker” URL template per source.

Excerpt (how topics are pulled from the DB):
```
for row in c.execute("SELECT topic FROM feeds WHERE source = '{}'".format(self.__source)).fetchall():
    self.__possible_topics.append(row[0])
```

Excerpt (ticker template per source):
```
self.__ticker_url = c.execute(
    "SELECT url FROM feeds WHERE source = '{}' and topic='ticker'".format(self.__source)
).fetchone()[0]
```

Ticker templates present in the DB:
- Nasdaq: `https://www.nasdaq.com/feed/rssoutbound?symbol={}`
- Seeking Alpha: `https://seekingalpha.com/api/sa/combined/{}.xml`
- Yahoo Finance: `https://feeds.finance.yahoo.com/rss/2.0/headline?s={}&region=US&lang=en-US`
- Reddit: `https://www.reddit.com/r/{}/.rss`


## Implementation Steps (no code yet)
1) **Catalog generation**
   - Extract financial_resource `rss.db` into `rss_feeds.json` with a small script.
   - Select default topics per source and mark ticker templates.
2) **Model + resolver**
   - Add `RssFeedCatalog` + `RssFeedResolver` (domain or data layer).
   - Resolver inputs: tickers (with source info), selection settings, stage.
   - Output: de-duplicated feed URLs.
3) **Wire into pipeline**
   - Replace `DEFAULT_RSS_FEEDS` usage with resolver output in `RunAnalysisV2UseCase` and `requestDeepDive()`.
4) **Settings migration**
   - Migrate stored `rssFeeds` to new selection format (default on first run).
   - Remove `VerifyRssFeedUseCase` if no longer needed.
5) **UI updates**
   - Replace custom URL UI with curated source/topic toggles.
6) **Tests**
   - Unit tests for resolver (ticker inclusion rules, de-dup, limits).
   - Regression test for `BuildRssDigestUseCase` input list.

## Open Questions / Decisions Needed
- Which sources/topics are safe and desired for v1 (licensing + noise)?
- Should user-entered tickers always force ticker feeds even if the user disables them globally?
- Do we want a “market news only” mode vs “full topics” mode?

## Proposed Default Policy (updated)
- Always include ticker-specific feeds for `TickerSource.CUSTOM` tickers (ticker-only).
- For final-stage tickers: include ticker feeds **only when `rssNeeded == true`**.
- Core feeds always run; expanded feeds apply per ticker when `rss_expanded_needed == true`.
- Core + Expanded are the only global RSS sources; no user-entered feed URLs.
- Start with 2 ticker-feed sources (Yahoo + Seeking Alpha), optional third (Nasdaq).
- **LLM prompt guidance**: `rssNeeded` should not be overly strict with a bias toward `true` when recent catalysts or material news could affect the setup which will often be the case.
