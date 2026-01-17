# Implementation Plan: Converting MCP Server to an Android App

## 1. Analyzing the Current MCP Server's Core Functionality

-   **Multi-Provider Data Access:** The MCP server aggregates financial
    data from multiple providers (Alpaca, Polygon, Finnhub, Financial
    Modeling Prep, etc.) using API
    keys[\[1\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/README.md#L6-L13).
    It instantiates provider clients based on which API keys are
    configured and follows a preference order, falling back to a mock
    data source if
    needed[\[2\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/routing/router.ts#L34-L43)[\[3\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/routing/router.ts#L82-L91).
    This ensures real-time stock quotes and other data can be fetched
    from any available source.

-   **JSON-RPC Tools API:** The server exposes a single `/mcp` endpoint
    that accepts JSON-RPC requests. Various "tools" (functions) are
    registered, each performing a specific analysis
    task[\[4\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/README.md#L144-L151).
    Clients (or an AI agent) can call methods like `get_quotes`,
    `discover_candidates`, etc., by sending JSON with the tool name. The
    server handles authentication (via OAuth tokens or API keys) and
    routes calls to the corresponding
    functions[\[5\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/README.md#L100-L108)[\[6\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/README.md#L113-L121).

-   **Core Analysis Pipeline ("plan_and_run"):** At the heart of the
    server is an **analysis pipeline** that identifies promising stock
    setups. This pipeline is composed of several tools, orchestrated in
    sequence[\[7\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/orchestrator.ts#L20-L28)[\[8\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/orchestrator.ts#L48-L56):

-   *Candidate Discovery:* Identify a list of stock symbols to analyze
    based on trading intent (e.g. day trade, swing, long term). In the
    current server, this is simplified to return a default set of
    popular
    symbols[\[9\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/discovery.ts#L10-L17).
    (The intent was to use a "smart screener" for intelligent
    discovery[\[1\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/README.md#L6-L13),
    possibly filtering by criteria like price, volume, sector, etc., but
    the current implementation uses a static list as a placeholder.)

-   *Tradeability Filtering:* Filter out illiquid or unsuitable stocks.
    The server fetches real-time quotes for the candidates and removes
    any stock with a price below \$1 (penny stocks) or zero
    volume[\[10\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/filters.ts#L30-L38).
    This yields a refined list of symbols that are *tradable* (liquid
    and reasonably priced).

-   *Intraday Enrichment:* For each remaining symbol, gather recent
    intraday price data and compute technical indicators. The server
    pulls intraday price bars (e.g. last 2 days) via the data providers
    and calculates metrics like **VWAP** (Volume-Weighted Average
    Price), **RSI** (Relative Strength Index, 14-period), and **ATR**
    (Average True
    Range)[\[11\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L39-L47)[\[12\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L52-L60).
    (The design also includes MACD and Bollinger Bands as
    features[\[1\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/README.md#L6-L13),
    though the current code only computes RSI and ATR for simplicity.)

-   *Context Enrichment:* Augment each symbol with fundamental and
    sentiment data. This involves fetching the company's profile
    (sector, industry, description), key financial metrics (market cap,
    P/E ratio, etc.), and market sentiment (e.g. bullish/bearish score)
    from the
    providers[\[13\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L74-L80).
    This gives broader context for each stock beyond technicals.

-   *End-of-Day (EOD) Enrichment:* For swing or long-term trades, gather
    historical daily data. The server pulls daily price series (e.g.
    last 200 days) and computes trend indicators like the 50-day and
    200-day simple moving averages (SMA50,
    SMA200)[\[14\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L88-L96)
    to assess long-term momentum.

-   *Setup Ranking:* Finally, evaluate each candidate and rank potential
    trade "setups." The server applies heuristic scoring: e.g. **price
    above VWAP** (+1 score), **oversold RSI \< 30** (+1), **overbought
    RSI \> 70** (-0.5), **price above 200-day SMA** (+1), **positive
    sentiment**
    (+1)[\[15\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/ranking.ts#L23-L31)[\[16\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/ranking.ts#L40-L48).
    It then assigns a confidence (scaled 0.1 to 1.0) and tags the setup
    as "High Probability" or "Speculative" based on the
    score[\[17\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/ranking.ts#L56-L64).
    Each output setup includes the symbol, suggested entry (current
    price), a stop-loss and target price (e.g. ±2%/5% from current
    price), rationale (list of signals that contributed), and a short
    validity
    period[\[18\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/ranking.ts#L58-L66).

-   *Result Delivery:* The orchestrator returns a structured result
    containing the selected intent, timestamp, counts of candidates
    (found vs. filtered), and the ranked list of trade
    setups[\[7\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/orchestrator.ts#L20-L28)[\[8\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/orchestrator.ts#L48-L56).
    This is returned via the API to the caller (which could be a UI or
    even an AI agent using these tools).

-   **Supporting Services:** The MCP server also includes features like
    authentication and API key management. It provides a web UI for
    users to input their own API keys (for data providers) and obtain a
    personal API
    token[\[5\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/README.md#L100-L108).
    This, along with OAuth support, allows secure multi-user access.
    Additionally, an in-memory cache is used to avoid redundant API
    calls (e.g. caching quotes for a few seconds) to improve
    performance[\[19\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/routing/router.ts#L100-L108)[\[20\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/config/schema.ts#L26-L34).

**Summary:** The current MCP server essentially gathers market data from
various APIs, computes technical/fundamental indicators, and generates
trading signals in
real-time[\[1\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/README.md#L6-L13).
Its modular "tools" design allows these functions to be invoked
programmatically (suitable for integration with an AI or other clients).
The key takeaway is the **logic for screening stocks and evaluating
them** -- from retrieving data (quotes, indicators, fundamentals) to
scoring potential investments -- which we will carry over into the
Android app.

## 2. Mapping MCP Logic to an Android App Architecture

-   **Standalone App (Offline-first Logic):** In the Android app, all
    analysis will happen on-device instead of on a server. We will
    reimplement the core logic (screening, filtering, enrichment,
    ranking) in Kotlin, producing the same kind of results. The app will
    not expose a JSON-RPC API; instead, the functions will be called
    internally (e.g. when a user taps "Analyze Market"). This eliminates
    the need for a network server and gives users direct control. The
    same algorithms defined in the server will be translated into Kotlin
    methods (e.g. computing RSI, ATR, moving averages, etc.).

-   **Data Retrieval via APIs:** To fetch real-time stock data and news,
    the app will call external provider APIs (like Finnhub, Polygon,
    Alpaca) directly using HTTP requests. Users will enter their own API
    keys which the app stores securely (e.g. in Android's encrypted
    storage). This mirrors the server's multi-provider setup: for
    example, if a user provides keys for multiple sources, the app can
    try each in a preferred order (similar to the server's provider
    fallback
    logic)[\[2\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/routing/router.ts#L34-L43)[\[3\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/routing/router.ts#L82-L91).
    We'll likely use Retrofit/OkHttp for API calls, defining interfaces
    for each provider's endpoints (e.g. GET quote, GET historical
    prices, etc.). The app's data layer can include a **Repository** or
    **DataSource** class that encapsulates this provider logic --
    choosing the first available provider and falling back to others if
    a call fails, akin to the server's `Router.execute`
    strategy[\[21\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/routing/router.ts#L78-L86).
    We will also include a lightweight caching layer (in-memory or small
    database) to avoid unnecessary repeat requests within a short
    interval (simulating the server's caching of quotes and
    searches)[\[19\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/routing/router.ts#L100-L108).

-   **Replicating Data Models:** We will create Kotlin data classes
    corresponding to the server's core data types -- e.g. `Quote`,
    `IntradayBar`, `DailyBar`, `CompanyProfile`, `FinancialMetrics`,
    `SentimentData`, and `TradeSetup` -- matching the fields used in the
    MCP
    server[\[22\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/types.ts#L11-L21)[\[23\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/types.ts#L70-L79).
    This ensures consistency in what information is handled. For
    instance, a `TradeSetup` class will have properties for symbol,
    setupType, triggerPrice, stopLoss, targetPrice, confidence,
    reasoning, etc., just like the server's output.

-   **UI and Architecture:** We'll use **Kotlin with Jetpack Compose**
    for the UI, following modern Android architecture (MVVM --
    Model-View-ViewModel). The heavy lifting (data fetching and analysis
    logic) will reside in ViewModels or use-case classes, not in the UI
    directly, to keep the app modular and testable. For example, an
    `AnalysisViewModel` could orchestrate the pipeline: when the user
    requests analysis, it calls a function that performs the steps
    (perhaps by invoking smaller helper functions or classes
    corresponding to each "tool"). Each step in the pipeline can be a
    suspend function (using Kotlin coroutines) that mirrors the server's
    tool functions:

-   `discoverCandidates()` -- possibly returning a list of symbols (from
    either an API screener or a static list).

-   `filterTradeable()` -- fetching quotes and filtering.

-   `enrichIntraday()` -- fetching intraday data and computing
    indicators.

-   `enrichContext()` -- fetching profile/metrics/sentiment.

-   `enrichEod()` -- fetching daily history and computing SMAs.

-   `rankSetups()` -- computing scores and assembling `TradeSetup`
    objects. These will use the data repository to get raw data and then
    perform calculations in Kotlin (we can port the indicator formulas
    directly from TypeScript to Kotlin).

-   **Local API Key Management:** In the server, users either configured
    keys via environment or through the web UI (which stored them in a
    database tied to a
    token)[\[5\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/README.md#L100-L108).
    In the app, we will have a **Settings screen** where the user can
    input their API keys for the supported providers (and optionally for
    the LLM service, discussed later). The app will store these keys in
    secure storage (e.g. EncryptedSharedPreferences or Android's
    KeyStore). All API calls will pull the keys from there. This way,
    the keys never leave the device, addressing the requirement of
    "providing API keys locally" -- the user is in control of their
    credentials.

-   **Lifecycle and Performance:** Because the logic runs on-device, we
    must handle threading properly (using coroutines or background
    threads for network calls and computations to avoid blocking the
    UI). The heavy computations (like indicator math or sorting
    rankings) are not extremely intensive (the data sets are relatively
    small per request), but we will ensure efficient implementation. The
    app can perform these computations on a background dispatcher and
    then emit results to the UI. We also need to consider that mobile
    devices have limited uptime for background tasks; long-running or
    periodic analyses will use Android's WorkManager (discussed in the
    next section on notifications) to ensure they run even if the app is
    not in the foreground.

-   **No External Server Required:** By embedding the logic in-app, we
    remove the need for deploying a server or managing OAuth tokens for
    multiple users. Each user's app is essentially their personal "MCP
    server". The same capabilities (fetching quotes, scanning and
    scoring stocks) are now just local functions. This means when
    converting logic, any server-specific concepts (like HTTP requests,
    JSON parsing of input) are replaced with direct method calls and UI
    controls. The app will present the results in a user-friendly way
    (e.g. a list of recommended trades with their confidence and
    reasoning), rather than returning JSON.

## 3. Extended Features for the Android App

In addition to matching the MCP server's capabilities, the Android app
will include extended features as described:

-   **User-Provided LLM Integration:** AI remains a big part of the
    solution. In the app, we will integrate an LLM (Large Language
    Model) by allowing the user to plug in their own API key (for
    services like OpenAI GPT-4 or others). With this, the app can offer
    AI-driven insights or interactions *on top of* the core signal
    generation logic. For example:

-   *Explanations & Summaries:* After generating trade setups, the app
    can formulate a prompt with those results and ask the LLM to
    **explain in plain language** why certain stocks are recommended or
    what the indicators mean, giving the user a narrative summary.

-   *Q&A or Chat:* The app could include a chatbot-style interface where
    the user asks questions (e.g. "Why is this stock flagged as High
    Probability?" or "Show me other tech stocks with bullish signals")
    and the LLM, given access to the analysis results or even the
    ability to call the same analysis functions, can respond
    intelligently. This might involve using a library or SDK to call the
    LLM's API with the user's query and some context (few-shot examples
    or the recent analysis data).

-   *Local AI Processing:* The wording *"runs seamlessly within the
    app"* suggests the AI's logic should feel integrated. We will ensure
    the LLM calls happen behind the scenes when needed (triggered by
    user action or to generate notifications), and results are displayed
    in the UI. Since the model itself likely runs on a cloud service via
    API (given limited on-device AI capability for large models), the
    "seamless" aspect is mostly about user experience. We will clearly
    prompt the LLM with only user-authorized data and use the API key
    securely. (All LLM interactions are optional and under user control,
    as they supply the key.)

-   **Notifications for Market Events:** A key extension is to notify
    users when certain market conditions occur, such as a tracked stock
    dipping or hitting a "buy/sell now" signal threshold. Implementing
    this requires:

-   *Background Monitoring:* We will use Android's WorkManager or
    AlarmManager to schedule periodic checks (for example, every 15
    minutes during market hours) even if the app is closed. A background
    worker can run the analysis pipeline (or a simplified check) on a
    set of symbols of interest. The symbols of interest could be those
    that were recently identified by the app's analysis or a
    user-defined watchlist.

-   *Signal Conditions:* We need to define what triggers a notification.
    Examples:
    -   If a stock's price dips a certain percentage below its VWAP or a
        recent recommended entry price, which might indicate a good
        buying opportunity ("dip buy" signal).
    -   If an indicator threshold is crossed (e.g., RSI dropping below
        30 for oversold or rising above 70 for overbought on a watched
        stock).
    -   If a price hits a target or stop-loss level that was identified
        (though the app is not executing trades, it can notify if, say,
        *"Stock XYZ has reached its target price of \$123"* or *"XYZ
        dropped to the suggested stop-loss level"*).

-   *Implementation:* The worker will fetch fresh quotes (or run a
    focused part of the pipeline) for relevant stocks. If any condition
    is met, it will generate a local notification using Android's
    Notification API. For instance, *"Alert: ABC Corp dipped 5% since
    open -- potential buy opportunity!"*. These notifications direct the
    user to open the app for more details. Users will be able to
    configure these alerts (which conditions to watch, which stocks, and
    the threshold percentages) in the app's settings.

-   *Efficiency:* To avoid excessive API calls, we might limit the
    number of symbols monitored and use provider streaming endpoints if
    available (though implementing real-time streaming in a mobile app
    is complex and can drain battery; a periodic pull is simpler and
    more battery-friendly). WorkManager will ensure the checks happen at
    safe intervals and can respect device conditions (like only run on
    Wi-Fi or while charging, if the user prefers).

-   **Improved Screening Logic:** We aim to use "same or improved" logic
    for identifying stocks. The server's candidate discovery was basic
    (static
    list)[\[9\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/discovery.ts#L10-L17),
    but the app can be smarter:

-   We can incorporate a **stock screener** API if available. For
    example, some provider might support queries like "give me all
    stocks with price between X and Y, volume above Z, in Technology
    sector". If the user's provider has such an endpoint (some do),
    we'll use it to get candidates matching the user's intent and
    criteria.

-   If no direct screening API, we can fetch a broad list of active
    stocks and filter it in-app. For instance, retrieve the day's top
    gainers/losers or most active stocks (many data APIs have an
    endpoint for market movers), then apply criteria (min/max price,
    volume, sector) in code. This approach was hinted in the server
    tests (e.g., using `getMovers` and filtering by sector or volume
    when a native screen wasn't available).

-   We'll allow the user to specify criteria in the UI (optionally). For
    simplicity, the app might offer presets for *day trading* (focus on
    high volume, volatility), *swing trading* (mid-term setups, maybe
    filter by trend or sector), *long-term* (larger market cap, good
    fundamentals). These can map to different parameter presets for the
    screener.

-   By improving this step, the app can propose more relevant symbols
    rather than a fixed list, truly leveraging the "intelligent
    candidate discovery"
    promised[\[1\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/README.md#L6-L13).

-   **UI/UX Improvements:** The app will make the features more
    accessible:

-   A **Dashboard** or home screen will likely summarize the current
    market status (maybe a few major indices or user-selected favorites)
    and provide an entry point to run the analysis.

-   The **Analysis Results Screen** will display the list of recommended
    trade setups in an easy-to-read format. Each item might show the
    stock ticker, confidence level (perhaps as a percentage or 0-100
    gauge), and a couple of key reasons (e.g., "Above VWAP, Positive
    Sentiment"). Tapping an item could expand to show full details: all
    reasoning points, the exact indicator values (RSI value, SMA values,
    etc.), and fundamental context (company info, market cap).

-   If the user has provided an LLM key, we might include an **"AI
    Insight"** section, where an AI-generated commentary on the analysis
    appears (e.g., *"XYZ is looking bullish as it's trading above its
    200-day average and has positive news sentiment. However, RSI is
    very high, indicating it might be overbought in the short term."*).
    This provides a natural language summary to complement the raw data.

-   A **Notifications/Alerts Screen** for configuring alerts as
    discussed, and possibly viewing a history of triggered alerts.

-   **Settings Screens** for managing API keys (for both market data and
    AI), selecting provider preferences (if multiple keys), and other
    options (like toggle use of mock data for demo, set analysis refresh
    interval, etc.).

In essence, the Android app will **carry forward the MCP server's core
mission** -- to analyze and synthesize trading signals -- but will do so
in a self-contained way, while also extending functionality to be more
interactive and proactive (through AI explanations and notifications).
Next, we outline a step-by-step implementation plan to achieve this.

## 4. Detailed Step-by-Step Implementation Plan

Below is a comprehensive plan, broken into stages and tasks, for
building the Android application in Kotlin (with Jetpack Compose). This
plan ensures we start by porting core logic from the server, then
gradually add the UI and extended features. Each step is designed to be
digestible by an LLM or a development team, facilitating agentic coding.

**Phase 1: Project Setup and Core Data Structures**

1.  **Set Up Android Project:** Initialize a new Android project
    (preferably in Android Studio). Choose Kotlin as the language, **Min
    SDK \~24+** (for modern API usage), and include Jetpack Compose
    support. Configure necessary dependencies:

2.  Retrofit and OkHttp for networking.

3.  Coroutines (Kotlinx Coroutines) for async tasks.

4.  Jetpack Compose libraries and AndroidX ViewModel for MVVM.

5.  (Optional) WorkManager dependency for scheduling background tasks
    (for notifications).

6.  (Optional) Security crypto library for encrypted storage of API
    keys.

7.  Ensure Internet permission in the manifest, since we'll call
    external APIs.

8.  **Define Data Models (Kotlin Data Classes):** Create a package
    `model` (or `data.model`) and define data classes reflecting the MCP
    server's domain objects:

9.  `Quote`: fields for symbol, price, change, changePercent, volume,
    high, low, open, prevClose, timestamp,
    source[\[22\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/types.ts#L11-L21).

10. `IntradayBar`: timestamp, open, high, low, close,
    volume[\[24\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/types.ts#L25-L33).

11. `DailyBar`: date, open, high, low, close,
    volume[\[25\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/types.ts#L34-L42).

12. `CompanyProfile`: symbol, name, sector, industry, description,
    etc.[\[26\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/types.ts#L1-L9).

13. `FinancialMetrics`: marketCap, peRatio, dividendYield,
    etc.[\[27\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/types.ts#L43-L50).

14. `SentimentData`: score (e.g. -1 to 1), label
    (Bullish/Bearish/Neutral),
    confidence[\[28\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/types.ts#L62-L70).

15. `TradeSetup`: symbol, setupType (e.g. \"High Probability\"),
    triggerPrice, stopLoss, targetPrice, confidence (0.0--1.0),
    reasoning (list of strings), validUntil
    (Date/DateTime)[\[23\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/types.ts#L70-L79).

16. Also define a `ScreeningCriteria` class/struct (intent, minPrice,
    maxPrice, minVolume, sector, minMarketCap) to represent filters for
    discovery[\[29\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/types.ts#L82-L90).
    These structures will hold the data throughout the pipeline.

17. **Utility: Time and JSON Handling:** If needed, include libraries or
    write util functions for date/time parsing (e.g., if using Java Time
    for timestamps) and JSON (Retrofit can use Moshi or Gson to parse
    API responses directly into these data classes).

**Phase 2: Data Provider Integration (Networking Layer)**

1.  **Provider API Interfaces:** For each data provider we want to
    support (Finnhub, Polygon, Alpaca, FMP, etc.), define Retrofit
    interfaces for the endpoints we need. For example:

2.  Finnhub API (if used): endpoints for quote (e.g.
    `/quote?symbol=XYZ`), company profile, basic financials, news or
    sentiment if available, etc.

3.  Polygon.io API: endpoints for real-time last trade/quote, historic
    bars (aggregates), etc.

4.  Alpaca (if used for market data): they have data API endpoints for
    bars, quotes, fundamentals (though Alpaca might require OAuth and is
    more for trading -- maybe less needed if focusing on data).

5.  FMP API: endpoints for stock screener or financial metrics. Each
    interface will have methods annotated with GET/POST and the expected
    query parameters (with API key as needed). We will configure each
    Retrofit instance with a base URL for the provider and add an
    interceptor or parameter for the API key.

6.  **Provider Selection Logic:** Implement a **Repository** or
    **DataManager** class that knows about all providers and chooses
    which to call:

7.  Load the user's configured API keys from storage.

8.  If multiple providers are available, use a preference order (we can
    define a default order similar to server's default: e.g., Finnhub
    first, then Polygon, etc., with a fallback to a "Mock" provider).

9.  For each type of data, attempt providers in order until one returns
    a valid response. For example, `getQuotes(List<symbols>)` will try
    Finnhub's batch quote API; if that fails or returns incomplete data,
    try Polygon's endpoint next,
    etc.[\[21\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/routing/router.ts#L78-L86).
    This can be implemented with simple `try-catch` around Retrofit
    calls or using Kotlin's `runCatching`.

10. Include a **MockProvider** for offline/demo mode: This can return
    hardcoded or randomly generated data for a given symbol. It's useful
    for testing the app without real API calls or if the user has no
    keys. We can trigger mock mode if no real keys are provided or the
    user explicitly enables a "demo mode".

11. **Caching Layer:** To avoid hitting rate limits or unnecessary
    network usage, implement a basic caching for certain calls:

12. For real-time quotes, we might cache them for e.g. 1 minute per
    symbol.

13. For company profiles or financial metrics, cache for a longer time
    (these don't change often, maybe cache for an hour or a day).

14. Use an in-memory cache (e.g. a `Map` with timestamp) or Room
    database for persistence across app restarts. We might also use
    DataStore/SharedPreferences for small data. Alternatively, simply
    rely on WorkManager to pre-fetch data at intervals so the UI uses
    mostly fresh cached data.

15. Ensure the repository checks the cache first and only calls the API
    if data is stale or missing, similar to how the server's Router uses
    `getOrSet` with
    TTL[\[19\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/routing/router.ts#L100-L108).

16. **Testing Data Layer:** Write unit tests or use the app in a debug
    mode with a sample API key to ensure each provider call works and
    the fallback logic is correct. This step is crucial before moving
    on, as the rest of the app depends on reliable data retrieval.

**Phase 3: Core Logic Implementation (MCP Tools in Kotlin)**

1.  **Candidate Discovery (**`discoverCandidates`**):** Implement a
    function (in, say, an `AnalysisRepository` or a use-case class) to
    retrieve a list of candidate symbols:

2.  If the user selected an **intent** (day_trade, swing, long_term),
    apply appropriate logic. For example:
    -   *day_trade:* We might use a "top movers" endpoint to get the
        most volatile stocks of the day, or get high volume stocks near
        real-time. Many providers have an API for biggest gainers/losers
        or active stocks.
    -   *swing/long_term:* We might use a screener query: e.g. find
        stocks within a certain price range, market cap, and perhaps in
        a specific sector if the user chose one.

3.  If a provider offers a **screener API** (like FMP has a screener
    endpoint, Polygon has some /snapshot endpoints), use it directly by
    passing the criteria. If it returns a list of symbols, take the top
    N (the server defaulted to 20
    symbols[\[30\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/discovery.ts#L4-L12)).

4.  If no direct API, fallback: use a predefined list or a combination
    of indices. For instance, include a static list of popular tickers
    (like S&P 500 components) and then filter that list by criteria
    (price, volume using last known data). The server's default
    list[\[9\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/discovery.ts#L10-L17)
    can be a starting point, but we aim to improve it. We could also
    fetch a broad market list from one provider (some APIs allow
    downloading all tickers or an entire index's constituents).

5.  Ensure this function returns a `List<String>` of symbols. Also
    return some metadata if needed (like number of symbols found before
    filtering, etc., which the server tracked as
    `candidatesFound`[\[31\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/orchestrator.ts#L30-L37)).

6.  **Filter Tradeable (**`filterTradeable`**):** Implement a function
    to filter out unsuitable symbols:

7.  Input: List of candidate symbols (from previous step).

8.  For those symbols, fetch real-time quotes via the repository.

9.  Check each quote: if price \< \$1.00 or volume == 0, exclude it (and
    record the
    reason)[\[10\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/filters.ts#L30-L38).
    We might also filter out stocks that have excessively wide spread or
    other quality criteria if desired (the server's description mentions
    "based on
    volume/spread"[\[32\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/README.md#L145-L148),
    though the current code only checks volume and price).

10. Output: a filtered list of symbols that passed, and optionally a
    list of rejections with reasons (for debugging or display if
    needed). The server returns both validSymbols and
    rejections[\[33\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/filters.ts#L20-L28)[\[10\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/filters.ts#L30-L38).
    In the app, we might not need to show rejections to the end user,
    but keeping this for logging or AI explanation is useful.

11. **Intraday Enrichment (**`enrichIntraday`**):** For each symbol, get
    intraday price data and compute technical indicators:

    -   Use the repository to fetch recent intraday bars (e.g. 5-minute
        candles for the last 1-2 days). For example, Polygon has an
        endpoint for historic aggregates (e.g. 5-min bars for X days),
        Finnhub has similar.
    -   Compute **VWAP**: Calculate volume-weighted average price over
        all retrieved intraday bars (sum(price \* volume) /
        sum(volume))[\[34\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L44-L52).
    -   Compute **ATR**: Use a window (e.g. 14 periods as default) --
        iterate over bars to calculate the Average True
        Range[\[35\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L137-L145).
        ATR gives a sense of volatility.
    -   Compute **RSI**: 14-period RSI on close
        prices[\[36\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L109-L118)[\[37\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L121-L129).
        The algorithm from the server can be directly translated.
    -   (Optional but desirable) Compute **MACD**: Calculate the Moving
        Average Convergence Divergence (12-day EMA, 26-day EMA,
        difference = MACD line, signal = 9-day EMA of MACD line,
        histogram = MACD - signal). Since intraday data covers 2 days in
        server, MACD might be more meaningful on daily data; but if
        needed, we can compute a shorter-term MACD on intraday or skip
        it.
    -   (Optional) Compute **Bollinger Bands**: Usually based on a
        20-period moving average and standard deviations. Could apply to
        intraday if enough points, or skip if data is limited.
    -   Package the results for each symbol in an `IntradayStats` object
        (with fields like vwap, rsi, atr, and possibly macd, bollinger,
        plus the raw
        bars)[\[38\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L4-L13)[\[39\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L33-L41).
        Return a map of symbol -\> IntradayStats.
    -   Handle edge cases: If no bars returned (e.g. symbol has no
        intraday data), skip that symbol. If data length is insufficient
        for a 14-period RSI/ATR, those can be left null/undefined.

12. **Context Enrichment (**`enrichContext`**):** For each symbol, fetch
    fundamental and sentiment data:

    -   Use repository to get company profile (name, sector, industry,
        description)[\[40\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L74-L78).
    -   Get financial metrics (market cap, PE,
        etc.)[\[40\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L74-L78).
    -   Get sentiment data. If the provider has a news sentiment
        endpoint or an analyst sentiment score, use that. Finnhub, for
        example, provides sentiment analysis for news or social media
        sentiment for a stock. If not available, we could derive a
        simple sentiment from news headlines (or even use the LLM to
        classify news sentiment in the future).
    -   Combine these into a `ContextData` object for each
        symbol[\[41\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L68-L76)[\[42\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L78-L81).
    -   If any of these are unavailable (e.g. no sentiment data), just
        set those fields to null or default. The app can handle missing
        data gracefully (and the scoring logic will just ignore missing
        sentiment or profile).

13. **End-of-Day Enrichment (**`enrichEod`**):** (Only needed for swing
    or long_term intents):

    -   Fetch daily bars for each symbol (at least 200 days to compute
        SMA200)[\[43\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L90-L96).
    -   Compute 50-day and 200-day simple moving averages: sum of last N
        closes /
        N[\[44\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L93-L101).
    -   Prepare an `EodStats` for each symbol with SMA50, SMA200, and
        perhaps the array of daily bars or any other trend indicator we
        want[\[45\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L84-L92)[\[44\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L93-L101).
    -   If a symbol has fewer than 200 days of data (e.g. recent IPO),
        we might skip or compute what we can (the server skips if less
        than 50 data
        points)[\[46\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/enrichment.ts#L89-L96).
    -   This step is skipped for day_trade intent in our logic, to save
        time, just as the server
        does[\[47\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/orchestrator.ts#L42-L46).

14. **Ranking (**`rankSetups`**):** Implement the scoring logic to
    convert all the gathered data into ranked trade setups:

    -   Inputs: the IntradayStats, ContextData, and EodStats maps from
        previous steps.
    -   For each symbol in the intraday map (which represents each
        candidate that passed filtering):
    -   Retrieve the latest price (e.g. last close of intraday bars).
    -   Initialize a score and reasons list.
    -   Apply rules similarly to the server:
        -   If price is above VWAP, add +1 (reason: "Price above
            VWAP")[\[15\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/ranking.ts#L23-L31).
        -   If RSI is defined:
        -   If RSI \< 30, add +1 (reason: "RSI Oversold (X)" where X is
            the
            value)[\[48\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/ranking.ts#L29-L37).
        -   If RSI \> 70, subtract 0.5 (reason: "RSI Overbought (Y)").
        -   If EodStats is present:
        -   If current price \> SMA200, add +1 (reason: "Price above 200
            SMA")[\[16\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/ranking.ts#L40-L48).
        -   (We could also consider price vs SMA50 for shorter trend,
            but the server only checked SMA200 as a long-term bullish
            signal.)
        -   If sentiment is available:
        -   If sentiment.score \> 0.2 (meaning notably positive), add +1
            (reason: e.g. "Positive Sentiment (Bullish)" or use the
            label from
            data)[\[49\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/ranking.ts#L48-L56).
        -   If sentiment.score \< -0.2, we might subtract points (the
            server example didn't subtract for negative sentiment, but
            we could if we want to refine logic).
        -   (Optional additional criteria: If company has strong
            financial metrics, e.g. low PE ratio or high growth, we
            could add a small score for fundamentals -- this was not in
            the original logic, but an app improvement could incorporate
            it.)
    -   Compute a confidence = clamp(score / MaxScore, min 0.1, max
        1.0)[\[17\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/ranking.ts#L56-L64).
        In the server MaxScore was 4 (because there were roughly 4
        criteria checked). If we add more criteria, adjust accordingly.
    -   Determine setupType: if score is above a threshold (say \> 2.0
        as in server), label it "High Probability"; otherwise
        "Speculative"[\[18\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/ranking.ts#L58-L66).
    -   Determine triggerPrice (we can use current price as an entry
        point)[\[50\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/ranking.ts#L58-L64).
    -   Calculate a suggested stopLoss and targetPrice. The server used
        -2% and +5% from current
        price[\[51\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/ranking.ts#L60-L67)
        -- we can do the same or make it configurable. (These are
        generic placeholders; the user might ultimately decide their own
        exits, but providing some standard risk/reward marker is
        helpful.)
    -   Set validUntil to now + 30 minutes (server used 30
        min)[\[52\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/ranking.ts#L62-L66)
        or maybe longer for swing/long-term (we could extend validity
        for long_term setups, e.g. a few days, since those signals are
        slower).
    -   Create a `TradeSetup` object with all these fields and add to a
        list.
    -   Sort the list by confidence
        descending[\[53\]](https://github.com/polaralias/signal-synthesis-mcp/blob/3d1a5a398606c386c8d3368d390acff375fe1cb0/src/tools/ranking.ts#L68-L71)
        so the best ideas are first.
    -   Return the list of TradeSetup objects.

15. **Review and Test Core Logic:** At this point, we have all the main
    computational pieces implemented. We should test them in isolation:

    -   Write unit tests for indicator calculations (RSI, ATR, etc.)
        using known data to ensure correctness.
    -   Test the ranking function with synthetic
        `IntradayStats`/`ContextData` inputs to see if scoring behaves
        as expected (e.g. create a scenario and verify the outcome).
    -   Possibly create a dummy flow in the console or a simple UI to
        run `planAndRun` equivalent and log the results for a known set
        of symbols (for example, use mock data or real API with a
        personal key to simulate a full run).
    -   This verifies that converting from TypeScript/Node to Kotlin
        preserved the logic.

**Phase 4: Android UI Development (Jetpack Compose)**

1.  **Design UI Structure:** Outline the screens and navigation. Likely
    screens:

    a.  **API Key Configuration Screen:** First-run experience or a
        settings page where users input their keys (for various data
        providers and LLM). This can be a simple form in Compose
        (TextFields for each key, maybe a drop-down to select provider,
        etc.). Include validation (e.g. length or format) and secure
        storage on save. If multiple providers, allow entering multiple
        keys.
    b.  **Main Analysis Screen (Dashboard):** The home screen after
        setup. It could show:
    c.  A picker for trading intent (Day Trade, Swing, Long Term) --
        perhaps as a dropdown or toggle chips.
    d.  A button "Run Analysis" which triggers the pipeline.
    e.  Optionally, some quick info like current date/time, maybe market
        status or indices.
    f.  If results from a previous run exist, maybe show a summary or
        the top pick.
    g.  **Results List Screen:** After running analysis, display a list
        of TradeSetups. Each item shows key info (ticker, confidence %,
        label, maybe an icon or color indicating High Probability vs
        Speculative). We can use LazyColumn in Compose to list them.
    h.  Each item on click can navigate to a **Details Screen**.
    i.  **Trade Setup Details Screen:** Shows full details for one
        selected stock:
    j.  Price info (current price, day's change, maybe a small chart of
        intraday if we can draw from data).
    k.  Indicators: list out the values (RSI, VWAP vs price, ATR value,
        SMA50/200 if applicable).
    l.  Fundamentals: company profile snippet (name, industry), market
        cap, PE, etc.
    m.  Sentiment: show the sentiment score/label, perhaps with an icon.
    n.  The reasoning list: display the bullet points of reasoning that
        were in the TradeSetup (these are basically the signals
        triggered).
    o.  If AI is enabled, possibly a button "Explain with AI" on this
        screen, which when pressed will generate an LLM explanation for
        this particular stock's setup (taking the reasoning and data as
        input to the prompt).
    p.  **Alerts/Notifications Screen:** Allows configuration of alerts.
        For simplicity, it could list recent alerts and have a settings
        area:
    q.  Let the user toggle on/off certain conditions (e.g. "Notify me
        on price dips of 5%").
    r.  Possibly list the symbols currently being monitored (maybe those
        from the last analysis or user's saved watchlist).
    s.  Could reuse the Results list to let user pick which ones to
        watch.
    t.  **Settings Screen:** General settings including managing API
        keys (if not done on a separate screen), toggling mock mode,
        setting update intervals for background tasks, etc. Also
        possibly theme toggle (light/dark) since Compose makes that
        straightforward.
    u.  Set up navigation (using Jetpack Navigation for Compose or
        simple state management with Compose navigation APIs) between
        these screens.

2.  **Compose UI Implementation:**

    -   Start with the **Main Analysis Screen**. Implement the intent
        picker (could be `SegmentedButtons` or a Row of selection chips)
        and a primary action button. When clicking "Run Analysis", call
        a function in the ViewModel to execute the pipeline. Show a
        loading indicator (CircularProgressIndicator) while analysis is
        running.
    -   Bind the ViewModel's state to the UI:
    -   The ViewModel will expose LiveData/StateFlow for loading state,
        error messages, and the list of results.
    -   Compose can collect these (using `collectAsState`) and
        reactively update the UI.
    -   Implement the **Results List Screen** (this can be part of the
        main screen or a separate destination). Each item in the list
        can be a Card composable with the info. Use visual cues for
        confidence (e.g. progress bar or color coding high vs low
        confidence).
    -   Implement the **Details Screen**. This might involve more
        complex UI (maybe a scrollable column with sections for each
        category of info). Use Text composables for each piece of data,
        and perhaps Divider to separate sections. If adding a small
        chart, we might use a Canvas or a third-party chart library to
        plot intraday prices; or a simpler approach: show a list of last
        few price points or percent change. To keep initial
        implementation simple, focus on textual data and maybe a
        sparkline later.
    -   **API Key Screen:** Use basic TextField components for each key.
        You might have one TextField per provider (labeled accordingly).
        Also a field for LLM API key. Provide a "Save" button. On save,
        validate and store to EncryptedSharedPreferences (via a small
        helper function or ViewModel function).
    -   Ensure to handle cases where required keys are missing: e.g., if
        the user tries to run analysis without any data provider key,
        show an error or prompt them to add keys.
    -   Use Compose theming for a clean look, and ensure the layout is
        responsive (use Column/Row with appropriate weights, etc., or
        Box with alignment for overlaying loading spinner).
    -   Test the UI flows with dummy data initially (you can simulate a
        ViewModel returning a hardcoded TradeSetup list to verify the UI
        layout).

3.  **Connecting UI with Logic (ViewModel):**

    -   Create an `AnalysisViewModel` which has functions like
        `runAnalysis(intent)` and data streams for `analysisResults`
        (list of TradeSetup), `isLoading`, and `errorMessage`.
    -   When `runAnalysis` is called, the ViewModel will:
    -   Check that API keys exist (if not, emit an error state telling
        user to configure keys).
    -   Switch to loading state.
    -   Call the repository functions in sequence: discoverCandidates,
        filterTradeable, enrichIntraday & enrichContext (& enrichEod if
        needed), rankSetups. This can be done in a coroutine (e.g. using
        `viewModelScope.launch`).
    -   Collect the results into a list of TradeSetup and post to
        `analysisResults`.
    -   Handle exceptions: if any step fails (network error, etc.),
        catch it and emit an `errorMessage` for the UI to display (and
        maybe fall back gracefully if possible).
    -   The repository (from Phase 2 and 3) can be injected or
        instantiated in the ViewModel. Use dependency injection (like
        Hilt) if comfortable, or a simple singleton pattern for the
        repository.
    -   Also consider an `AIViewModel` or extend the same ViewModel to
        handle AI requests. For example, a function
        `explainSetupWithAI(tradeSetup)` that uses the LLM API. It would
        format a prompt with the trade setup data (and perhaps related
        data like company profile) and call the OpenAI API (using
        Retrofit or OkHttp). The response (a text explanation) would
        then be emitted for the UI to display. Since API calls should be
        off the main thread, use coroutines for this as well. Provide
        feedback to UI (e.g., a loading spinner while the AI is
        thinking, and error handling for failed requests).

4.  **LLM API Integration:** Implement the service to call the LLM:

    -   Assuming OpenAI GPT-4/3.5: the endpoint is typically a POST to
        `https://api.openai.com/v1/chat/completions` with the API key in
        header and a JSON body of messages. We can set up a Retrofit
        service or just use OkHttp directly for this single endpoint.
    -   Create a method that takes a prompt or a list of messages (for
        chat format) and returns the assistant's reply. The prompt would
        include the context. For example:

    <!-- -->

    -   "Analyze the following trading signal:\nTicker: ABC\nReasons: Price above VWAP, RSI Oversold (28), Positive Sentiment (Bullish).\nExplain what this means and why ABC might be a good opportunity."

        The model's answer can be shown to the user.

    <!-- -->

    -   Ensure to handle the API key securely: read it from encrypted
        storage and include in request header
        (`Authorization: Bearer <key>`).
    -   This integration should be decoupled from the main analysis flow
        (so that lack of an AI key doesn't stop the core features). It's
        an add-on the user can utilize on demand.

5.  **Notifications & Background Work:**

    -   Decide the strategy: periodic checks vs. push (likely periodic
        since we can't rely on server push in a local-only scenario).
    -   Use **WorkManager** to schedule a periodic work (e.g. a
        **PeriodicWorkRequest** that runs every X minutes). WorkManager
        is ideal as it respects Doze mode and system optimizations.
    -   Create a Worker class, e.g. `MarketAlertWorker`, that when
        executed will:
    -   Load the list of symbols to monitor. (This could be from the
        last analysis result stored in local storage, or a user-defined
        list. For a first version, we might say it monitors the symbols
        from the most recent analysis run or a curated list of important
        symbols if none.)
    -   Fetch fresh quotes for those symbols (via the same data
        repository).
    -   Apply the alert conditions. For example, for each symbol:
        -   Check if current price is X% below the price when it was
            last analyzed or below its VWAP from last analysis (if we
            saved that). Or simply, if current RSI just dropped below 30
            or MACD turned positive, etc.
        -   If any condition is true, create a Notification. Use
            NotificationCompat to build it, with appropriate channel
            (create a notification channel for "Market Alerts"). The
            notification text should be concise, e.g., *"ABC dropped 5%
            since last check -- possible dip buy opportunity."* or
            *"XYZ's RSI is now oversold (28)."*
        -   The notification's tap action should lead the user into the
            app, maybe opening the detail screen for that symbol.
    -   We might include in the Worker some logic to not spam
        notifications -- e.g., only one notification per symbol per
        significant event or per day, etc.
    -   Enqueue this work when the user enables alerts. Perhaps in the
        Alerts Screen, have a toggle "Enable background alerts". When
        turned on, schedule the WorkManager job with the chosen
        frequency. When off, cancel the WorkManager.
    -   Also handle device reboot: if alerts are on, use WorkManager's
        existing persistence (it will usually continue, but we might
        also use BootReceiver if needed to reschedule).

6.  **Polish and Additional Enhancements:**

    -   Implement persistence for analysis results if needed (so that
        the user can see the last results without re-running, or so the
        background worker knows previous trigger prices/VWAPs).
    -   Optimize performance: e.g., if analyzing many symbols, consider
        concurrency limits to not overload the device or network. Use
        coroutines with dispatchers and maybe limit the number of
        parallel API calls (Dispatchers.IO is fine for network calls,
        but dozens of calls at once might be heavy).
    -   Add error handling and retry logic for network calls in
        repository (maybe use Retrofit's features or manual retry with
        exponential backoff for certain endpoints).
    -   UI improvements: Add animations or better state handling in
        Compose, such as pull-to-refresh if applicable, or nice
        transition when navigating to detail.
    -   Security considerations: ensure API keys are not exposed (don't
        log them, and perhaps allow the user to wipe them easily).
    -   If time permits, implement support for OAuth for providers like
        Alpaca that might need it (since the server had an OAuth flow).
        However, this can be complex on mobile; alternatively, the app
        can ask for Alpaca keys directly (which is essentially the same
        as an API key method).
    -   **Testing:** Do a round of integration testing -- run the app
        with actual API keys during market hours to see if the data
        populates and signals make sense. Adjust thresholds or logic if
        the results seem off (the heuristic can be tuned based on real
        feedback).
    -   Finally, prepare the app for release: set up proper permission
        requests (if using work in background on certain OEMs, might
        need battery optimizations exemption instructions), and ensure
        compliance with provider API terms (some require attributions or
        have usage limits which we should document to the user).

7.  **Documentation & Agent Handoff:** Since the development will rely
    on agentic coding (LLM assistance), maintain clear documentation in
    the code for each step (comments explaining the logic ported from
    the MCP server). Also document how the app's architecture
    corresponds to the original server (for future maintainers or
    agents: e.g., "This class corresponds to the MCP server's
    Router/Provider mechanism", "This function's formula is derived from
    the server's indicator calculation"). This will help an LLM (or any
    developer) understand the context of each part. We should also
    include usage instructions in a README for the app.

By following these steps, we will have systematically transformed the
MCP server into a feature-rich Android application. The core logic of
**market signal synthesis** is preserved (and even improved with more
indicators and smarter screening), while the new app design adds
usability (UI, notifications) and personalization (user's own API keys
and AI integration). Each phase ensures the system is built up in
logical increments, which is ideal for an LLM-driven development
approach to implement and validate iteratively.

