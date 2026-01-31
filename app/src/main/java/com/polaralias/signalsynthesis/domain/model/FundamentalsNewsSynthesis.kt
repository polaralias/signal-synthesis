package com.polaralias.signalsynthesis.domain.model

import com.polaralias.signalsynthesis.util.JsonExtraction.toStringList
import com.polaralias.signalsynthesis.util.Logger
import org.json.JSONObject

data class FundamentalsNewsSynthesis(
    val rankedReviewList: List<RankedReviewItem> = emptyList(),
    val portfolioGuidance: PortfolioGuidance = PortfolioGuidance()
) {
    companion object {
        fun fromJson(json: String?): FundamentalsNewsSynthesis {
            if (json == null) return FundamentalsNewsSynthesis()
            return try {
                val obj = JSONObject(json)
                
                val rankedReviewList = mutableListOf<RankedReviewItem>()
                val reviewArray = obj.optJSONArray("ranked_review_list")
                if (reviewArray != null) {
                    for (i in 0 until reviewArray.length()) {
                        val itemObj = reviewArray.optJSONObject(i)
                        if (itemObj != null) {
                            rankedReviewList.add(RankedReviewItem.fromJson(itemObj))
                        }
                    }
                }

                val guidanceObj = obj.optJSONObject("portfolio_guidance")
                val portfolioGuidance = if (guidanceObj != null) {
                    PortfolioGuidance.fromJson(guidanceObj)
                } else {
                    PortfolioGuidance()
                }

                FundamentalsNewsSynthesis(rankedReviewList, portfolioGuidance)
            } catch (e: Exception) {
                Logger.e("FundamentalsNewsSynthesis", "Failed to parse fundamentals/news JSON", e)
                FundamentalsNewsSynthesis()
            }
        }
    }
}

data class RankedReviewItem(
    val symbol: String = "",
    val whatToReview: List<String> = emptyList(),
    val riskSummary: List<String> = emptyList(),
    val oneParagraphBrief: String = ""
) {
    companion object {
        fun fromJson(obj: JSONObject): RankedReviewItem {
            return RankedReviewItem(
                symbol = obj.optString("symbol", ""),
                whatToReview = obj.optJSONArray("what_to_review").toStringList(),
                riskSummary = obj.optJSONArray("risk_summary").toStringList(),
                oneParagraphBrief = obj.optString("one_paragraph_brief", "")
            )
        }
    }
}

data class PortfolioGuidance(
    val positionCount: Int = 0,
    val riskPosture: String = "moderate"
) {
    companion object {
        fun fromJson(obj: JSONObject): PortfolioGuidance {
            return PortfolioGuidance(
                positionCount = obj.optInt("position_count", 0),
                riskPosture = obj.optString("risk_posture", "moderate")
            )
        }
    }
}
