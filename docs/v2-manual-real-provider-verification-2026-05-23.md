---
type: "Validation Evidence"
title: "V2 Manual Real-Provider Verification 2026-05-23"
description: "Documents V2 Manual Real-Provider Verification 2026-05-23 for the signal-synthesis repository."
timestamp: 2026-07-28T21:55:36Z
authority: evidence
verification: verified-limited
owner: polaralias
tags:
  - signal-synthesis
  - validation-evidence
navigation:
  role: reference
  order: 200
---
# V2 Manual Real-Provider Verification 2026-05-23

Status: `verified working`

This note captures completed real-provider staged-pipeline verification runs.

It satisfies the repository requirement for a documented manual real-provider scenario alongside deterministic automated coverage.

## Run Metadata

- Date: 2026-05-23
- Tester: Codex live verification
- App build / commit: `792f95c10c758bfdb427ba9e36be625b17bee388`
- Device or emulator: local JVM test invocation via `RunAnalysisV2LiveVerificationTest`
- Network environment: local desktop network access

## Provider And Routing Setup

### Run A: Gemini-routed staged verification

- Market-data provider keys used: Alpaca, Polygon/Massive, FMP, Finnhub, Twelve Data
- LLM provider keys used: Gemini
- Effective stage routing:
  - `SHORTLIST`: `GEMINI/gemini-2.5-flash`
  - `DECISION_UPDATE`: `GEMINI/gemini-2.5-flash`
  - `FUNDAMENTALS_NEWS_SYNTHESIS`: `GEMINI/gemini-2.5-flash`
- RSS settings:
  - enabled topic keys: `seeking_alpha:all_news`
  - enabled ticker source ids: `yahoo_finance`
  - ticker feeds for final stage: enabled
- force expanded feeds: disabled

### Run B: OpenAI-routed staged verification

- Date: 2026-05-24
- LLM provider keys used: OpenAI
- Effective stage routing:
  - `SHORTLIST`: `OPENAI/gpt-5.2`
  - `DECISION_UPDATE`: `OPENAI/gpt-5.2`
  - `FUNDAMENTALS_NEWS_SYNTHESIS`: `OPENAI/gpt-5.2`
- Transport note:
  - the first OpenAI attempts failed due to client-side timeout pressure
  - verification succeeded after increasing default OkHttp connect/read/write/call timeouts in the OpenAI service clients

### Run C: Anthropic-routed staged verification

- Date: 2026-05-24
- LLM provider keys used: Anthropic
- Effective stage routing:
  - `SHORTLIST`: `ANTHROPIC/claude-sonnet-4-5`
  - `DECISION_UPDATE`: `ANTHROPIC/claude-sonnet-4-5`
  - `FUNDAMENTALS_NEWS_SYNTHESIS`: `ANTHROPIC/claude-sonnet-4-5`
- Transport note:
  - verification succeeded after the Anthropic stage runner and service clients were updated to respect stage `timeoutMs` with explicit OkHttp timeouts

## Input Scenario

- Trading intent: `SWING`
- Risk tolerance: `MODERATE`
- Asset class: `STOCKS`
- Discovery mode: `CUSTOM`
- Custom tickers: `AAPL`, `MSFT`, `NVDA`, `TSLA`, `AMZN`, `META`
- Mock mode enabled: no

## Observed Stage Outputs

### Run A: Gemini-routed

- Discovered universe: `AAPL`, `MSFT`, `NVDA`, `TSLA`, `AMZN`, `META`
- Tradeable symbols: `AAPL`, `MSFT`, `NVDA`, `TSLA`, `AMZN`, `META`
- Shortlist output: `NVDA`, `AMZN`, `META`, `MSFT`
- Targeted enrichment outcome:
  - ranked setups before decision update: `NVDA`, `AMZN`, `META`, `MSFT`
  - global notes:
    - provided change data was null, so momentum assessment stayed limited
    - focus stayed on large-cap tech with strong liquidity for moderate-risk swing trading
    - volume was the main differentiator given the limited input surface
- Decision update keep/drop output:
  - keep: `NVDA`, `AMZN`
  - drop: `META`, `MSFT`
- Resolved RSS feeds:
  - `https://feeds.finance.yahoo.com/rss/2.0/headline?s=NVDA&region=US&lang=en-US`
  - `https://feeds.finance.yahoo.com/rss/2.0/headline?s=AMZN&region=US&lang=en-US`
- RSS digest summary:
  - matched tickers: `AMZN`, `NVDA`
- Fundamentals/news synthesis:
  - review symbols: `AMZN`, `NVDA`
  - portfolio guidance risk posture: `moderate`

### Run B: OpenAI-routed

- Discovered universe: `AAPL`, `MSFT`, `NVDA`, `TSLA`, `AMZN`, `META`
- Tradeable symbols: `AAPL`, `MSFT`, `NVDA`, `TSLA`, `AMZN`, `META`
- Shortlist output: `MSFT`, `AAPL`, `AMZN`, `META`
- Targeted enrichment outcome:
  - ranked setups before decision update: `AAPL`, `AMZN`, `META`, `MSFT`
  - global notes:
    - mega-cap tech correlation remained high across the candidate set
    - higher-timeframe trend and event risk were explicitly flagged for swing sizing
