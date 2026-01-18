Agent Prompts for Missing Functionality (Ordered by Priority)

Below are detailed, actionable prompts for implementing all identified gaps from the QA analysis, ordered to maximize stability and debuggability before adding complex new features.

1. Execute and Verify Test Suite
Prompt: Verify All Unit Tests Pass
code
Markdown
download
content_copy
expand_less
## Task: Execute and Fix Unit Test Suite

### Context
The Signal Synthesis Android app has a comprehensive test suite that has never been executed in a proper Android development environment. The tests exist but their pass/fail status is unknown. 

### Objective
Execute all unit tests and fix any failures to ensure the test suite passes completely.

### Files to Review
- `app/src/test/java/com/polaralias/signalsynthesis/domain/indicators/VwapIndicatorTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/indicators/RsiIndicatorTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/indicators/AtrIndicatorTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/indicators/SmaIndicatorTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/RankSetupsUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/FilterTradeableUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/DiscoverCandidatesUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/EnrichIntradayUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/EnrichEodUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/domain/usecase/RunAnalysisUseCaseTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/data/cache/TimedCacheTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/data/repository/MarketDataRepositoryTest.kt`
- `app/src/test/java/com/polaralias/signalsynthesis/ui/AnalysisViewModelTest.kt`

### Steps
1. Ensure `local.properties` has a valid `sdk.dir` path
2. Run `./gradlew testDebugUnitTest --info`
3. Capture any test failures with full stack traces
4. For each failing test: 
   - Analyze the root cause (test bug vs implementation bug)
   - Fix the appropriate code
   - Re-run to confirm the fix
5. Ensure all tests pass with zero failures

### Acceptance Criteria
- `./gradlew testDebugUnitTest` exits with code 0
- All tests pass (no failures, no errors)
- Test report shows 100% pass rate
- Update `docs/implementation/implementation_log.md` with verification entry

### Output
Provide a summary of: 
- Total tests executed
- Any tests that required fixes (and what was fixed)
- Final pass/fail counts
2. Error Logging and Crash Reporting
Prompt: Add Comprehensive Error Logging
code
Markdown
download
content_copy
expand_less
## Task: Implement Error Logging Throughout the App

### Context
Currently, many catch blocks silently swallow exceptions without logging. This makes debugging production issues difficult.

### Objective
Add comprehensive logging throughout the codebase and optionally integrate crash reporting.

### Implementation

