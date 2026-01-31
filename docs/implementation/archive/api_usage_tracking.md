# API Usage Tracking & Logging Enhancements

## Overview
Comprehensive improvements to API usage tracking, logging, and UI display to provide users with detailed insights into how API calls are being consumed across different providers and operation types.

## Key Features Implemented

### 1. **Categorized API Usage Tracking**

#### New Usage Categories
API calls are now categorized by operation type:
- **Discovery** - Finding tickers (screener, gainers, losers, actives)
- **Analysis** - Reviewing trends (quotes, intraday, daily data)
- **Fundamentals** - Company data (profile, metrics, sentiment)
- **Alerts** - Background alert checks
- **Search** - Ticker search operations
- **Other** - Fallback for uncategorized operations

#### Automatic Categorization
The system automatically categorizes each API call based on the operation name:
```kotlin
enum class ApiUsageCategory {
    DISCOVERY, ANALYSIS, FUNDAMENTALS, ALERTS, SEARCH, OTHER
    
    companion object {
        fun fromOperation(operation: String): ApiUsageCategory {
            // Intelligent categorization based on operation name
        }
    }
}
```

### 2. **Daily Usage Tracking with Archiving**

#### Changed from Monthly to Daily Tracking
- **Previous**: Monthly aggregated counts per provider
- **New**: Daily detailed counts per provider AND category
- **Benefit**: More granular insights into daily usage patterns

#### Automatic Daily Archiving
- Usage automatically archives at midnight (day rollover)
- Archives stored for up to 30 days
- Old archives automatically cleaned up
- Archived data retrievable for auditing

#### Manual Archive Function
Users can manually archive current day's usage:
```kotlin
fun manualArchive() {
    // Archives current day and resets counter
    // Useful for tracking multiple analysis sessions per day
}
```

### 3. **Enhanced UI Display**

#### Settings Screen - Usage Status Section
**Before:**
```
Total Monthly API Requests: 150
Provider1: 75
Provider2: 75
```

**After:**
```
Total API Requests Today: 45

[Card: Provider1: 28 calls]
  Finding Tickers: 10
  Reviewing Trends: 15
  Company Data: 3

[Card: Provider2: 17 calls]
  Finding Tickers: 5
  Reviewing Trends: 10
  Company Data: 2

[Button: View Previous Days (7)]
  [Expandable list showing last 7 days of archived usage]

[Button: Archive Today & Reset Counter]
```

#### User-Friendly Category Names
Technical category names are translated to user-friendly descriptions:
- `DISCOVERY` → "Finding Tickers"
- `ANALYSIS` → "Reviewing Trends"
- `FUNDAMENTALS` → "Company Data"
- `ALERTS` → "Alert Checks"
- `SEARCH` → "Ticker Search"

### 4. **Mock Provider Logging Re-enabled**

#### Previous Behavior
Mock provider calls were excluded from logging to avoid inflating usage counts when no real API keys were configured.

#### New Behavior
Mock provider calls are NOW logged and tracked, providing users with:
- **Better understanding** of usage patterns before adding real API keys
- **Realistic preview** of how many API calls will be made with real providers
- **Educational value** showing which operations consume the most calls

**Rationale**: Since mock mode is now clearly indicated with a prominent banner, users understand they're viewing simulated data. Logging these calls helps them understand the app's API consumption patterns.

### 5. **Password Manager Support for API Keys**

#### Enhanced TextField Configuration
API key input fields now include:
```kotlin
OutlinedTextField(
    // ... other params
    singleLine = true,
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        autoCorrect = false
    )
)
```

**Benefits:**
- Password managers (1Password, LastPass, Bitwarden, etc.) can detect fields
- Auto-fill suggestions appear for saved API keys
- Improved UX for users managing multiple API keys
- Follows Android best practices for credential input

## Technical Implementation

### Data Structures

#### ActivityEntry (Enhanced)
```kotlin
data class ActivityEntry(
    val timestamp: Instant,
    val type: ActivityType,
    val tag: String,  // Provider name
    val input: String,  // Operation name
    val output: String,
    val isSuccess: Boolean,
    val durationMs: Long,
    val category: ApiUsageCategory  // NEW
)
```

#### DailyUsageArchive (New)
```kotlin
data class DailyUsageArchive(
    val date: LocalDate,
    val totalCalls: Int,
    val providerBreakdown: Map<String, Map<ApiUsageCategory, Int>>
)
```

### Storage Architecture

#### Current Day Storage
- **SharedPreferences**: `usage_prefs`
- **Keys**: `provider_{name}_category_{category}`
- **Auto-reset**: On day rollover

