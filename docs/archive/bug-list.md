# OpenAI Responses API Alignment Bugs

Reviewed against:
- https://platform.openai.com/docs/api-reference/responses
- https://platform.openai.com/docs/guides/migrate-to-responses
- https://platform.openai.com/docs/guides/structured-outputs

Bug-fix pass status summary:
- DONE: 14
- PENDING: 0

## OPENAI-RESP-001 (High)
Title: OpenAI provider default API format is still marked as chat
Status: DONE
Resolution check:
- `app/src/main/java/com/polaralias/signalsynthesis/domain/ai/LlmModels.kt` now sets `LlmProvider.OPENAI.apiFormat = LlmApiFormat.OPENAI_RESPONSES`.

Evidence:
- `app/src/main/java/com/polaralias/signalsynthesis/domain/ai/LlmModels.kt:34` sets `LlmProvider.OPENAI.apiFormat = LlmApiFormat.OPENAI_CHAT`.

Why this is a bug:
- Our policy is "all OpenAI models use Responses API."
- Keeping OpenAI provider default as chat increases risk of wrong endpoint selection in config/export/migration paths.

Required change:
- Set OpenAI provider default API format to `OPENAI_RESPONSES`.
- Audit code that relies on provider-level API format and ensure no OpenAI path prefers chat completions.

Acceptance criteria:
- OpenAI provider resolves to Responses API by default, even when model metadata is incomplete.

## OPENAI-RESP-002 (High)
Title: OpenAI stage runner still falls back to `/v1/chat/completions`
Status: DONE
Resolution check:
- `app/src/main/java/com/polaralias/signalsynthesis/data/ai/OpenAiStageRunner.kt` now routes only through Responses API paths.
- No `LlmProvider.OPENAI` stage runner path references `OpenAiService.createChatCompletion(...)`.

Evidence:
- `app/src/main/java/com/polaralias/signalsynthesis/data/ai/OpenAiStageRunner.kt:17-20` routes to `runStandardChat(...)` when `usesOpenAiResponsesApi(model)` is false.
- `app/src/main/java/com/polaralias/signalsynthesis/data/ai/OpenAiStageRunner.kt:25-50` calls `OpenAiService.createChatCompletion(...)`.
- `app/src/main/java/com/polaralias/signalsynthesis/data/ai/OpenAiService.kt:14` posts to `v1/chat/completions`.

Why this is a bug:
- OpenAI models must always use `POST /v1/responses` per project policy and migration guidance.

Required change:
- Remove chat fallback for `LlmProvider.OPENAI` stage execution.
- Route every OpenAI stage request through `OpenAiResponsesService`.

Acceptance criteria:
- No `LlmProvider.OPENAI` stage execution path can hit `OpenAiService.createChatCompletion`.

## OPENAI-RESP-003 (High)
Title: General OpenAI client still falls back to `/v1/chat/completions`
Status: DONE
Resolution check:
- `app/src/main/java/com/polaralias/signalsynthesis/data/ai/OpenAiLlmClient.kt` now uses `OpenAiResponsesService` only.

Evidence:
- `app/src/main/java/com/polaralias/signalsynthesis/data/ai/OpenAiLlmClient.kt:28-39` uses Responses only conditionally.
- `app/src/main/java/com/polaralias/signalsynthesis/data/ai/OpenAiLlmClient.kt:40-58` fallback path uses `OpenAiService.createChatCompletion(...)`.

Why this is a bug:
- Same policy violation as stage runner: OpenAI calls must be Responses-only.

Required change:
- Make `OpenAiLlmClient` Responses-only for `LlmProvider.OPENAI`.
- Remove or isolate legacy chat path from OpenAI provider code.

Acceptance criteria:
- `OpenAiLlmClient` never calls chat completions for OpenAI models.

## OPENAI-RESP-004 (Medium)
Title: `usesOpenAiResponsesApi(...)` does not cover all OpenAI model families
Status: DONE
Resolution check:
- `app/src/main/java/com/polaralias/signalsynthesis/domain/ai/LlmModels.kt` now classifies unknown OpenAI-family IDs via provider inference and returns Responses routing for OpenAI.

Evidence:
- `app/src/main/java/com/polaralias/signalsynthesis/domain/ai/LlmModels.kt:549-552` only returns true for `gpt-5*` and `computer-use*` heuristics.
- Same file infers OpenAI provider for `o1/o3/o4/chatgpt*` at `:532-537`.
- Responses API docs examples include OpenAI models like `o3` and `gpt-4o` in `POST /v1/responses` usage.

Why this is a bug:
- OpenAI models outside the narrow heuristic can be misrouted to chat fallback.

Required change:
- Prefer provider-based routing (`OPENAI => responses`) instead of model-name heuristics.
- If helper remains, expand coverage to all supported OpenAI model families.

Acceptance criteria:
- Representative OpenAI IDs (`gpt-5.2`, `o3`, `gpt-4o`, `chatgpt-*`) are all classified as Responses.

