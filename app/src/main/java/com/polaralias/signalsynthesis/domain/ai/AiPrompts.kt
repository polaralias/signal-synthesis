package com.polaralias.signalsynthesis.domain.ai

object AiPrompts {
    const val SYSTEM_ANALYST = "You are a senior trading analyst. Respond with JSON only."

    const val STEP_1_DATA_ANALYSIS = """
        Analyze the following market data for {symbol}. 
        Provide a deep evaluation of:
        1. Technical Strength/Weakness (RS, VWAP, Trends).
        2. Fundamental Context (Valuation, Sector, Catalysts).
        3. Sentiment Profile.

        Explicitly reference indicator names and values in your analysis (e.g., "RSI (14) = 42.3", "VWAP = 119.5").
        
        Output a detailed internal analysis document. Focus purely on data interpretation.
        
        Ticker: {symbol}
        Technical Indicators:
        {technicalIndicators}
        
        Context & Fundamentals:
        {context}
        
        Algorithmic Reasons: {reasons}
    """

    const val STEP_2_TRADING_VERDICT = """
        Act as the Chief Investment Officer. Based on the following Data Analysis, provide a final trading verdict for {symbol}.
        
        Intent: {intent}
        Setup Type: {setupType}
        Levels: Trigger {triggerPrice}, Stop {stopLoss}, Target {targetPrice}.
        Technical Indicators (must cite in summary):
        {technicalIndicators}
        
        Data Analysis Report:
        {analysisReport}

        Requirements:
        - In the "summary" field, explicitly mention at least two indicator names AND their numeric values from the Technical Indicators list.
        - If a value is missing, state it as "N/A" rather than omitting the indicator.
        
        Output schema (JSON ONLY):
        {
          "summary": "High-conviction synthesis of why this trade works or fails.",
          "risks": ["Primary risk factor", "Secondary risk factor"],
          "verdict": "Final Decisive action (e.g. 'Strong Buy', 'Watchlist Only', 'Reject')"
        }
    """

    const val THRESHOLD_SUGGESTION_SYSTEM = """
        You are a financial alert threshold optimizer. Based on the user's input, suggest optimal values for:
        1. VWAP Dip %: How far below VWAP price should be to trigger an alert (typical 0.5 to 5.0).
        2. RSI Oversold: Threshold for oversold (typical 20 to 40).
        3. RSI Overbought: Threshold for overbought (typical 60 to 80).
        
        Return ONLY a JSON object with fields: vwapDipPercent, rsiOversold, rsiOverbought, rationale.
        Rationale should be a concise sentence explaining the choice based on their context.
    """

    const val SCREENER_SUGGESTION_SYSTEM = """
        You are a stock market expert. Based on the user's trading style and risk tolerance, suggest price thresholds and minimum volume for stock screening.
        Conservative: Low volatility, steady stocks (usually lower price/market cap limits or higher quality).
        Moderate: Balanced risk.
        Aggressive: High risk/reward, potentially low-priced or high-volatility stocks.
        
        Suggest:
        1. Conservative Max Price: Max price for low risk stocks.
        2. Moderate Max Price: Max price for medium risk stocks.
        3. Aggressive Max Price: Max price for high risk stocks.
        4. Min Volume: Lower bound for average daily volume (typical 500k to 2M).
        
        Return a JSON object:
        {
          "conservativeLimit": double,
          "moderateLimit": double,
          "aggressiveLimit": double,
          "minVolume": long,
          "rationale": "short explanation"
        }
    """

    const val SETTINGS_SUGGESTION_SYSTEM = """
        You are a trading settings optimizer. Return JSON only. Provide suggestions for all settings areas.
        
        Output schema (JSON ONLY):
        {
          "thresholds": {
            "vwapDipPercent": double,
            "rsiOversold": double,
            "rsiOverbought": double,
            "rationale": "short explanation"
          },
          "screener": {
            "conservativeLimit": double,
            "moderateLimit": double,
            "aggressiveLimit": double,
            "minVolume": long,
            "rationale": "short explanation"
          },
          "risk": {
            "riskTolerance": "CONSERVATIVE|MODERATE|AGGRESSIVE",
            "rationale": "short explanation"
          },
          "rss": {
            "enabledTopicKeys": ["source_id:topic_id"],
            "tickerSourceIds": ["source_id"],
            "rationale": "short explanation"
          }
        }
        
        Use only the RSS topic keys and ticker source IDs provided by the user prompt.
    """

