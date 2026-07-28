---
type: "Repository Guide"
title: "Signal Synthesis"
description: "Documents Signal Synthesis for the signal-synthesis repository."
timestamp: 2026-07-28T21:55:36Z
authority: canonical
verification: untested
owner: polaralias
tags:
  - signal-synthesis
  - repository-guide
navigation:
  role: entry-point
  order: 10
---
<p align="center">
  <img src="Signal%20Synthesis%20Banner.png" alt="Signal Synthesis banner" width="960" />
</p>

# Signal Synthesis

Signal Synthesis is an Android app for staged AI-assisted market screening, watchlist management, and trade-setup research using bring-your-own market-data and LLM providers.

## What It Does

The app helps a user move from broad market scanning to a smaller set of candidates worth researching further. It combines market-data provider fallbacks, staged synthesis prompts, saved watchlists, alerts, and locally stored AI summaries so research can be repeated and reviewed over time.

## Core Features

- staged LLM synthesis pipeline
- shortlist gating and targeted enrichment
- watchlist and history tracking
- market alerts and scheduled background work
- provider fallback across multiple market-data sources
- local encrypted API-key storage
- OpenAI, Gemini, and Anthropic model discovery during setup

## How It Works

Signal Synthesis uses:

- provider adapters for market data and news inputs
- a staged AI analysis flow rather than a single monolithic prompt
- Room for local persistence of watchlists, history, RSS state, and summaries
- WorkManager for background alert scheduling

The app is designed so users can supply their own provider credentials and choose the providers they want to rely on.

## Build And Run

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

## Project Structure

- `app/` Android application code and resources
- `docs/` product contract, architecture, decisions, and verification material
- `.github/workflows/` debug CI and release automation

## Documentation

Start with:

- [docs/product-contract.md](docs/product-contract.md)
- [docs/v2-verification-matrix.md](docs/v2-verification-matrix.md)
- [docs/codebase-map.md](docs/codebase-map.md)

For repository workflow and agent-focussed context, read [AGENTS.md](AGENTS.md).

## Repository knowledge

- [Documentation map](docs/knowledge/documentation-map.md) — RKE-managed reading order and relationship hub.
