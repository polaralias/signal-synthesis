# V2 Manual Real-Provider Verification Template

Status: `untested`

Purpose:

- provide the exact repo surface where a real-provider staged verification run should be captured
- avoid leaving the final proof artefact only in chat once live credentials and a device/session are available

Use this note only after an actual manual staged-pipeline run has been completed.
Do not mark this note as evidence until the run has happened.

## Run Metadata

- Date:
- Tester:
- App build / commit:
- Device or emulator:
- Network environment:

## Provider And Routing Setup

- Market-data provider keys used:
- LLM provider keys used:
- Effective stage routing:
  - `SHORTLIST`:
  - `DECISION_UPDATE`:
  - `FUNDAMENTALS_NEWS_SYNTHESIS`:
- RSS settings:

## Input Scenario

- Trading intent:
- Risk tolerance:
- Asset class:
- Discovery mode:
- Custom tickers or screener settings:
- Mock mode enabled: yes/no

## Observed Stage Outputs

- Discovered universe:
- Tradeable symbols:
- Shortlist output:
- Targeted enrichment outcome:
- Ranked setups before decision update:
- Decision update keep/drop output:
- Resolved RSS feeds:
- RSS digest summary:
- Fundamentals/news synthesis:

## Failure And Fallback Notes

- Provider failures observed:
- Fallback behaviour observed:
- JSON or parsing issues observed:
- Any user-visible inconsistencies:

## Evidence Assessment

- Deterministic automated coverage reference:
- Manual scenario outcome:
- Does this run justify any status change:
- Remaining caveats:
