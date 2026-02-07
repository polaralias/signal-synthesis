# Refactor + staged pipeline implementation reference (authoritative)

Last updated: 2026-01-30

This document is the source of truth for validating the staged pipeline, RSS ingestion, and Deep Dive tooling. It includes exact code excerpts and file paths so an agent can verify behavior without searching the repo.

---

# 1) What is implemented today (authoritative)

## 1.1 V1 (single-pass) pipeline

- Implemented in `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RunAnalysisUseCase.kt`.
- It discovers candidates, filters tradeable, enriches *all* tradeable symbols, then ranks them.

Excerpt (V1 flow):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RunAnalysisUseCase.kt
// Steps: discover -> tradeable -> quotes -> intraday -> context -> eod -> rank
val tradeable = filterTradeable.execute(symbols, minPrice = minPrice)
val quotes = repository.getQuotes(tradeable)
val intradayStats = enrichIntraday.execute(tradeable, days = 2)
val contextData = enrichContext.execute(tradeable)
val eodStats = if (intent != TradingIntent.DAY_TRADE) {
    enrichEod.execute(tradeable, days = 200)
} else {
    emptyMap()
}
val rawSetups = rankSetups.execute(
    symbols = tradeable,
    quotes = quotes,
    intradayStats = intradayStats,
    eodStats = eodStats,
    contextData = contextData,
    intent = intent
)
```

## 1.2 V2 (staged) pipeline with shortlist gate

- Implemented in `app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RunAnalysisV2UseCase.kt`.
- Flow is staged and uses an LLM shortlist to reduce enrichment calls.

Excerpt (V2 high-level flow):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RunAnalysisV2UseCase.kt
// 1) Discover 2) Filter tradeable 3) Quotes 4) LLM shortlist
val quotes = repository.getQuotes(tradeable)
val shortlistPlan = shortlistCandidates.execute(
    symbols = tradeable,
    quotes = quotes,
    intent = intent,
    risk = risk,
    maxShortlist = maxShortlist
)

val shortlistedSymbols = shortlistPlan.shortlist
    .filter { !it.avoid }
    .map { it.symbol }
    .filter { tradeable.contains(it) }
```

Excerpt (V2 targeted enrichment driven by `requested_enrichment`):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/RunAnalysisV2UseCase.kt
val symbolsRequestingIntraday = shortlistPlan.shortlist
    .filter { it.requestedEnrichment.contains("INTRADAY") && shortlistedSymbols.contains(it.symbol) }
    .map { it.symbol }

val symbolsRequestingEod = shortlistPlan.shortlist
    .filter { it.requestedEnrichment.contains("EOD") && shortlistedSymbols.contains(it.symbol) }
    .map { it.symbol }

val symbolsRequestingContext = shortlistPlan.shortlist
    .filter { (it.requestedEnrichment.contains("FUNDAMENTALS") || it.requestedEnrichment.contains("SENTIMENT")) && shortlistedSymbols.contains(it.symbol) }
    .map { it.symbol }

val intradayStats = if (symbolsRequestingIntraday.isNotEmpty()) {
    enrichIntraday.execute(symbolsRequestingIntraday, days = 2)
} else {
    enrichIntraday.execute(shortlistedSymbols, days = 2)
}
```

## 1.3 Pipeline selection in UI (V1 vs V2)

- The UI chooses V1 or V2 via the `useStagedPipeline` setting in `AppSettings`.
- If staged is enabled, LLM key is required before the run.

Excerpt (selection in `AnalysisViewModel.runAnalysis`):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/ui/AnalysisViewModel.kt
val useStaged = uiStateValue.appSettings.useStagedPipeline
if (useStaged && llmKey.isBlank()) {
    _uiState.update { it.copy(errorMessage = "LLM Key is required for the staged pipeline (shortlist stage).") }
    return
}

val result = if (useStaged) {
    val rssClient = RssFeedClient(rssDao = rssDao)
    val rssDigestBuilder = BuildRssDigestUseCase(rssClient, rssDao)
    val useCase = RunAnalysisV2UseCase(repository, stageModelRouter, rssDigestBuilder, clock)
    useCase.execute(
        intent = state.intent,
        risk = state.appSettings.riskTolerance,
        assetClass = state.assetClass,
        discoveryMode = state.appSettings.discoveryMode,
        llmKey = llmKey,
        customTickers = customTickerList,
        blocklist = state.blocklist,
        screenerThresholds = mapOf(
            "conservative" to state.appSettings.screenerConservativeThreshold,
            "moderate" to state.appSettings.screenerModerateThreshold,
            "aggressive" to state.appSettings.screenerAggressiveThreshold
        ),
        rssFeeds = DEFAULT_RSS_FEEDS,
        onProgress = { msg ->
            _uiState.update { it.copy(progressMessage = msg) }
        }
    )
} else {
    val useCase = RunAnalysisUseCase(repository, clock)
    useCase.execute(
        intent = state.intent,
        risk = state.appSettings.riskTolerance,
        assetClass = state.assetClass,
        discoveryMode = state.appSettings.discoveryMode,
        customTickers = customTickerList,
        blocklist = state.blocklist,
        screenerThresholds = mapOf(
            "conservative" to state.appSettings.screenerConservativeThreshold,
            "moderate" to state.appSettings.screenerModerateThreshold,
            "aggressive" to state.appSettings.screenerAggressiveThreshold
        )
    )
}
```

---

# 2) LLM stage routing and tooling (authoritative)

## 2.1 Stage routing config and defaults

- Routing is centralized in `StageModelRouter` + `StageModelConfig`.
- Only `AnalysisStage.DEEP_DIVE` can use tools; everything else is forced to `ToolsMode.NONE`.

Excerpt (router guardrails):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/domain/ai/StageModelRouter.kt
val finalToolsMode = when (stage) {
    AnalysisStage.DEEP_DIVE -> stageConfig.tools
    else -> ToolsMode.NONE
}

val finalRequest = request.copy(
    stage = stage,
    toolsMode = finalToolsMode,
    maxOutputTokens = stageConfig.maxOutputTokens,
    timeoutMs = stageConfig.timeoutMs,
    temperature = stageConfig.temperature,
    reasoningDepth = stageConfig.reasoningDepth
)
```

Excerpt (default configs):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/domain/ai/StageModelConfig.kt
AnalysisStage.SHORTLIST -> StageModelConfig(
    provider = LlmProvider.OPENAI,
    model = "gpt-5.2",
    temperature = 0.2f,
    reasoningDepth = ReasoningDepth.MEDIUM,
    maxOutputTokens = 1000
)
AnalysisStage.DEEP_DIVE -> StageModelConfig(
    provider = LlmProvider.OPENAI,
    model = "gpt-5.2",
    tools = ToolsMode.WEB_SEARCH,
    temperature = 0.2f,
    reasoningDepth = ReasoningDepth.HIGH,
    maxOutputTokens = 2000,
    timeoutMs = 60_000L
)
```

## 2.2 LLM stage request/response contract

Excerpt (request/response payloads):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/domain/ai/StageLlmRunner.kt
enum class ToolsMode { NONE, WEB_SEARCH, GOOGLE_SEARCH }

data class LlmStageRequest(
    val systemPrompt: String,
    val userPrompt: String,
    val stage: AnalysisStage,
    val expectedSchemaId: String? = null,
    val timeoutMs: Long = 30_000L,
    val maxOutputTokens: Int = 2000,
    val toolsMode: ToolsMode = ToolsMode.NONE,
    val temperature: Float? = null,
    val reasoningDepth: ReasoningDepth = ReasoningDepth.MEDIUM
)

