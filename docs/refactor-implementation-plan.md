# Refactor plan for the new flow

## Target flow

1. **API discovery**
2. **LLM shortlist**
3. **Targeted enrichment APIs**
4. **LLM decision update**
5. **Local RSS pull**
6. **LLM fundamentals + news synthesis**
7. **Deep dive button** → LLM with **web search tool**

## Key design principles

* The LLM is a **gate + planner**, not an always-on narrator.
* Every LLM step returns **strict JSON** you can parse and enforce.
* RSS is **local and cached**, pulled only for shortlisted tickers.
* Deep dive is **explicitly user-triggered**, with hard limits.

---

# Architecture changes mapped to your codebase

## 1) Replace single-pass orchestration with staged orchestration

Today `RunAnalysisUseCase` does:

* discover → tradeable → quotes → intraday(all) → context(all) → eod(all) → rank

Refactor to a staged pipeline (either modify `RunAnalysisUseCase` or create `RunAnalysisV2UseCase` and keep the old one as fallback).

### New staged `RunAnalysisV2UseCase` responsibilities

**Stage A: API discovery**

* Keep `DiscoverCandidatesUseCase` and `FilterTradeableUseCase`.
* Fetch quotes for tradeable universe (prefer batching, but not mandatory for this refactor step).

**Stage B: LLM shortlist**

* New use case: `ShortlistCandidatesUseCase` (LLM structured output)
* Inputs: user config + tradeable symbols + quotes (and whatever lightweight stats you have without intraday)
* Output: shortlist (tickers + priority + requested enrichment types)

**Stage C: Targeted enrichment APIs**

* Only enrich tickers from shortlist.
* Split enrichment based on what shortlist requested:

  * intraday for day-trade candidates
  * eod for swing/long-term
  * context (profile/metrics/sentiment) only if requested

**Stage D: LLM decision update**

* New use case: `UpdateDecisionsUseCase` (LLM structured output)
* Inputs: enriched signals for shortlisted tickers + user risk config
* Output: final set of “contenders to present”, plus what needs RSS.

**Stage E: Local RSS pull**

* New use case: `BuildRssDigestUseCase`
* Inputs: final tickers, time window, feed list
* Output: per-ticker digest (headlines + timestamps + snippets)

**Stage F: Fundamentals + news synthesis**

* New use case: `SynthesizeFundamentalsAndNewsUseCase`
* Inputs: per-ticker fundamentals + technicals + rss digest + user config
* Output: structured “what to review” list + per-ticker narrative

**Stage G: Deep dive (UI action)**

* New use case: `DeepDiveUseCase`
* Inputs: ticker, current setup snapshot, rss digest (if any)
* Calls OpenAI Responses API with `tools: [{type:"web_search"}]` and strict constraints. ([OpenAI Platform][1])

---

# Data contracts (structured outputs)

Your existing `SynthesizeSetupUseCase` already does JSON extraction in `parseResponse`. Reuse that pattern, but tighten it:

### Shortlist JSON (LLM shortlist)

Example schema:

```json
{
  "shortlist": [
    {
      "symbol": "AAPL",
      "priority": 0.92,
      "reasons": ["liquid", "trend_strength", "fits_risk"],
      "requested_enrichment": ["INTRADAY", "EOD", "FUNDAMENTALS"],
      "avoid": false,
      "risk_flags": ["earnings_soon"]
    }
  ],
  "global_notes": ["Prefer conservative stops today due to elevated volatility"],
  "limits_applied": { "max_shortlist": 15 }
}
```

### Decision update JSON (LLM decision update)

```json
{
  "keep": [
    {
      "symbol": "AAPL",
      "confidence": 0.81,
      "setup_bias": "bullish",
      "must_review": ["invalidations", "macro_headlines"],
      "rss_needed": true
    }
  ],
  "drop": [
    { "symbol": "XYZ", "reasons": ["illiquid", "spread"] }
  ],
  "limits_applied": { "max_keep": 10 }
}
```

### Fundamentals + news synthesis JSON

```json
{
  "ranked_review_list": [
    {
      "symbol": "AAPL",
      "what_to_review": [
        "Key catalyst from news",
        "Invalidation level",
        "Earnings proximity risk"
      ],
      "risk_summary": ["headline risk", "gap risk"],
      "one_paragraph_brief": "..."
    }
  ],
  "portfolio_guidance": {
    "position_count": 3,
    "risk_posture": "moderate"
  }
}
```

### Deep dive JSON (web search tool)

```json
{
  "summary": "What’s driving the move today...",
  "drivers": [
    { "type": "news", "direction": "bullish", "detail": "..." }
  ],
  "risks": ["..."],
  "what_changes_my_mind": ["..."],
  "sources": [
    { "title": "...", "publisher": "...", "published_at": "..." }
  ]
}
```

---

# Local RSS plan (no backend)

## Minimal local RSS components

