<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" alt="Signal Synthesis app icon" width="160" />
</p>

# Signal Synthesis

Signal Synthesis is an Android application for staged LLM market screening and trade setup synthesis using bring-your-own market-data and LLM providers.

Current status: `verified working`

Supported product contract:

- one staged LLM synthesis pipeline
- shortlist gating, targeted enrichment, decision update, RSS digest, and fundamentals/news synthesis
- provider fallback with provider cooldown on access-denied and rate-limit responses, local persistence, watchlist/history support, and background alerts

Current implementation and evidence notes are tracked separately from the product contract.

## Read This First

Use this reading order if you are new to the repository:

1. [`docs/product-contract.md`](docs/product-contract.md)
2. [`GLOSSARY.md`](GLOSSARY.md)
3. [`AGENTS.md`](AGENTS.md)
4. [`docs/README.md`](docs/README.md)
5. [`docs/decisions/`](docs/decisions/)
6. [`docs/v2-verification-matrix.md`](docs/v2-verification-matrix.md)
7. [`docs/v2-proof-gap.md`](docs/v2-proof-gap.md)

## Repository Summary

The app currently exposes these main product surfaces:

- dashboard and analysis flow
- results and setup detail
- watchlist and history
- market alerts
- API key and settings management
- RSS/news-assisted research flows

The codebase currently supports:

- market-data provider fallback across Alpaca, Polygon/Massive, Finnhub, FMP, Twelve Data, and mock mode, with repository-level cooldown on `403` and `429` provider responses
- a staged LLM synthesis pipeline
- encrypted local storage for API keys
- OpenAI, Gemini, and Anthropic model discovery during API-key setup, with provider-specific curated defaults based on the models actually available to the saved key
- Room-backed persistence for watchlist, history, AI summaries, and RSS state
- background alert scheduling via WorkManager

Implementation drift and convergence work are documented in:

- [`docs/codebase-map.md`](docs/codebase-map.md)
- [`docs/analysis-viewmodel-pipeline-map.md`](docs/analysis-viewmodel-pipeline-map.md)
- [`docs/v2-convergence-plan.md`](docs/v2-convergence-plan.md)

## Verification Posture

Use these terms consistently when updating docs:

- `verified working`: directly evidenced by code plus meaningful validation
- `verified limited`: implemented and partially evidenced, with known gaps in scope or proof
- `known broken`: expected not to work correctly
- `untested`: not meaningfully validated yet

Current repository posture:

- staged LLM synthesis pipeline: `verified working`
- public repository readiness: `verified working`

Claim-upgrade rule:

- A capability should only be moved to `verified working` when it has meaningful deterministic automated coverage for the claimed path and a documented manual real-provider verification for externally dependent stages.
- The staged pipeline now meets that threshold through deterministic V2 contract coverage plus the documented live-provider note in [`docs/v2-manual-real-provider-verification-2026-05-23.md`](docs/v2-manual-real-provider-verification-2026-05-23.md).
- Full staged live-provider evidence now exists for Gemini, OpenAI, and Anthropic in [`docs/v2-manual-real-provider-verification-2026-05-23.md`](docs/v2-manual-real-provider-verification-2026-05-23.md), with the narrower Claude-only stage-route proof retained in [`docs/anthropic-stage-route-verification-2026-05-24.md`](docs/anthropic-stage-route-verification-2026-05-24.md).
- The public repository readiness claim is backed by the bounded publish-readiness pass in [`docs/public-readiness-verification-2026-05-24.md`](docs/public-readiness-verification-2026-05-24.md).

## Development Basics

Prerequisites:

- Android SDK
- JDK 17
- Android emulator or device

Build:

```bash
./gradlew assembleDebug
```

Unit tests:

```bash
./gradlew testDebugUnitTest
```

Connected Android tests:

```bash
./gradlew connectedDebugAndroidTest
```

## Notes

- Historical planning and archaeology live under [`docs/archive/`](docs/archive/).
  - Archive documents are historical context, not active contract.
- Provider research in [`docs/LLMProviders.md`](docs/LLMProviders.md) is useful context, but it is not a canonical support claim.