#### Logging Wrapper
Create `app/src/main/java/com/polaralias/signalsynthesis/util/Logger.kt`:
```kotlin
object Logger {
    private const val TAG = "SignalSynthesis"
    
    fun d(tag: String, message:  String) {
        Log.d("$TAG: $tag", message)
    }
    
    fun i(tag: String, message: String) {
        Log.i("$TAG:$tag", message)
    }
    
    fun w(tag: String, message:  String, throwable: Throwable?  = null) {
        if (throwable != null) {
            Log.w("$TAG:$tag", message, throwable)
        } else {
            Log.w("$TAG:$tag", message)
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable?  = null) {
        if (throwable != null) {
            Log.e("$TAG:$tag", message, throwable)
            // Report to crash reporting service if configured
            CrashReporter.recordException(throwable)
        } else {
            Log.e("$TAG:$tag", message)
        }
    }
    
    // Structured logging for analytics
    fun event(name: String, params: Map<String, Any> = emptyMap()) {
        Log. d("$TAG:Event", "$name: $params")
        // Could send to analytics service
    }
}
Crash Reporter Interface

Create app/src/main/java/com/polaralias/signalsynthesis/util/CrashReporter.kt:

code
Kotlin
download
content_copy
expand_less
object CrashReporter {
    private var isEnabled = false
    
    fun init(context: Context, enabled: Boolean) {
        isEnabled = enabled
        if (enabled) {
            // Initialize Firebase Crashlytics or similar
            // FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        }
    }
    
    fun recordException(throwable: Throwable) {
        if (! isEnabled) return
        // FirebaseCrashlytics.getInstance().recordException(throwable)
        Log.e("CrashReporter", "Exception recorded", throwable)
    }
    
    fun log(message: String) {
        if (! isEnabled) return
        // FirebaseCrashlytics.getInstance().log(message)
    }
    
    fun setUserId(userId: String) {
        if (!isEnabled) return
        // FirebaseCrashlytics.getInstance().setUserId(userId)
    }
}
Update All Providers

Example for FinnhubMarketDataProvider:

code
Kotlin
download
content_copy
expand_less
override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
    Logger.d("Finnhub", "Fetching quotes for ${symbols.size} symbols")
    
    return try {
        val results = symbols.mapNotNull { symbol ->
            try {
                val response = service.getQuote(symbol, apiKey)
                response.toQuote(symbol)?.let { symbol to it }
            } catch (e: Exception) {
                Logger.w("Finnhub", "Failed to fetch quote for $symbol", e)
                null
            }
        }.toMap()
        
        Logger. i("Finnhub", "Successfully fetched ${results. size}/${symbols.size} quotes")
        results
    } catch (e: Exception) {
        Logger.e("Finnhub", "Failed to fetch quotes", e)
        emptyMap()
    }
}
Update Repository
code
Kotlin
download
content_copy
expand_less
class MarketDataRepository(private val providers: ProviderBundle) {
    
    suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
        Logger.d("Repository", "getQuotes called for ${symbols.size} symbols")
        
        // Check cache first
        val cacheKey = symbols.sorted().joinToString(",")
        val cached = quoteCache.get(cacheKey)
        if (cached != null) {
            Logger. d("Repository", "Cache hit for quotes")
            return cached
        }
        
        // Try providers in order
        for (provider in providers.quoteProviders) {
            val providerName = provider:: class.simpleName ?: "Unknown"
            Logger. d("Repository", "Trying $providerName for quotes")
            
            try {
                val result = provider.getQuotes(symbols)
                if (result.isNotEmpty()) {
                    Logger.i("Repository", "$providerName returned ${result.size} quotes")
                    quoteCache.put(cacheKey, result)
                    return result
                }
                Logger.w("Repository", "$providerName returned empty results")
            } catch (e: Exception) {
                Logger. w("Repository", "$providerName failed", e)
            }
        }
        
        Logger.w("Repository", "All providers failed for quotes")
        return emptyMap()
    }
}
Update ViewModel
code
Kotlin
download
content_copy
expand_less
fun runAnalysis() {
    Logger.event("analysis_started", mapOf("intent" to _uiState.value.intent. name))
    
    viewModelScope. launch(ioDispatcher) {
        try {
            val result = useCase. execute(_uiState.value.intent)
            Logger.event("analysis_completed", mapOf(
                "intent" to result.intent.name,
                "candidates" to result.totalCandidates,
                "setups" to result. setupCount
            ))
            // ... update UI state
        } catch (e: Exception) {
            Logger.e("ViewModel", "Analysis failed", e)
            // ... update error state
        }
    }
}
Acceptance Criteria

All provider calls have try-catch with logging

Repository logs cache hits/misses and fallback decisions

ViewModel logs user actions and results

Exceptions include stack traces in logs

CrashReporter interface ready for production integration

No silent catch blocks remain

Update implementation log

code
Code
download
content_copy
expand_less
---

## 3. Provider Retry Logic

### Prompt: Add Retry Logic for Provider API Calls

```markdown
## Task: Implement Retry Logic for Provider API Failures

### Context
Currently, when a provider API call fails, the repository immediately falls back to the next provider. There's no retry mechanism for transient failures (network hiccups, rate limits, etc.).

### Objective
Add configurable retry logic with exponential backoff for provider API calls. 

### Implementation