Add package: `data/rss`

* `RssFeedClient` (OkHttp + conditional GET via ETag/Last-Modified)
* `RssParser` (Android XmlPullParser is fine)
* Room entities:

  * `RssFeedStateEntity(feedUrl, etag, lastModified, lastFetchedAt)`
  * `RssItemEntity(feedUrl, title, link, publishedAt, snippet, guidHash, fetchedAt)`
* DAO queries:

  * get items by time window
  * delete old items (TTL)
* `RssDigestBuilder`:

  * inputs: tickers, company aliases (optional), items
  * output: per-ticker list capped to N headlines sorted by recency

## Matching tickers locally

Start simple, then improve:

1. match `$AAPL` cashtags and bare tickers in titles
2. match company name from profile if available (optional)
3. add a small alias map (manual) for common collisions

---

# Deep dive with OpenAI web search tool

You currently call `POST /v1/chat/completions`.

For deep dive you’ll add `POST /v1/responses` and enable web search via the `tools` array. ([OpenAI Platform][1])

Key constraints you should enforce in your request/prompt:

* max queries: 2–3
* time window: last 24–72h
* output must be JSON only
* require sources in output (and also request `include` sources in API response if you want tool call sources) ([OpenAI Platform][2])

Important: the web search tool can be called multiple times by the model if you let it, so you should explicitly instruct it to do **at most one tool call round** and limit queries in the prompt.

---

# Implementation plan (PR-sized steps)

## PR 1: Add domain models + parsing utilities for structured LLM outputs

**Goal:** Make strict JSON parsing a first-class thing (reused by multiple LLM stages).

Deliverables:

* `domain/model/ShortlistPlan.kt`
* `domain/model/DecisionUpdate.kt`
* `domain/model/RssDigest.kt`
* `domain/model/FundamentalsNewsSynthesis.kt`
* Shared JSON extraction + parsing helper (reuse your `extractJson` logic but centralise it)

Acceptance:

* Unit tests for parsing happy path and partial/malformed JSON
* Fallback behaviour is deterministic (empty lists, “Unavailable” strings)

---

## PR 2: LLM shortlist stage (no orchestration changes yet)

**Goal:** Implement `ShortlistCandidatesUseCase` that takes “discovery outputs” and returns shortlist.

Deliverables:

* `ShortlistCandidatesUseCase`
* New prompt template(s) in `AiPrompts`
* Add a new button or dev toggle to run shortlist on a sample set for testing

Acceptance:

* Returns stable JSON for same inputs
* Applies `max_shortlist` and symbols are always subset of input symbols

---

## PR 3: New orchestration `RunAnalysisV2UseCase` with shortlist gating

**Goal:** Wire flow up to: discovery → tradeable → quotes → shortlist → (enrichment only for shortlist) → rank.

Deliverables:

* `RunAnalysisV2UseCase`
* Keep existing `RunAnalysisUseCase` unchanged for fallback
* Update DI / factories where needed

Acceptance:

* For same discovery universe, total API calls drop (verify via your `UsageTracker`)
* Setup results still produced and app remains functional

---

## PR 4: Decision update stage

**Goal:** Add second LLM pass after enrichment.

Deliverables:

* `UpdateDecisionsUseCase`
* Modify `RunAnalysisV2UseCase` to:

  * run enrichment for shortlist
  * run decision update
  * only keep final “review list” tickers for presentation

Acceptance:

* Drops tickers with explicit reasons
* Produces “must_review” list per ticker

---

## PR 5: Local RSS ingestion + digest

**Goal:** Add on-demand RSS pulling during synthesis run.

Deliverables:

* Room entities + DAOs for RSS cache
* `RssFeedClient`, `RssParser`
* `BuildRssDigestUseCase`
* Integrate into `RunAnalysisV2UseCase` after decision update, but only for final tickers

Acceptance:

* First run fetches feeds, next run uses cached items (ETag/Last-Modified honoured)
* Digest returns max N items per ticker within time window

---

## PR 6: Fundamentals + news synthesis stage

**Goal:** Produce “what to review” and narrative output that incorporates both APIs and RSS.

Deliverables:

* `SynthesizeFundamentalsAndNewsUseCase`
* UI wiring: show “review checklist” and summary per ticker
* Store synthesis to DB if you want persistence

Acceptance:

* Output is structured and UI is driven off that structure, not free text
* Works even when RSS fails (empty digest)

---

## PR 7: Deep dive with web search tool (user-triggered)

**Goal:** Add a “Deep dive” button on setup detail screen.

Deliverables:

* `OpenAiResponsesService` (`POST /v1/responses`)
* `OpenAiWebSearchClient` (new interface, or extend `LlmClient`)
* `DeepDiveUseCase`
* UI states: idle → loading → results → error

Acceptance:

* Only runs on explicit user action
* Hard limits are enforced (max queries, time window, timeout)
* Outputs sources list and displays them