- Decision update keep/drop output:
  - keep: `AAPL`, `AMZN`, `META`
  - drop: `MSFT`
- Resolved RSS feeds:
  - `https://seekingalpha.com/market_currents.xml`
  - `https://feeds.finance.yahoo.com/rss/2.0/headline?s=AAPL&region=US&lang=en-US`
  - `https://feeds.finance.yahoo.com/rss/2.0/headline?s=AMZN&region=US&lang=en-US`
  - `https://feeds.finance.yahoo.com/rss/2.0/headline?s=META&region=US&lang=en-US`
- RSS digest summary:
  - matched tickers: `AAPL`, `AMZN`, `META`
- Fundamentals/news synthesis:
  - review symbols: `AMZN`, `AAPL`, `META`
  - portfolio guidance risk posture: `moderate`

### Run C: Anthropic-routed

- Discovered universe: `AAPL`, `MSFT`, `NVDA`, `TSLA`, `AMZN`, `META`
- Tradeable symbols: `AAPL`, `MSFT`, `NVDA`, `TSLA`, `AMZN`, `META`
- Shortlist output: `NVDA`, `AMZN`, `META`, `TSLA`
- Targeted enrichment outcome:
  - ranked setups before decision update: `NVDA`, `AMZN`, `META`, `TSLA`
  - global notes:
    - all shortlisted symbols were liquid mega-cap tech names suitable for swing-trading liquidity expectations
    - `NVDA` and `AMZN` were prioritised for stronger volume profiles
    - sector concentration was explicitly called out as a timing and diversification consideration
- Decision update keep/drop output:
  - keep: `NVDA`, `META`, `AMZN`
  - drop: `TSLA`
- Resolved RSS feeds:
  - `https://seekingalpha.com/market_currents.xml`
  - `https://feeds.finance.yahoo.com/rss/2.0/headline?s=NVDA&region=US&lang=en-US`
  - `https://feeds.finance.yahoo.com/rss/2.0/headline?s=AMZN&region=US&lang=en-US`
  - `https://feeds.finance.yahoo.com/rss/2.0/headline?s=META&region=US&lang=en-US`
- RSS digest summary:
  - matched tickers: `AMZN`, `META`, `NVDA`
- Fundamentals/news synthesis:
  - review symbols: `META`, `NVDA`, `AMZN`
  - portfolio guidance risk posture: `conservative`

## Failure And Fallback Notes

- Provider failures observed:
  - a separate OpenAI-routed attempt failed before shortlist with `HTTP 429`
- Provider failures observed:
  - later OpenAI-routed attempts failed with `SocketTimeoutException` until the OpenAI HTTP client timeouts were increased above OkHttp defaults
- Fallback behaviour observed:
  - Gemini served as the first successful live-provider proof surface before OpenAI transport was fixed
- Provider failures observed:
  - an initial Anthropic full live rerun was blocked by transient local Gradle resource-merger corruption rather than provider behaviour
- JSON or parsing issues observed:
  - none in the successful Gemini-routed, OpenAI-routed, or Anthropic-routed runs
- Any user-visible inconsistencies:
  - shortlist notes explicitly stated that change-based momentum assessment was limited because the incoming change field was null in that run

## Evidence Assessment

- Deterministic automated coverage reference:
  - `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/RunAnalysisV2UseCaseTest.kt`
  - `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/ShortlistCandidatesUseCaseTest.kt`
  - `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/UpdateDecisionsUseCaseTest.kt`
  - `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/SynthesizeFundamentalsAndNewsUseCaseTest.kt`
  - `app/src/test/java/com/polaralias/signalsynthesis/ui/AnalysisViewModelTest.kt`
- Manual scenario outcome:
  - successful live-provider staged runs completed with real market-data providers, live Gemini stage routing, live OpenAI stage routing, live Anthropic stage routing, RSS resolution, digest matching, and final synthesis
- Does this run justify any status change:
  - yes, it justifies upgrading the staged LLM synthesis pipeline product claim from `verified limited` to `verified working`
- Remaining caveats:
  - this note verifies the staged product path with Gemini, OpenAI, and Anthropic, not every possible external provider combination in the repository
  - provider-specific latency can still materially affect large staged prompts, so transport timeout settings remain part of runtime correctness

## Report Artefacts

- Gemini live report artefact: `app/build/reports/live-verification/v2-live-report-gemini.json`
- OpenAI live report artefact: `app/build/reports/live-verification/v2-live-report-openai.json`
- Anthropic live report artefact: `app/build/reports/live-verification/v2-live-report-anthropic.json`

## Repository knowledge

- [Documentation map](knowledge/documentation-map.md) — RKE-managed reading order and relationship hub.