#### Retry Configuration
Create `app/src/main/java/com/polaralias/signalsynthesis/data/provider/RetryConfig.kt`:
```kotlin
data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 10000,
    val multiplier: Double = 2.0,
    val retryableExceptions: Set<Class<out Throwable>> = setOf(
        java.net.SocketTimeoutException:: class.java,
        java.net.UnknownHostException:: class.java,
        java.io.IOException::class.java
    )
)
Retry Utility

Create app/src/main/java/com/polaralias/signalsynthesis/data/provider/RetryHelper.kt:

code
Kotlin
download
content_copy
expand_less
object RetryHelper {
    private val defaultConfig = RetryConfig()
    
    suspend fun <T> withRetry(
        config: RetryConfig = defaultConfig,
        block: suspend () -> T
    ): T {
        var currentDelay = config.initialDelayMs
        var lastException: Throwable? = null
        
        repeat(config.maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Throwable) {
                lastException = e
                
                // Check if exception is retryable
                val isRetryable = config.retryableExceptions.any { 
                    it.isInstance(e) 
                }
                
                if (! isRetryable || attempt == config.maxRetries - 1) {
                    throw e
                }
                
                // Log retry attempt
                Log. w("RetryHelper", "Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms", e)
                
                delay(currentDelay)
                currentDelay = (currentDelay * config.multiplier)
                    .toLong()
                    .coerceAtMost(config.maxDelayMs)
            }
        }
        
        throw lastException ?: IllegalStateException("Retry failed without exception")
    }
}
Apply to Providers

Update each provider to use retry logic. Example for FinnhubMarketDataProvider:

code
Kotlin
download
content_copy
expand_less
class FinnhubMarketDataProvider(
    private val apiKey:  String,
    private val retryConfig: RetryConfig = RetryConfig(),
    clock: Clock = Clock. systemUTC()
) : QuoteProvider, IntradayProvider, DailyProvider, ProfileProvider, MetricsProvider, SentimentProvider {
    
    private val service = FinnhubService. create()
    
    override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
        return RetryHelper.withRetry(retryConfig) {
            symbols.mapNotNull { symbol ->
                try {
                    val response = service.getQuote(symbol, apiKey)
                    response.toQuote(symbol)?.let { symbol to it }
                } catch (e: Exception) {
                    null
                }
            }. toMap()
        }
    }
    
    // Apply similarly to other methods...
}
Rate Limit Handling

Add HTTP 429 handling:

code
Kotlin
download
content_copy
expand_less
data class RetryConfig(
    // ... existing
    val retryOn429: Boolean = true,
    val rateLimitDelayMs: Long = 60000 // Wait 1 minute on rate limit
)

// In RetryHelper
if (e is HttpException && e.code() == 429 && config.retryOn429) {
    Log.w("RetryHelper", "Rate limited, waiting ${config.rateLimitDelayMs}ms")
    delay(config.rateLimitDelayMs)
    // Don't count this as a retry attempt
    continue
}
Logging

Add logging for observability:

code
Kotlin
download
content_copy
expand_less
class ProviderLogger {
    fun logRequest(provider: String, endpoint: String, symbol: String) {
        Log. d("Provider", "[$provider] Request: $endpoint for $symbol")
    }
    
    fun logSuccess(provider: String, endpoint: String, durationMs: Long) {
        Log. d("Provider", "[$provider] Success:  $endpoint in ${durationMs}ms")
    }
    
    fun logError(provider: String, endpoint: String, error: Throwable) {
        Log. e("Provider", "[$provider] Error:  $endpoint - ${error.message}", error)
    }
    
    fun logFallback(fromProvider: String, toProvider: String, reason: String) {
        Log.w("Provider", "Fallback:  $fromProvider -> $toProvider ($reason)")
    }
}
Acceptance Criteria

Transient failures retry up to 3 times with exponential backoff

Rate limit (429) responses wait and retry

Non-retryable errors fail fast

Retry attempts are logged for debugging

Retry config is injectable for testing

Add unit tests for retry logic

Update implementation log

code
Code
download
content_copy
expand_less
---

## 4. Compose UI Tests

### Prompt: Implement Comprehensive UI Tests

```markdown
## Task: Add Jetpack Compose UI Tests

### Context
The Signal Synthesis app uses Jetpack Compose for its UI layer but has no UI tests.  The implementation plan (Phase 8) requires "UI tests for ViewModel state" to verify screen rendering and state binding.

### Objective
Implement UI tests for all Compose screens to verify correct rendering and state handling.

### Prerequisites
Add test dependencies to `app/build.gradle.kts`:
```kotlin
androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
Tests to Implement
1. AnalysisScreenTest.kt

Location: app/src/androidTest/java/com/polaralias/signalsynthesis/ui/AnalysisScreenTest.kt

Test cases:

Screen displays all three intent chips (Day Trade, Swing, Long Term)

Selecting an intent chip updates visual selection state

"Configure Keys" button is visible and clickable

"Run Analysis" button shows loading indicator when isLoading = true

Error message displays when errorMessage is not null

Error message dismisses when acknowledged

"No API keys" warning shows when hasAnyApiKeys = false

2. ResultsScreenTest.kt

Location: app/src/androidTest/java/com/polaralias/signalsynthesis/ui/ResultsScreenTest.kt

Test cases:

Empty state shows "Run analysis to see results" when result = null

Results list renders correct number of setup cards

Each card displays symbol, confidence, setup type

Tapping a card triggers onOpenDetail callback

AI summary placeholder shows correct text based on hasLlmKey state

3. SetupDetailScreenTest.kt

Location: app/src/androidTest/java/com/polaralias/signalsynthesis/ui/SetupDetailScreenTest.kt

Test cases:

Screen displays symbol in top bar

Price levels (trigger, stop, target) are visible

AI synthesis loading state shows spinner/text

AI synthesis ready state shows summary, risks, verdict

AI synthesis error state shows error message

"Show Raw Data" toggle switches between AI and raw views

Back button triggers onBack callback

4. ApiKeysScreenTest.kt

Location: app/src/androidTest/java/com/polaralias/signalsynthesis/ui/ApiKeysScreenTest.kt

Test cases:

All key input fields are present (Alpaca Key, Alpaca Secret, Polygon, Finnhub, FMP, LLM)

Input fields update state via onFieldChanged

Save button triggers onSave callback

Back navigation works correctly

5. SettingsScreenTest.kt

Location: app/src/androidTest/java/com/polaralias/signalsynthesis/ui/SettingsScreenTest.kt

Test cases:

Alert toggle switch reflects alertsEnabled state

Toggle interaction calls onToggleAlerts

"Edit Keys" button triggers navigation

"Clear Keys" button triggers onClearKeys

Monitored symbol count displays correctly

Implementation Pattern
code
Kotlin
download
content_copy
expand_less
@RunWith(AndroidJUnit4::class)
class AnalysisScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun intentChipsAreDisplayed() {
        composeTestRule.setContent {
            AnalysisScreen(
                uiState = AnalysisUiState(),
                onIntentSelected = {},
                onRunAnalysis = {},
                onOpenKeys = {},
                onOpenResults = {},
                onOpenSettings = {},
                onDismissError = {}
            )
        }
        
        composeTestRule.onNodeWithText("Day Trade").assertIsDisplayed()
        composeTestRule.onNodeWithText("Swing").assertIsDisplayed()
        composeTestRule.onNodeWithText("Long Term").assertIsDisplayed()
    }
}
Acceptance Criteria

All 5 test files created with comprehensive test cases

Tests run successfully via ./gradlew connectedDebugAndroidTest

Minimum 20 UI test cases total

All tests pass on API 24+ emulator

Update docs/implementation/implementation_log.md with testing entry

code
Code
download
content_copy
expand_less
---

## 5. Settings Enhancements

### Prompt: Add Configurable Settings for Refresh Interval and Cache

```markdown
## Task: Implement Configurable Settings for Refresh and Cache

