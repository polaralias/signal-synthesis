package com.polaralias.signalsynthesis.domain.ai

object AiPrompts {
    const val SYSTEM_ANALYST = "You are a senior trading analyst. Respond with JSON only."

    const val STEP_1_DATA_ANALYSIS = """
        Analyze the following market data for {symbol}. 
        Provide a deep evaluation of:
        1. Technical Strength/Weakness (RS, VWAP, Trends).
        2. Fundamental Context (Valuation, Sector, Catalysts).
        3. Sentiment Profile.
        
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
        
        Data Analysis Report:
        {analysisReport}
        
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
}
