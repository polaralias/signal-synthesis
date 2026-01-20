- **Smart Filtering & Ranking:** Filters candidates based on liquidity and ranks them using a weighted scoring system based on technical and sentiment factors.
- **Provider Fallback:** Robustly fetches data from multiple providers (Alpaca, Polygon, Finnhub, Financial Modeling Prep) with automatic fallback and prioritization.
- **Background Alerts:** Monitors your watchlists in the background using WorkManager and notifies you of significant price or indicator movements.
- **Secure Storage:** Encrypted storage for all API keys.

## Architecture

The app follows modern Android development practices and Clean Architecture principles:

- **UI Layer:** Jetpack Compose, Material3, Navigation Component.
- **Presentation Layer:** MVVM with `StateFlow` for reactive UI updates.
- **Domain Layer:** Pure Kotlin Use Cases encapsulating business logic (e.g., `RunAnalysisUseCase`, `RankSetupsUseCase`).
- **Data Layer:** Repository pattern with `Retrofit` for network calls, `Moshi` for parsing, and local caching strategies.
- **Dependency Injection:** Manual dependency injection (currently) via `MainActivity` and factories, structured for easy migration to Hilt.

### Project Structure

- `app/src/main/java/com/polaralias/signalsynthesis/`
  - `ui/` - Compose screens, ViewModel, and UI state.
  - `domain/` - Use cases, models, and interfaces (core logic).
  - `data/` - Repositories, API providers, storage, and workers.

## Setup

1. **Prerequisites:**
   - Android SDK (minSdk 24, targetSdk 34)
   - JDK 17
   - Valid API keys for at least one data provider (Alpaca, Polygon, Finnhub, or FMP).
   - OpenAI API key (optional, for AI synthesis).

2. **Configuration:**
   - Open `local.properties` and set `sdk.dir` to your Android SDK location.
   - Build using Gradle: `./gradlew assembleDebug`

3. **Running the App:**
   - Install on an emulator or device.
   - Navigate to the "Settings" or "Keys" screen.
   - Enter your API keys.
   - Go to "Analysis", select your intent, and tap "Run Analysis".

## Testing

The project includes unit and integration tests for core domain logic:

- **Unit Tests:** Cover technical indicators, ranking logic, and tradeability filters.
- **Integration Tests:** Verify the pipeline orchestration and repository fallback mechanisms using fake providers.

Run tests with:
```bash
./gradlew testDebugUnitTest
```

## Current Status

- **Phases 0-7 Complete:** Core pipeline, UI, AI integration, and Alerts are implemented.
- **Phase 8 (Hardening):** Unit tests added. Thread-safety improvements in progress.