data class LlmStageResponse(
    val rawText: String,
    val parsedJson: String? = null,
    val sources: List<LlmSource> = emptyList(),
    val providerDebug: String? = null
)
```

---

# 3) Shortlist stage (LLM gate)

## 3.1 Prompt template

Excerpt (shortlist prompt):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/domain/ai/AiPrompts.kt
const val SHORTLIST_PROMPT = """
    You are a trading strategist assistant. Your task is to shortlist a set of tradeable symbols for deeper analysis.
    
    Input Data:
    - Trading Intent: {intent}
    - Risk Tolerance: {risk}
    - Candidate Symbols and Quotes:
    {quotesData}
    - Constraints: {constraints}
    
    Guidelines:
    1. Select at most {maxShortlist} symbols ...
    2. For each symbol, provide ...
       - "requested_enrichment": ["INTRADAY", "EOD", "FUNDAMENTALS", "SENTIMENT"]
    ...
"""
```

## 3.2 Shortlist use case

Excerpt (ShortlistCandidatesUseCase execution):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/ShortlistCandidatesUseCase.kt
val prompt = AiPrompts.SHORTLIST_PROMPT
    .replace("{intent}", intent.name)
    .replace("{risk}", risk.name)
    .replace("{quotesData}", quotesData)
    .replace("{maxShortlist}", maxShortlist.toString())
    .replace("{constraints}", "Select at most $maxShortlist symbols.")
    .trimIndent()

val request = LlmStageRequest(
    systemPrompt = AiPrompts.SYSTEM_ANALYST,
    userPrompt = prompt,
    stage = AnalysisStage.SHORTLIST,
    expectedSchemaId = "ShortlistPlan"
)

val response = stageModelRouter.run(AnalysisStage.SHORTLIST, request)
val json = response.parsedJson ?: JsonExtraction.extractFirstJsonObject(response.rawText)
return ShortlistPlan.fromJson(json)
```

## 3.3 Shortlist JSON parsing

Excerpt (parser fields are authoritative):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/domain/model/ShortlistPlan.kt
val globalNotes = obj.optJSONArray("global_notes").toStringList()
val limitsObj = obj.optJSONObject("limits_applied")

// ShortlistItem fields
ShortlistItem(
    symbol = obj.optString("symbol", ""),
    priority = obj.optDouble("priority", 0.0),
    reasons = obj.optJSONArray("reasons").toStringList(),
    requestedEnrichment = obj.optJSONArray("requested_enrichment").toStringList(),
    avoid = obj.optBoolean("avoid", false),
    riskFlags = obj.optJSONArray("risk_flags").toStringList()
)
```

---

# 4) LLM tool usage and JSON enforcement

## 4.1 OpenAI stage runner (chat + web search)

Excerpt (standard JSON enforcement via `response_format`):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/data/ai/OpenAiStageRunner.kt
val openAiRequest = OpenAiChatRequest(
    model = model,
    messages = listOf(
        OpenAiMessage(role = "system", content = request.systemPrompt),
        OpenAiMessage(role = "user", content = request.userPrompt)
    ),
    maxCompletionTokens = request.maxOutputTokens,
    reasoningEffort = mapReasoningEffort(request.reasoningDepth),
    temperature = request.temperature,
    responseFormat = if (request.expectedSchemaId != null) {
         OpenAiResponseFormat(type = "json_object")
    } else null
)
```

Excerpt (Responses API for web search + sources):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/data/ai/OpenAiStageRunner.kt
val openAiRequest = OpenAiResponseRequest(
    model = model,
    input = "${request.systemPrompt}\n\n${request.userPrompt}",
    tools = listOf(OpenAiTool(type = "web_search")),
    include = listOf("tool_calls"),
    reasoningEffort = mapReasoningEffort(request.reasoningDepth),
    temperature = request.temperature
)

val response = responsesService.createResponse("Bearer $apiKey", openAiRequest)
val text = response.output?.text.orEmpty()

val sources = response.output?.toolCalls?.flatMap { call ->
    call.webSearchCall?.result?.sources?.map { source ->
        LlmSource(
            title = source.title ?: "Untitled",
            url = source.url ?: "",
            snippet = null
        )
    } ?: emptyList()
} ?: emptyList()
```

## 4.2 OpenAI Responses API payload shape

