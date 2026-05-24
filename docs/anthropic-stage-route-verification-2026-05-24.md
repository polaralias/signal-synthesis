# Anthropic Stage-Route Verification 2026-05-24

Status: `verified working`

This note captures a real-provider verification run for Anthropic-routed staged analysis components using fixed deterministic fixture inputs.

It is narrower than `docs/v2-manual-real-provider-verification-2026-05-23.md`:

- that earlier note proves the full staged product path with real market-data providers plus live Gemini, OpenAI, and Anthropic stage routing
- this note proves the Anthropic staged route itself using real Claude calls for the LLM-driven stages

## Run Metadata

- Date: 2026-05-24
- Tester: Codex live verification
- Provider: Anthropic
- Model: `claude-sonnet-4-5`
- Invocation surface: `AnthropicStageRouteLiveVerificationTest`
- Network environment: local desktop network access

## Inputs

Fixed deterministic inputs were used for:

- shortlist candidate symbols and quotes: `AAPL`, `MSFT`, `NVDA`
- decision-update setups: `AAPL`, `MSFT`
- RSS digest headlines for synthesis: fixture headlines for `AAPL` and `MSFT`

This run intentionally isolated the live dependency to Claude-routed stages:

- `SHORTLIST`
- `DECISION_UPDATE`
- `FUNDAMENTALS_NEWS_SYNTHESIS`

## Observed Outputs

- shortlist symbols: `AAPL`, `NVDA`, `MSFT`
- shortlist global notes:
  - tech sector showed mixed signals across the candidates
  - all candidates were sufficiently liquid for moderate-risk swing analysis
  - volume patterns suggested institutional participation
  - broader EOD context was recommended
- decision update keep symbols: `AAPL`, `MSFT`
- decision update drop symbols: none
- synthesis review symbols: `AAPL`, `MSFT`

All three staged Anthropic calls completed successfully in the recorded report artifact:

- `SHORTLIST`
- `DECISION_UPDATE`
- `FUNDAMENTALS_NEWS_SYNTHESIS`

Report artifact:

- `app/build/reports/live-verification/anthropic-stage-route-live-report.json`

## Why This Matters

This note closes the prior provider-breadth gap for Claude support at the staged-route level.

The repository now has:

- deterministic automated staged-path coverage
- full live staged-product evidence for Gemini, OpenAI, and Anthropic with real market-data providers
- direct live Anthropic stage-route evidence with real Claude calls

## Residual Caveat

This note does not claim that the narrower stage-route run replaces the fuller market-data-backed evidence in `docs/v2-manual-real-provider-verification-2026-05-23.md`.

It claims the narrower and accurate thing:

- Anthropic is a verified working staged LLM route for shortlist, decision update, and fundamentals/news synthesis
