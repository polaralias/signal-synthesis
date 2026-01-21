package com.polaralias.signalsynthesis.domain.ai

object AiPrompts {
    const val SYSTEM_ANALYST = "You are a senior trading analyst. Respond with JSON only."

    const val SETUP_SYNTHESIS_TEMPLATE = """
        Act as an elite quantitative trading analyst. Review the provided market data and synthesize it into a high-conviction trade thesis.
        
        Mandatory Guidelines:
        1. Explicitly reference technical indicators (RSI, VWAP, ATR, SMA) and their current values.
        2. Evaluate the risk-to-reward ratio based on the trigger, stop-loss, and target prices.
        3. Consider the trading 'Intent' ({intent}) and ensure your analysis matches the horizon.
        4. In the 'summary', provide a professional narrative explaining *why* this setup is unique or standard.
        5. In 'risks', list specific environmental or technical factors that could invalidate the thesis.
        
        Output schema (JSON ONLY):
        {
          "summary": "Professional narrative synthesis of technicals, fundamentals, and sentiment.",
          "risks": ["Specific invalidation point 1", "Specific invalidation point 2"],
          "verdict": "Clear, decisive recommendation (e.g., 'Strong Buy on Dip', 'Avoid - Low Confluence')"
        }

        Ticker: {symbol}
        Setup: {setupType}
        Trigger: {triggerPrice}
        Stop: {stopLoss}
        Target: {targetPrice}
        Confidence: {confidence}
        Reasons: {reasons}
        
        Technical Indicators:
        {technicalIndicators}
        
        Context & Fundamentals:
        {context}
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