Excerpt (Retrofit models):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/data/ai/OpenAiResponsesService.kt
data class OpenAiResponseRequest(
    val model: String,
    val input: String,
    val tools: List<OpenAiTool>? = null,
    val include: List<String>? = null,
    @Json(name = "reasoning_effort") val reasoningEffort: String? = null,
    val temperature: Float? = null
)
```

## 4.3 Gemini stage runner with Google Search tool

Excerpt (ToolsMode.GOOGLE_SEARCH only):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/data/ai/GeminiStageRunner.kt
val tools = if (request.toolsMode == ToolsMode.GOOGLE_SEARCH) {
    listOf(GeminiTool(googleSearch = GoogleSearchTool()))
} else null

val sources = candidate?.groundingMetadata?.groundingChunks?.mapNotNull { chunk ->
    chunk.web?.let { web ->
        LlmSource(
            title = web.title ?: "Untitled",
            url = web.uri ?: "",
            snippet = null
        )
    }
} ?: emptyList()
```

---

# 5) RSS ingestion and digest (local only)

## 5.1 RSS entities + DAO

Excerpt (Room entities):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/data/rss/RssEntities.kt
@Entity(tableName = "rss_feed_states")
data class RssFeedStateEntity(
    @PrimaryKey val feedUrl: String,
    val etag: String? = null,
    val lastModified: String? = null,
    val lastFetchedAt: Long = 0
)

@Entity(tableName = "rss_items")
data class RssItemEntity(
    @PrimaryKey val guidHash: String,
    val feedUrl: String,
    val title: String,
    val link: String,
    val publishedAt: Long,
    val snippet: String,
    val fetchedAt: Long
)
```

Excerpt (DAO):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/data/rss/RssDao.kt
@Query("SELECT * FROM rss_items WHERE publishedAt >= :since ORDER BY publishedAt DESC")
suspend fun getAllRecentItems(since: Long): List<RssItemEntity>

@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insertItems(items: List<RssItemEntity>)

@Query("DELETE FROM rss_items WHERE publishedAt < :threshold")
suspend fun deleteOldItems(threshold: Long)
```

## 5.2 RSS fetch (conditional GET)

Excerpt (ETag/Last-Modified):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/data/rss/RssFeedClient.kt
val state = rssDao.getFeedState(url)
val requestBuilder = Request.Builder().url(url)

state?.etag?.let { requestBuilder.header("If-None-Match", it) }
state?.lastModified?.let { requestBuilder.header("If-Modified-Since", it) }

okHttpClient.newCall(request).execute().use { response ->
    if (response.code == 304) {
        state?.let {
            rssDao.insertFeedState(it.copy(lastFetchedAt = System.currentTimeMillis()))
        }
        return@withContext
    }
    ...
}
```

## 5.3 RSS parsing

Excerpt (RSS 2.0 + Atom basics, timestamp fallbacks):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/data/rss/RssParser.kt
private val dateFormats = listOf(
    SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
    SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
)

val finalGuid = guid.ifEmpty { link }
return RssItemEntity(
    guidHash = hashString(finalGuid),
    feedUrl = feedUrl,
    title = title,
    link = link,
    publishedAt = if (pubDate == 0L) System.currentTimeMillis() else pubDate,
    snippet = stripHtml(description),
    fetchedAt = System.currentTimeMillis()
)
```

## 5.4 Digest building + ticker matching

Excerpt (matching and limits):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/BuildRssDigestUseCase.kt
val recentItems = rssDao.getAllRecentItems(since)

val matches = recentItems.filter { item ->
    isMatch(item.title, ticker) || isMatch(item.snippet, ticker)
}
.distinctBy { it.guidHash }
.sortedByDescending { it.publishedAt }
.take(maxItemsPerTicker)

private fun isMatch(text: String, ticker: String): Boolean {
    val cashtag = "\\$$ticker\\b".toRegex(RegexOption.IGNORE_CASE)
    val bareTicker = "\\b$ticker\\b".toRegex(RegexOption.IGNORE_CASE)
    return cashtag.containsMatchIn(text) || bareTicker.containsMatchIn(text)
}
```

## 5.5 Default feeds

Excerpt (default feed list used by V2 and Deep Dive):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/ui/AnalysisViewModel.kt
private val DEFAULT_RSS_FEEDS = listOf(
    "https://finance.yahoo.com/rss/",
    "http://feeds.marketwatch.com/marketwatch/topstories/",
    "https://www.benzinga.com/feeds/news"
)
```