### Context
The implementation guide mentions settings for "Configure refresh interval" and "Clear cached data" but these are not currently implemented.  Users should be able to control how often data refreshes and manually clear cached data. 

### Objective
Add configurable settings for: 
1. Analysis refresh interval
2. Alert check frequency
3. Cache TTL settings
4. Manual cache clear option

### Implementation

#### Settings Model
Create/update `app/src/main/java/com/polaralias/signalsynthesis/data/settings/AppSettings.kt`:
```kotlin
data class AppSettings(
    // Refresh intervals
    val quoteRefreshIntervalMinutes: Int = 5,
    val alertCheckIntervalMinutes: Int = 15,
    
    // Cache TTLs (in minutes)
    val quoteCacheTtlMinutes: Int = 1,
    val intradayCacheTtlMinutes: Int = 5,
    val dailyCacheTtlMinutes: Int = 60,
    val profileCacheTtlMinutes: Int = 1440, // 24 hours
    
    // Feature toggles
    val useMockDataWhenOffline: Boolean = true,
    val prefetchAiSummaries: Boolean = false
)
Settings Storage

Create app/src/main/java/com/polaralias/signalsynthesis/data/storage/AppSettingsStore.kt:

code
Kotlin
download
content_copy
expand_less
interface AppSettingsStorage {
    suspend fun loadSettings(): AppSettings
    suspend fun saveSettings(settings: AppSettings)
}

class AppSettingsStore(context: Context) : AppSettingsStorage {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    override suspend fun loadSettings(): AppSettings = withContext(Dispatchers.IO) {
        AppSettings(
            quoteRefreshIntervalMinutes = prefs.getInt(KEY_QUOTE_REFRESH, 5),
            alertCheckIntervalMinutes = prefs. getInt(KEY_ALERT_INTERVAL, 15),
            quoteCacheTtlMinutes = prefs.getInt(KEY_QUOTE_CACHE_TTL, 1),
            // ... other fields
        )
    }
    
    override suspend fun saveSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        prefs.edit {
            putInt(KEY_QUOTE_REFRESH, settings.quoteRefreshIntervalMinutes)
            putInt(KEY_ALERT_INTERVAL, settings.alertCheckIntervalMinutes)
            // ... other fields
        }
    }
    
    companion object {
        private const val KEY_QUOTE_REFRESH = "quote_refresh_interval"
        private const val KEY_ALERT_INTERVAL = "alert_check_interval"
        private const val KEY_QUOTE_CACHE_TTL = "quote_cache_ttl"
        // ... other keys
    }
}
Updated Settings Screen

Modify app/src/main/java/com/polaralias/signalsynthesis/ui/SettingsScreen.kt:

Add new sections:

code
Kotlin
download
content_copy
expand_less
@Composable
fun SettingsScreen(
    uiState: AnalysisUiState,
    appSettings: AppSettings,
    onBack: () -> Unit,
    onEditKeys: () -> Unit,
    onClearKeys: () -> Unit,
    onToggleAlerts: (Boolean) -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    onClearCache: () -> Unit
) {
    // ... existing content
    
    SectionHeader("Data Settings")
    
    // Quote Refresh Interval
    SettingsSlider(
        label = "Quote Refresh Interval",
        value = appSettings.quoteRefreshIntervalMinutes,
        range = 1..30,
        unit = "min",
        onValueChange = { onUpdateSettings(appSettings.copy(quoteRefreshIntervalMinutes = it)) }
    )
    
    // Alert Check Frequency
    SettingsSlider(
        label = "Alert Check Frequency",
        value = appSettings. alertCheckIntervalMinutes,
        range = 15..60,
        unit = "min",
        onValueChange = { 
            onUpdateSettings(appSettings.copy(alertCheckIntervalMinutes = it))
        }
    )
    
    SectionHeader("Cache")
    
    // Cache TTL Info
    Text("Quote cache:  ${appSettings.quoteCacheTtlMinutes} min")
    Text("Intraday cache: ${appSettings. intradayCacheTtlMinutes} min")
    Text("Daily cache:  ${appSettings.dailyCacheTtlMinutes} min")
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Clear Cache Button
    OutlinedButton(
        onClick = onClearCache,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme. error
        )
    ) {
        Text("Clear All Cached Data")
    }
    
    SectionHeader("Advanced")
    
    // Mock data toggle
    SettingsSwitch(
        label = "Use mock data when offline",
        checked = appSettings.useMockDataWhenOffline,
        onCheckedChange = { onUpdateSettings(appSettings. copy(useMockDataWhenOffline = it)) }
    )
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Int,
    range: IntRange,
    unit:  String,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier. fillMaxWidth(),
            horizontalArrangement = Arrangement. SpaceBetween
        ) {
            Text(label)
            Text("$value $unit")
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it. roundToInt()) },
            valueRange = range. first. toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1
        )
    }
}
Wire Cache Clear to Repository

Add to MarketDataRepository.kt:

code
Kotlin
download
content_copy
expand_less
fun clearAllCaches() {
    quoteCache.clear()
    intradayCache.clear()
    dailyCache. clear()
    profileCache.clear()
    metricsCache.clear()
    sentimentCache.clear()
}

Add to TimedCache.kt:

code
Kotlin
download
content_copy
expand_less
@Synchronized
fun clear() {
    cache.clear()
}
ViewModel Updates

Add to AnalysisViewModel:

appSettings: StateFlow<AppSettings>

updateSettings(settings: AppSettings)

clearCache()

Acceptance Criteria

All settings persist across app restarts

Changing alert interval reschedules WorkManager

Cache clear removes all cached data

Settings UI is intuitive with sliders and toggles

Repository uses configurable TTLs

Update implementation log

code
Code
download
content_copy
expand_less
---

## 6. AI Summary Prefetching and Caching

### Prompt: Implement AI Summary Prefetching and Local Caching

```markdown
## Task: Add AI Summary Prefetching and Caching

