package com.polaralias.signalsynthesis.domain.ai

object AiPrompts {
    const val SYSTEM_ANALYST = "You are a senior trading analyst. Respond with JSON only."

    const val SETUP_SYNTHESIS_TEMPLATE = """
        Act as a senior trading analyst. Review the following setup and return JSON only.
        Important: When forming conclusions, explicitly reference the relevant indicators (e.g., RSI, VWAP, ATR, SMA), state their values, and explain how those values support or weaken the trade thesis.
        
        Output schema:
        {
          "summary": "Full detailed analysis referencing specific indicators and values",
          "risks": ["Specific risk identified from data"],
          "verdict": "Short final recommendation"
        }

        Ticker: {symbol}
        Setup: {setupType}
        Intent: {intent}
        Trigger: {triggerPrice}
        Stop: {stopLoss}
        Target: {targetPrice}
        Confidence: {confidence}
        Reasons: {reasons}
        
        Technical Indicators:
        {technicalIndicators}
        
        Context:
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
}