## OPENAI-RESP-005 (Medium)
Title: Responses structured output parameters are not wired (`text.format`)
Status: DONE
Resolution check:
- `app/src/main/java/com/polaralias/signalsynthesis/data/ai/OpenAiResponsesService.kt` request DTO includes `text`.
- `app/src/main/java/com/polaralias/signalsynthesis/data/ai/OpenAiStageRunner.kt` sets `text.format` when `expectedSchemaId` is present.

Evidence:
- `app/src/main/java/com/polaralias/signalsynthesis/data/ai/OpenAiStageRunner.kt:53-61` and `:73-83` build `OpenAiResponseRequest` without `text.format`, even when `expectedSchemaId` is present.
- `app/src/main/java/com/polaralias/signalsynthesis/data/ai/OpenAiResponsesService.kt:37-46` request model has no `text` field.
- Migration docs specify: in Responses API, structured output should use `text.format` (not chat `response_format`).

Why this is a bug:
- JSON-shape enforcement for schema-bound stages is weaker than intended, increasing parse-failure risk.

Required change:
- Add Responses `text` configuration support in request DTOs.
- For schema-required stages, set `text.format` (`json_object` or `json_schema` as appropriate).

Acceptance criteria:
- Schema-bound OpenAI stages use native Responses structured output config and no longer rely on best-effort brace extraction alone.


Findings (Severity-Ordered)

[HIGH][DONE] Potential infinite retry loop on HTTP 429
RetryHelper.kt (line 39) does continue for 429 without incrementing attempts, while attempts are only incremented at RetryHelper.kt (line 53).
Impact: a persistent 429 can stall analysis indefinitely.

[HIGH][DONE] Unit test suite is currently broken (compile-time)
./gradlew testDebugUnitTest and ./gradlew testReleaseUnitTest both fail because RssDao now requires getItemsForFeed at RssDao.kt (line 17), but fakes do not implement it at BuildRssDigestUseCaseTest.kt (line 42) and AnalysisViewModelTest.kt (line 183).
Impact: CI/unit regression coverage is blocked.

[HIGH][DONE] Staged enrichment logic can skip enrichment for part of shortlist
Code comment says defaults should run for symbols without explicit requests at RunAnalysisV2UseCase.kt (line 149), but when any symbol requests intraday/context/eod, only that subset is enriched (...RunAnalysisV2UseCase.kt (line 150), ...RunAnalysisV2UseCase.kt (line 156), ...RunAnalysisV2UseCase.kt (line 163)).
Impact: shortlisted symbols may be ranked with incomplete data.

[MEDIUM][DONE] Keep/drop filtering is case-sensitive and unnormalized
Decision symbols are read raw from JSON at DecisionUpdate.kt (line 69) and DecisionUpdate.kt (line 88), then matched case-sensitively at RunAnalysisV2UseCase.kt (line 194) and RunAnalysisV2UseCase.kt (line 195).
Impact: if LLM returns lowercase/mixed-case tickers, keep/drop instructions may be ignored.

[MEDIUM][DONE] includeMock flag in ProviderFactory is ineffective
Constructor has includeMock at ProviderFactory.kt (line 13), but mock provider creation ignores it and only checks key presence at ProviderFactory.kt (line 40).
Impact: config toggle is dead; behavior differs from API contract expectations.

[MEDIUM][DONE] RSS ticker regex is not escaped
Ticker is interpolated directly into regex at BuildRssDigestUseCase.kt (line 67) and BuildRssDigestUseCase.kt (line 68).
Impact: symbols containing regex metacharacters (for example .) can produce false matches.

[MEDIUM][DONE] Destructive Room migration is enabled
MainActivity.kt (line 39) uses .fallbackToDestructiveMigration().
Impact: app updates with schema changes can wipe local data (history/cache/settings tables in DB).

[LOW][DONE] Static context holder pattern flagged as leak risk
UsageTracker singleton at ActivityLogger.kt (line 161) keeps context field at ActivityLogger.kt (line 170) (initialized via application context at ...ActivityLogger.kt (line 181)).
Impact: low practical risk due applicationContext, but lint correctly flags lifecycle risk pattern.

[LOW][DONE] Locale-sensitive formatting used widely
lintDebug reports 34 DefaultLocale warnings (example: AnalysisViewModel.kt (line 742), MarketAlertWorker.kt (line 175), UpdateDecisionsUseCase.kt (line 75)).
Impact: formatting/parsing behavior can vary by device locale.

Verification Run Summary

./gradlew assembleDebug --no-daemon passed.
./gradlew lintDebug --no-daemon passed with warnings.
./gradlew testDebugUnitTest --no-daemon passed.
./gradlew testReleaseUnitTest --no-daemon passed.
Residual Risk / Coverage Gaps

No pending items from this bug-fix pass were found in current code review scope.

This is a static + build/lint/unit-test verification pass; runtime-only issues (device/network/provider-specific) still need instrumentation or manual scenario testing.