### Context
Currently, AI summaries are only generated when a user opens the detail view for a specific setup. The implementation log notes:  "Consider prefetching summaries for top results or caching outputs."

### Objective
Implement: 
1. Automatic prefetching of AI summaries for top N results after analysis
2. Local caching of AI summaries to avoid redundant API calls
3. Cache invalidation when setup data changes

### Implementation

#### AI Summary Cache Entity
Add to Room database (if implementing #4) or create file-based cache: 

```kotlin
@Entity(tableName = "ai_summary_cache")
data class AiSummaryCacheEntity(
    @PrimaryKey val symbol: String,
    val setupHash: Int, // Hash of TradeSetup to detect changes
    val summary: String,
    val risksJson: String,
    val verdict: String,
    val cachedAt: Long,
    val expiresAt: Long
)
AI Summary Cache Repository

Create app/src/main/java/com/polaralias/signalsynthesis/data/repository/AiSummaryCacheRepository.kt:

code
Kotlin
download
content_copy
expand_less
class AiSummaryCacheRepository(
    private val dao: AiSummaryCacheDao,
    private val cacheDurationHours: Int = 24
) {
    suspend fun getCachedSummary(setup: TradeSetup): AiSynthesis? {
        val cached = dao.getBySymbol(setup.symbol) ?: return null
        
        // Check if cache is valid
        if (cached.expiresAt < System. currentTimeMillis()) {
            dao.delete(cached)
            return null
        }
        
        // Check if setup has changed
        if (cached.setupHash != setup.hashCode()) {
            dao.delete(cached)
            return null
        }
        
        return AiSynthesis(
            summary = cached.summary,
            risks = Json.decodeFromString(cached.risksJson),
            verdict = cached.verdict
        )
    }
    
    suspend fun cacheSummary(setup: TradeSetup, synthesis: AiSynthesis) {
        val entity = AiSummaryCacheEntity(
            symbol = setup.symbol,
            setupHash = setup. hashCode(),
            summary = synthesis.summary,
            risksJson = Json.encodeToString(synthesis.risks),
            verdict = synthesis.verdict,
            cachedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (cacheDurationHours * 60 * 60 * 1000)
        )
        dao.insert(entity)
    }
    
    suspend fun clearExpired() {
        dao.deleteExpired(System.currentTimeMillis())
    }
    
    suspend fun clearAll() {
        dao.deleteAll()
    }
}
Prefetch Use Case

Create app/src/main/java/com/polaralias/signalsynthesis/domain/usecase/PrefetchAiSummariesUseCase.kt:

code
Kotlin
download
content_copy
expand_less
class PrefetchAiSummariesUseCase(
    private val synthesizeUseCase: SynthesizeSetupUseCase,
    private val cacheRepository: AiSummaryCacheRepository,
    private val maxPrefetch: Int = 3
) {
    suspend fun execute(
        setups: List<TradeSetup>,
        llmKey: String,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Map<String, AiSynthesis> {
        val results = mutableMapOf<String, AiSynthesis>()
        val toPrefetch = setups.take(maxPrefetch)
        
        toPrefetch.forEachIndexed { index, setup ->
            onProgress(index + 1, toPrefetch.size)
            
            // Check cache first
            val cached = cacheRepository.getCachedSummary(setup)
            if (cached != null) {
                results[setup. symbol] = cached
                return@forEachIndexed
            }
            
            // Generate and cache
            try {
                val synthesis = synthesizeUseCase.execute(setup, llmKey)
                cacheRepository.cacheSummary(setup, synthesis)
                results[setup.symbol] = synthesis
            } catch (e: Exception) {
                // Log error but continue with other setups
            }
        }
        
        return results
    }
}
ViewModel Integration

Update AnalysisViewModel:

code
Kotlin
download
content_copy
expand_less
private val prefetchUseCase = PrefetchAiSummariesUseCase(...)

fun runAnalysis() {
    viewModelScope.launch(ioDispatcher) {
        // ...  existing analysis code ...
        
        val result = useCase.execute(_uiState.value.intent)
        
        // Update UI with results
        _uiState.update { it.copy(result = result, isLoading = false) }
        
        // Prefetch AI summaries for top setups (if LLM key available)
        val llmKey = _uiState.value. keys.llmKey
        if (llmKey. isNotBlank() && result.setups.isNotEmpty()) {
            _uiState.update { it.copy(isPrefetchingAi = true) }
            
            val prefetched = prefetchUseCase.execute(
                setups = result.setups,
                llmKey = llmKey,
                onProgress = { current, total ->
                    _uiState.update { 
                        it. copy(prefetchProgress = current to total) 
                    }
                }
            )
            
            // Update AI summaries in state
            prefetched.forEach { (symbol, synthesis) ->
                updateAiSummary(symbol, AiSummaryState(
                    status = AiSummaryStatus.READY,
                    summary = synthesis.summary,
                    risks = synthesis.risks,
                    verdict = synthesis.verdict
                ))
            }
            
            _uiState.update { it.copy(isPrefetchingAi = false) }
        }
    }
}
UI Updates

In ResultsScreen, show prefetch progress:

code
Kotlin
download
content_copy
expand_less
if (uiState. isPrefetchingAi) {
    val (current, total) = uiState.prefetchProgress
    LinearProgressIndicator(
        progress = current. toFloat() / total,
        modifier = Modifier.fillMaxWidth()
    )
    Text("Generating AI insights...  ($current/$total)")
}
Settings Integration

Add to AppSettings:

code
Kotlin
download
content_copy
expand_less
data class AppSettings(
    // ... existing
    val prefetchAiSummaries: Boolean = true,
    val maxAiPrefetch: Int = 3,
    val aiCacheDurationHours: Int = 24
)
Acceptance Criteria

Top 3 setups automatically get AI summaries after analysis

Cached summaries load instantly on detail view

Cache invalidates when setup data changes

Progress indicator shows during prefetch

Settings toggle to enable/disable prefetch

Cache respects TTL and can be manually cleared

Update implementation log

code
Code
download
content_copy
expand_less
---

## 7. Dashboard Screen

### Prompt: Implement Market Dashboard Screen

```markdown
## Task: Add Dashboard/Home Screen with Market Overview

### Context
The product vision document specifies:  "A Dashboard or home screen will likely summarize the current market status (maybe a few major indices or user-selected favorites) and provide an entry point to run the analysis."

Currently, the app opens directly to the Analysis screen. A dashboard would provide a better user experience. 

### Objective
Create a Dashboard screen that serves as the app's home screen, showing market overview and quick access to analysis. 

### Requirements

#### Data Model
Create `app/src/main/java/com/polaralias/signalsynthesis/domain/model/MarketOverview.kt`:
```kotlin
data class MarketOverview(
    val indices: List<IndexQuote>,
    val lastUpdated: Instant
)

data class IndexQuote(
    val symbol: String,
    val name: String,
    val price: Double,
    val changePercent: Double
)
Dashboard UI State

Add to AnalysisUiState.kt or create separate state:

code
Kotlin
download
content_copy
expand_less
data class DashboardState(
    val marketOverview: MarketOverview? = null,
    val isLoadingMarket: Boolean = false,
    val recentAnalysis: AnalysisResult?  = null,
    val topSetups: List<TradeSetup> = emptyList() // Top 3 from last run
)
Screen Implementation

Create app/src/main/java/com/polaralias/signalsynthesis/ui/DashboardScreen.kt:

Features:

Market Indices Section

Display SPY, QQQ, DIA with price and % change

Color code: green for positive, red for negative

Show last updated timestamp

Pull-to-refresh to update quotes

Quick Analysis Section

Three large buttons for each TradingIntent

Tapping navigates to Analysis screen with intent pre-selected

Recent Results Section (if available)

Show top 3 setups from last analysis

Each card shows symbol, confidence, AI summary snippet

"View All" button navigates to Results screen

Alerts Status

Small indicator showing alerts enabled/disabled

Count of monitored symbols

Navigation Update

Modify SignalSynthesisApp.kt:

Add Screen.Dashboard as the start destination

Add route for dashboard

Update navigation graph

ViewModel Updates

Add to AnalysisViewModel.kt:

loadMarketOverview() function to fetch index quotes

Store dashboard state

Auto-refresh market data on resume

UI Layout
code
Code
download
content_copy
expand_less
┌─────────────────────────────────┐
│  Signal Synthesis    [Settings] │
├─────────────────────────────────┤
│  MARKET OVERVIEW                │
│  ┌─────┐ ┌─────┐ ┌─────┐       │
│  │ SPY │ │ QQQ │ │ DIA │       │
│  │$450 │ │$380 │ │$350 │       │
│  │+1. 2%│ │-0.5%│ │+0.8%│       │
│  └─────┘ └─────┘ └─────┘       │
│  Updated: 2 min ago            │
├─────────────────────────────────┤
│  RUN ANALYSIS                   │
│  ┌───────────────────────────┐ │
│  │      Day Trade            │ │
│  └───────────────────────────┘ │
│  ┌───────────────────────────┐ │
│  │      Swing Trade          │ │
│  └───────────────────────────┘ │
│  ┌───────────────────────────┐ │
│  │      Long Term            │ │
│  └───────────────────────────┘ │
├─────────────────────────────────┤
│  RECENT SETUPS         View All│
│  ┌─────────────────────────────┐│
│  │ AAPL - High Probability    ││
│  │ Strong momentum...          ││
│  └─────────────────────────────┘│
│  🔔 Alerts:  ON (5 symbols)     │
└─────────────────────────────────┘
Acceptance Criteria

Dashboard is the new start destination

Market indices display with real-time quotes

Quick analysis buttons navigate correctly with pre-selected intent

Recent setups show if analysis was run previously

Pull-to-refresh updates market data

Smooth navigation to all other screens

Update implementation log

code
Code
download
content_copy
expand_less
---

## 8. Watchlist Persistence with Room Database

### Prompt: Implement Local Database for Watchlists and History

```markdown
## Task: Add Room Database for Watchlists and Analysis History

### Context
The implementation guide mentions: "optional local database for results and watchlists." Currently, analysis results are only held in memory and watchlist symbols are stored in SharedPreferences.  A proper database would enable: 
- Persistent watchlists
- Analysis history with timestamps
- Offline access to previous results

### Objective
Implement Room database for persisting watchlists and analysis history. 

### Dependencies
Add to `app/build.gradle.kts`:
```kotlin
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx: 2.6.1")
kapt("androidx.room:room-compiler: 2.6.1")

Add kapt plugin:

code
Kotlin
download
content_copy
expand_less
plugins {
    id("kotlin-kapt")
}
Database Schema
Entities

WatchlistItem.kt (data/db/entity/):

code
Kotlin
download
content_copy
expand_less
@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val symbol: String,
    val addedAt: Long = System.currentTimeMillis(),
    val notes: String? = null
)

AnalysisHistoryEntity.kt (data/db/entity/):

code
Kotlin
download
content_copy
expand_less
@Entity(tableName = "analysis_history")
data class AnalysisHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val intent: String, // TradingIntent.name
    val totalCandidates: Int,
    val tradeableCount: Int,
    val setupCount: Int,
    val generatedAt: Long,
    val setupsJson: String // JSON serialized list of TradeSetup
)

SavedSetupEntity.kt (data/db/entity/):

code
Kotlin
download
content_copy
expand_less
@Entity(
    tableName = "saved_setups",
    foreignKeys = [ForeignKey(
        entity = AnalysisHistoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["analysisId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SavedSetupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val analysisId: Long,
    val symbol:  String,
    val setupType: String,
    val triggerPrice: Double,
    val stopLoss: Double,
    val targetPrice: Double,
    val confidence: Double,
    val reasonsJson: String,
    val validUntil: Long,
    val intent: String
)
DAOs

WatchlistDao.kt (data/db/dao/):

code
Kotlin
download
content_copy
expand_less
@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAll(): Flow<List<WatchlistItem>>
    
    @Query("SELECT symbol FROM watchlist")
    suspend fun getAllSymbols(): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistItem)
    
    @Delete
    suspend fun delete(item: WatchlistItem)
    
    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun deleteBySymbol(symbol:  String)
    
    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE symbol = :symbol)")
    suspend fun exists(symbol: String): Boolean
}

AnalysisHistoryDao.kt (data/db/dao/):

code
Kotlin
download
content_copy
expand_less
@Dao
interface AnalysisHistoryDao {
    @Query("SELECT * FROM analysis_history ORDER BY generatedAt DESC LIMIT : limit")
    fun getRecent(limit: Int = 10): Flow<List<AnalysisHistoryEntity>>
    
    @Query("SELECT * FROM analysis_history WHERE id = :id")
    suspend fun getById(id: Long): AnalysisHistoryEntity?
    
    @Insert
    suspend fun insert(history: AnalysisHistoryEntity): Long
    
    @Query("DELETE FROM analysis_history WHERE generatedAt < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
}
Database

SignalSynthesisDatabase.kt (data/db/):

code
Kotlin
download
content_copy
expand_less
@Database(
    entities = [WatchlistItem::class, AnalysisHistoryEntity::class, SavedSetupEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SignalSynthesisDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun analysisHistoryDao(): AnalysisHistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: SignalSynthesisDatabase? = null
        
        fun getInstance(context: Context): SignalSynthesisDatabase {
            return INSTANCE ?:  synchronized(this) {
                Room.databaseBuilder(
                    context. applicationContext,
                    SignalSynthesisDatabase::class.java,
                    "signal_synthesis. db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
Repository Layer

WatchlistRepository.kt (data/repository/):

code
Kotlin
download
content_copy
expand_less
class WatchlistRepository(private val dao: WatchlistDao) {
    val watchlist: Flow<List<WatchlistItem>> = dao.getAll()
    
    suspend fun addToWatchlist(symbol: String, notes: String?  = null) {
        dao.insert(WatchlistItem(symbol = symbol, notes = notes))
    }
    
    suspend fun removeFromWatchlist(symbol: String) {
        dao.deleteBySymbol(symbol)
    }
    
    suspend fun isInWatchlist(symbol: String): Boolean = dao.exists(symbol)
    
    suspend fun getWatchlistSymbols(): List<String> = dao.getAllSymbols()
}
UI Integration

Add to Watchlist Button: In SetupDetailScreen, add a heart/star icon to add/remove from watchlist

Watchlist Screen: New screen showing all watchlist items with swipe-to-delete

History Screen: Show past analysis runs with ability to view old results

Acceptance Criteria

Room database created and migrates properly

Watchlist CRUD operations work

Analysis history persists across app restarts

DiscoverCandidatesUseCase can optionally include watchlist symbols

AlertSettingsStore migrated to use database for symbols

Unit tests for DAOs

Update implementation log

code
Code
download
content_copy
expand_less
---

## Summary Checklist

| # | Task | Priority | Complexity |
|---|------|----------|------------|
| 1 | Execute and Verify Test Suite | 🔴 Critical | Low |
| 2 | Error Logging & Crash Reporting | 🟡 High | Low |
| 3 | Provider Retry Logic | 🟡 High | Low |
| 4 | Compose UI Tests | 🟡 High | Medium |
| 5 | Settings Enhancements | 🟢 Medium | Low |
| 6 | AI Summary Caching | 🟢 Medium | Medium |
| 7 | Dashboard Screen | 🟢 Medium | Medium |
| 8 | Room Database for Watchlists | 🟢 Medium | High |