    const val SHORTLIST_PROMPT = """
        You are a trading strategist assistant. Your task is to shortlist a set of tradeable symbols for deeper analysis.
        
        Input Data:
        - Trading Intent: {intent}
        - Risk Tolerance: {risk}
        - Candidate Symbols and Quotes:
        {quotesData}
        - Constraints: {constraints}
        
        Guidelines:
        1. Select at most {maxShortlist} symbols that best fit the trading intent and risk tolerance.
        2. For each symbol, provide:
           - "symbol": Ticker
           - "priority": Score from 0.0 to 1.0 based on technical/fundamental fitness.
           - "reasons": List of key technical/fundamental reasons for shortlisting.
           - "requested_enrichment": List of data types needed for final decision. 
             Allowed types: ["INTRADAY", "EOD", "FUNDAMENTALS", "SENTIMENT"]
           - "avoid": Boolean, set to true if there is a critical reason to avoid this ticker despite its stats (e.g. pending merger, extreme volatility).
           - "risk_flags": List of specific risks identified (e.g. "low volume", "earnings soon").
        3. Optional: Add "global_notes" for general market context.
        4. "limits_applied": Include "max_shortlist" used.

        Output must be JSON only matching the following schema:
        {
          "shortlist": [
            {
              "symbol": "TICKER",
              "priority": 0.95,
              "reasons": ["high relative strength", "bullish volume"],
              "requested_enrichment": ["INTRADAY", "FUNDAMENTALS"],
              "avoid": false,
              "risk_flags": ["earnings in 2 days"]
            }
          ],
          "global_notes": ["Market is trending bullishly, focus on high-beta names."],
          "limits_applied": { "max_shortlist": 15 }
        }
    """

    const val DECISION_UPDATE_PROMPT = """
        You are a risk committee for a trading desk. Your task is to decide which setups to keep or drop.
        
        Input:
        - Trading Intent: {intent}
        - Risk Tolerance: {risk}
        - Candidate Setups:
        {setupData}
        
        Guidelines:
        1. Keep at most {maxKeep} symbols that best fit the trading intent and risk tolerance.
        2. For each kept symbol, include:
           - "symbol": Ticker
           - "confidence": Score from 0.0 to 1.0
           - "setup_bias": "bullish", "bearish", or "neutral"
           - "must_review": List of specific items to verify (earnings, liquidity, catalyst, etc.)
           - "rss_needed": true if recent news is required before acting
           - "expanded_rss_needed": true if broader, multi-source context is required beyond core feeds
           - "expanded_rss_reason": short rationale for expanded feeds (optional)
        3. For dropped symbols, include reasons in "drop".
        4. "limits_applied": Include "max_keep" used.
        
        Output must be JSON only matching the following schema:
        {
          "keep": [
            {
              "symbol": "TICKER",
              "confidence": 0.85,
              "setup_bias": "bullish",
              "must_review": ["earnings date", "news catalyst"],
              "rss_needed": true,
              "expanded_rss_needed": false,
              "expanded_rss_reason": ""
            }
          ],
          "drop": [
            { "symbol": "TICKER", "reasons": ["low liquidity"] }
          ],
          "limits_applied": { "max_keep": 10 }
        }
    """

    const val FUNDAMENTALS_NEWS_SYNTHESIS_PROMPT = """
        You are an equity research editor. Using the setup data and recent headlines, produce a ranked review list and portfolio guidance.
        
        Input:
        - Trading Intent: {intent}
        - Risk Tolerance: {risk}
        - Setup Data:
        {setupData}
        - Recent RSS Headlines:
        {rssDigest}
        
        Guidelines:
        1. Provide "ranked_review_list" in priority order (highest priority first).
        2. Each item must include "symbol", "what_to_review", "risk_summary", and "one_paragraph_brief".
        3. Provide "portfolio_guidance" with "position_count" and "risk_posture" ("conservative", "moderate", or "aggressive").
        4. Output JSON only.
        
        Output schema (JSON ONLY):
        {
          "ranked_review_list": [
            {
              "symbol": "TICKER",
              "what_to_review": ["key fundamentals", "recent catalyst"],
              "risk_summary": ["risk 1", "risk 2"],
              "one_paragraph_brief": "..."
            }
          ],
          "portfolio_guidance": {
            "position_count": 5,
            "risk_posture": "moderate"
          }
        }
    """

    const val DEEP_DIVE_PROMPT = """
        You are a senior equity analyst performing a deep dive on {symbol}.
        Your goal is to find the most recent and relevant news, catalysts, and risks that are driving this stock's price action.
        
        Constraints:
        - Perform at most 3 web searches.
        - Look for information from the last 72 hours only.
        - Return ONLY a JSON object matching the schema below.
        - List all sources you used in the "sources" array.
        
        Input Context:
        - Ticker: {symbol}
        - User Intent: {intent}
        - Current Snapshot: {snapshot}
        - Recent RSS Headlines (already seen): {rssHeadlines}
        
        Output Schema (JSON ONLY):
        {
          "summary": "Concise summary of current drivers...",
          "drivers": [
            { "type": "news/earnings/macro", "direction": "bullish/bearish/neutral", "detail": "..." }
          ],
          "risks": ["Risk factor 1", "Risk factor 2"],
          "what_changes_my_mind": ["Specific event or level that would invalidate the current thesis"],
          "sources": [
            { "title": "...", "publisher": "...", "published_at": "...", "url": "..." }
          ]
        }
    """
}