#### Archive Storage
- **SharedPreferences**: `usage_archive_prefs`
- **Keys**: `{date}_{provider}_{category}`
- **Retention**: 30 days
- **Format**: ISO date strings (YYYY-MM-DD)

### State Management

#### ViewModel Updates
```kotlin
// Observable state flows
val dailyApiCount: StateFlow<Int>
val dailyProviderUsage: StateFlow<Map<String, Map<ApiUsageCategory, Int>>>
val archivedUsage: StateFlow<List<DailyUsageArchive>>

// Manual archive function
fun archiveUsage() {
    UsageTracker.manualArchive()
}
```

#### UI State
```kotlin
data class AnalysisUiState(
    // ... other fields
    val dailyApiUsage: Int = 0,
    val dailyProviderUsage: Map<String, Map<ApiUsageCategory, Int>> = emptyMap(),
    val archivedUsage: List<DailyUsageArchive> = emptyList()
)
```

## User Benefits

### 1. **Transparency**
Users can see exactly how API calls are being used:
- Which providers are being called
- What operations consume the most calls
- Historical trends over time

### 2. **Cost Management**
Better understanding helps users:
- Stay within API rate limits
- Optimize which providers to use
- Identify expensive operations

### 3. **Debugging**
When issues occur, users can:
- Review recent API activity
- Identify failing providers
- See which operations succeeded/failed

### 4. **Planning**
Historical data helps users:
- Predict future usage
- Plan API key upgrades
- Understand daily vs. occasional usage patterns

## Migration Notes

### Breaking Changes
- Changed from `monthlyApiUsage` to `dailyApiUsage`
- Changed from `monthlyProviderUsage: Map<String, Int>` to `dailyProviderUsage: Map<String, Map<ApiUsageCategory, Int>>`
- Added new `archivedUsage` field

### Data Migration
- Old monthly data will be cleared on first app launch with new version
- No automatic migration from monthly to daily format
- Users will start fresh with daily tracking

## Testing Recommendations

### Unit Tests
1. Test category detection logic
2. Test archive creation and cleanup
3. Test day rollover behavior
4. Test manual archive function

### Integration Tests
1. Verify UI displays correct categorized counts
2. Test archive expansion/collapse
3. Verify archive button appears when usage > 0
4. Test password manager detection on API key fields

### Manual Testing
1. Run analysis and verify usage increments
2. Check category breakdown accuracy
3. Test manual archive function
4. Verify archived data persists
5. Test password manager autofill

## Future Enhancements (Optional)

1. **Export Functionality**: Allow users to export usage data as CSV/JSON
2. **Usage Alerts**: Notify when approaching provider limits
3. **Cost Estimation**: Show estimated costs based on provider pricing
4. **Charts/Graphs**: Visual representation of usage over time
5. **Per-Symbol Tracking**: Track which symbols consume the most calls
6. **Optimization Suggestions**: AI-powered recommendations to reduce API usage

## Files Modified

### Core Logic
- `app/src/main/java/com/polaralias/signalsynthesis/util/ActivityLogger.kt`
  - Added `ApiUsageCategory` enum
  - Added `DailyUsageArchive` data class
  - Enhanced `ActivityEntry` with category
  - Implemented daily tracking with archiving
  - Re-enabled mock provider logging

### UI State
- `app/src/main/java/com/polaralias/signalsynthesis/ui/AnalysisUiState.kt`
  - Updated to use daily tracking fields
  - Added `archivedUsage` field

### ViewModel
- `app/src/main/java/com/polaralias/signalsynthesis/ui/AnalysisViewModel.kt`
  - Changed `observeMonthlyUsage()` to `observeDailyUsage()`
  - Added `archiveUsage()` function
  - Updated state flow subscriptions

### UI Components
- `app/src/main/java/com/polaralias/signalsynthesis/ui/SettingsScreen.kt`
  - Completely redesigned usage display section
  - Added categorized breakdown cards
  - Added archive viewer with expand/collapse
  - Added manual archive button
  - Added `formatCategoryName()` helper
  - Added `onArchiveUsage` callback parameter

- `app/src/main/java/com/polaralias/signalsynthesis/ui/ApiKeysScreen.kt`
  - Added password manager support via `KeyboardOptions`
  - Set `keyboardType = Password`
  - Disabled autocorrect

- `app/src/main/java/com/polaralias/signalsynthesis/ui/SignalSynthesisApp.kt`
  - Wired up `onArchiveUsage` callback

## Documentation
- `docs/features/mock_mode_banner.md` - Mock mode banner feature
- `docs/features/api_usage_tracking.md` - This document
