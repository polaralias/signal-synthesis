# Glossary

This glossary defines the repository's preferred language.

## Analysis Result

The persisted output of an analysis run. It contains ranked setups and may also contain staged AI artifacts such as `globalNotes`, `decisionUpdate`, `rssDigest`, and `fundamentalsNewsSynthesis`.

## Analysis Stage

A named LLM-driven stage in the staged pipeline. Current stages include `SHORTLIST`, `DECISION_UPDATE`, `FUNDAMENTALS_NEWS_SYNTHESIS`, `DEEP_DIVE`, and `RSS_VERIFY`.

## Control Plane

The part of the application that coordinates other systems rather than only presenting UI. In the current codebase, `AnalysisViewModel` functions as a control plane.

## Deterministic Core

The non-LLM market-data path: discovery, tradeability filtering, enrichment, and heuristic ranking.

## Discovery Mode

How candidate symbols are sourced before filtering and enrichment. Current modes are `STATIC`, `SCREENER`, and `CUSTOM`.

## Evidence Strength

How strongly a repository claim is supported.

Preferred status labels:

- `verified working`
- `verified limited`
- `known broken`
- `untested`

## Expanded RSS

A broader RSS/news feed set beyond core feeds, used when a setup or deep dive requires wider context.

## Fundamentals/News Synthesis

The late staged-pipeline step that combines final setups with RSS digest content to produce ranked review guidance and portfolio posture.

## Keep/Drop Decision Update

The staged-pipeline step that re-evaluates ranked setups after enrichment and can keep, drop, annotate, or request more news context for setups.

## Mock Mode

The setting that allows the app to operate without live market-data provider keys by falling back to mock data behavior.

## Provider Fallback

The repository strategy of trying multiple market-data providers in capability-specific order rather than relying on a single provider.

## RSS Digest

A per-symbol set of matched recent headlines assembled from selected feeds and used as staged analysis context.

## Shortlist Gate

The first LLM stage in the staged synthesis pipeline. It receives lightweight quote-level context and selects which tradeable symbols should receive deeper enrichment.

## Staged LLM Synthesis Pipeline

The supported product pipeline. It combines candidate discovery, tradeability filtering, shortlist gating, targeted enrichment, decision update, RSS digestion, and fundamentals/news synthesis into one staged analysis flow.

## Symbol Context

The combined enrichment payload for a symbol's profile, financial metrics, and sentiment.

## Ticker Source

The origin classification for a candidate symbol, such as `PREDEFINED`, `SCREENER`, or `CUSTOM`. RSS feed resolution and result interpretation depend on this.
