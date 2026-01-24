# Mock Mode Banner Feature

## Overview
A prominent banner has been implemented to clearly indicate when the Signal Synthesis application is running in mock mode (i.e., when no API keys are configured and only simulated data is being used).

## Implementation Details

### Component: `MockModeBanner`
**Location:** `app/src/main/java/com/polaralias/signalsynthesis/ui/UiComponents.kt`

A new composable component that displays a warning banner when mock mode is active:

```kotlin
@Composable
fun MockModeBanner(isVisible: Boolean, onClick: () -> Unit = {})
```

**Features:**
- **Visibility Control:** Only displays when `isVisible = true` (i.e., when `!uiState.hasAnyApiKeys`)
- **Visual Design:** 
  - Uses error container color scheme for high visibility
  - Bold warning emoji (⚠️) and "MOCK MODE ACTIVE" title
  - Clear explanatory text about why mock mode is active
  - Clickable surface to navigate to API key configuration
- **Full-width banner** that spans the entire screen width
- **Consistent styling** using Material 3 design system

### Integration Points

The `MockModeBanner` has been integrated into three key screens:

#### 1. **AnalysisScreen**
- **Location:** Top of the screen, before all other content
- **Click Action:** Opens API Keys configuration screen
- **Replaced:** Previous inline Card-based mock mode indicator in the Keys section

#### 2. **ResultsScreen**
- **Location:** Top of the screen, immediately after the top app bar
- **Click Action:** No action (non-clickable)
- **Purpose:** Reminds users that displayed results are based on mock data

#### 3. **DashboardScreen**
- **Location:** Top of the screen, before market overview and other sections
- **Click Action:** Opens Settings screen
- **Purpose:** Provides context for all dashboard data being displayed

### User Experience Flow

1. **Initial State (No API Keys):**
   - User opens the app for the first time
   - Banner appears prominently at the top of all main screens
   - Clear warning that synthesis is running in mock mode only

2. **User Action:**
   - User can click the banner on AnalysisScreen → navigates to API Keys screen
   - User can click the banner on DashboardScreen → navigates to Settings screen
   - User can also use existing "Add Keys" button in AnalysisScreen

3. **After Configuration:**
   - Once API keys are added (`hasAnyApiKeys = true`)
   - Banner automatically disappears from all screens
   - Real market data providers become active

### Technical Implementation

**State Management:**
- Banner visibility is controlled by `uiState.hasAnyApiKeys` boolean
- This state is derived from `ApiKeys.hasAny()` in the `ProviderFactory`
- State flows through the ViewModel to all UI screens

**Styling:**
- Uses `MaterialTheme.colorScheme.errorContainer` for background
- Uses `MaterialTheme.colorScheme.error` for title text
- Uses `MaterialTheme.colorScheme.onErrorContainer` for body text
- Consistent padding: 16dp horizontal, 12dp vertical

## Benefits

1. **Improved User Awareness:** Users immediately understand they're viewing simulated data
2. **Reduced Confusion:** Clear explanation of why mock mode is active
3. **Easy Navigation:** One-click access to API key configuration
4. **Consistent UX:** Same banner appears across all relevant screens
5. **Non-Intrusive:** Automatically disappears once configured

## Testing Recommendations

1. **Visual Testing:**
   - Verify banner appears on AnalysisScreen, ResultsScreen, and DashboardScreen when no API keys are configured
   - Verify banner disappears when at least one API key is added
   - Check responsive layout on different screen sizes

2. **Interaction Testing:**
   - Click banner on AnalysisScreen → should navigate to API Keys screen
   - Click banner on DashboardScreen → should navigate to Settings screen
   - Verify banner on ResultsScreen is informational only

3. **State Testing:**
   - Add API keys → verify banner disappears
   - Remove all API keys → verify banner reappears
   - Test with partial API key configuration

## Future Enhancements (Optional)

- Add animation when banner appears/disappears
- Include a "Learn More" link to documentation
- Show which specific providers are in mock mode vs. real mode
- Add a dismiss option for advanced users who understand mock mode