---

# 6) Deep Dive (user-triggered)

## 6.1 Prompt template

Excerpt:

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/domain/ai/AiPrompts.kt
const val DEEP_DIVE_PROMPT = """
    You are a senior equity analyst performing a deep dive on {symbol}.
    ...
    Constraints:
    - Perform at most 3 web searches.
    - Look for information from the last 72 hours only.
    - Return ONLY a JSON object matching the schema below.
    - List all sources you used in the "sources" array.
    ...
"""
```

## 6.2 Deep Dive use case (StageModelRouter path)

Excerpt:

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/DeepDiveUseCase.kt
val request = LlmStageRequest(
    systemPrompt = AiPrompts.SYSTEM_ANALYST,
    userPrompt = prompt,
    stage = AnalysisStage.DEEP_DIVE,
    expectedSchemaId = "DeepDive",
    timeoutMs = timeoutMs,
    maxOutputTokens = 2000
)

val response = stageModelRouter.run(AnalysisStage.DEEP_DIVE, request)
val json = response.parsedJson ?: extractJson(response.rawText)

val deepDive = DeepDive.fromJson(json)

val groundingSources = response.sources.map { source ->
    DeepDiveSource(
        title = source.title,
        url = source.url,
        publisher = "",
        publishedAt = ""
    )
}

val combinedSources = (groundingSources + deepDive.sources).distinctBy { it.url }
return deepDive.copy(sources = combinedSources)
```

## 6.3 Deep Dive JSON parsing (authoritative schema)

Excerpt:

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/domain/model/DeepDive.kt
DeepDive(
    summary = obj.optString("summary", ""),
    drivers = driversList,
    risks = obj.optJSONArray("risks").toStringList(),
    whatChangesMyMind = obj.optJSONArray("what_changes_my_mind").toStringList(),
    sources = sourcesList
)
```

## 6.4 UI entry point

Excerpt (user-triggered only):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/ui/AnalysisViewModel.kt
fun requestDeepDive(symbol: String) {
    val llmKey = state.keys.llmKey
    if (llmKey.isBlank() || !state.hasLlmKey) return

    updateDeepDive(symbol, DeepDiveState(status = DeepDiveStatus.LOADING))
    viewModelScope.launch(ioDispatcher) { ... deepDiveUseCase.execute(...) ... }
}
```

---

# 7) JSON parsing helpers

## 7.1 Shared JSON extraction

Excerpt:

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/util/JsonExtraction.kt
fun extractFirstJsonObject(text: String): String? {
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    if (start == -1 || end == -1 || end <= start) return null
    return text.substring(start, end + 1)
}
```

## 7.2 SynthesizeSetupUseCase (legacy parsing)

Excerpt (uses a local extractJson):

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/SynthesizeSetupUseCase.kt
private fun extractJson(text: String): String? {
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    if (start == -1 || end == -1 || end <= start) return null
    return text.substring(start, end + 1)
}
```

---

# 8) Settings and routing persistence

- `useStagedPipeline` and model routing live in app settings and are persisted via `AppSettingsStore`.

Excerpt:

```kotlin
// app/src/main/java/com/polaralias/signalsynthesis/data/storage/AppSettingsStore.kt
useStagedPipeline = prefs.getBoolean(KEY_USE_STAGED, false),
modelRouting = UserModelRoutingConfig.fromJson(prefs.getString(KEY_MODEL_ROUTING, null)),
```

---

# 9) Known gaps, mismatches, and TODOs (as of 2026-01-30)

These are important for validation — the agent should highlight them if they remain.

1) Decision update stage is not wired into `RunAnalysisV2UseCase`.
   - The models exist (`DecisionUpdate.kt`), but there is no `UpdateDecisionsUseCase` and no call after enrichment.

2) Fundamentals + news synthesis stage is not implemented.
   - The model exists (`FundamentalsNewsSynthesis.kt`), but no `SynthesizeFundamentalsAndNewsUseCase` or orchestration step exists.

3) `RunAnalysisV2UseCase.execute` accepts `llmKey`, but never uses it.
   - The actual key is pulled from `StageModelRouter` via `_uiState.value.keys.llmKey` in the ViewModel.

4) `DeepDiveProvider` + `OpenAiDeepDiveProvider` + `GeminiDeepDiveProvider` are not used by `DeepDiveUseCase`.
   - `DeepDiveUseCase` routes directly through `StageModelRouter`, so the provider classes appear orphaned.

5) Gemini Deep Dive tooling requires explicit `ToolsMode.GOOGLE_SEARCH`.
   - Defaults in `StageModelConfig` use `ToolsMode.WEB_SEARCH`, so a Gemini deep dive will not include tools unless the routing config is updated.

---

# 10) Validation checklist (agentic pass)

Use this checklist to validate implementation behavior against the reference above.

## 10.1 Staged pipeline behavior

- Confirm `useStagedPipeline` toggles between V1 and V2 in `AnalysisViewModel.runAnalysis`.
- Confirm staged pipeline requires non-empty LLM key before execution.
- Confirm shortlist reduces enrichment scope:
  - In `RunAnalysisV2UseCase`, ensure enrichment calls only reference `shortlistedSymbols` or filtered subsets.

## 10.2 LLM request constraints

- Confirm `StageModelRouter` enforces `ToolsMode.NONE` for all stages except `DEEP_DIVE`.
- Confirm OpenAI JSON enforcement:
  - `OpenAiStageRunner.runStandard` uses `responseFormat = json_object` when `expectedSchemaId` is set.

## 10.3 Shortlist JSON integrity

- Confirm `ShortlistPlan.fromJson` defaults are safe and do not crash on malformed JSON.
- Confirm `requested_enrichment` is respected in `RunAnalysisV2UseCase`.

## 10.4 RSS ingestion

- Confirm ETag / Last-Modified are used, 304 responses update `lastFetchedAt`.
- Confirm digest respects `timeWindowHours` and `maxItemsPerTicker`.
- Confirm ticker matching handles cashtags and bare tickers with word boundaries.

## 10.5 Deep Dive

- Confirm `requestDeepDive` only runs when user triggers it and LLM key exists.
- Confirm `DeepDiveUseCase` merges grounding sources with JSON sources.
- Confirm Response tool sources are mapped correctly (`OpenAiStageRunner` for web search).
- Confirm Gemini uses `ToolsMode.GOOGLE_SEARCH` when configured.

## 10.6 Known gaps validation

- Flag missing `UpdateDecisionsUseCase` and `SynthesizeFundamentalsAndNewsUseCase`.
- Flag unused `DeepDiveProvider` implementations if still unused.
- Flag unused `llmKey` parameter in `RunAnalysisV2UseCase` if unchanged.

---

# 11) Suggested agentic validation prompt (copy/paste)

"""
Validate the staged analysis pipeline and Deep Dive implementation against `docs/refactor-implementation-plan.md`. 
1) Confirm code matches the cited excerpts in V1/V2, StageModelRouter, and RSS ingestion. 
2) Verify Deep Dive tool usage and source merging. 
3) Identify any deviations from the documented flow or missing TODOs. 
4) Produce a short list of concrete fixes with file paths.
"""

---

# Appendix: Data contracts (reference)

These are the JSON keys consumed by parsing logic and must remain stable.

- Shortlist: `shortlist`, `global_notes`, `limits_applied`, `requested_enrichment`, `risk_flags`.
- Decision update: `keep`, `drop`, `setup_bias`, `must_review`, `rss_needed`.
- RSS digest: map of `symbol -> [title, link, published_at, snippet]`.
- Fundamentals/news synthesis: `ranked_review_list`, `portfolio_guidance`.
- Deep dive: `summary`, `drivers`, `risks`, `what_changes_my_mind`, `sources`.
