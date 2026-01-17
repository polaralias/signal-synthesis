package com.polaralias.signalsynthesis.data.provider

import com.polaralias.signalsynthesis.domain.provider.DailyProvider
import com.polaralias.signalsynthesis.domain.provider.IntradayProvider
import com.polaralias.signalsynthesis.domain.provider.MetricsProvider
import com.polaralias.signalsynthesis.domain.provider.ProfileProvider
import com.polaralias.signalsynthesis.domain.provider.QuoteProvider
import com.polaralias.signalsynthesis.domain.provider.SentimentProvider

data class ProviderBundle(
    val quoteProviders: List<QuoteProvider>,
    val intradayProviders: List<IntradayProvider>,
    val dailyProviders: List<DailyProvider>,
    val profileProviders: List<ProfileProvider>,
    val metricsProviders: List<MetricsProvider>,
    val sentimentProviders: List<SentimentProvider>
) {
    fun isEmpty(): Boolean {
        return quoteProviders.isEmpty() &&
            intradayProviders.isEmpty() &&
            dailyProviders.isEmpty() &&
            profileProviders.isEmpty() &&
            metricsProviders.isEmpty() &&
            sentimentProviders.isEmpty()
    }

    companion object {
        fun empty(): ProviderBundle {
            return ProviderBundle(
                quoteProviders = emptyList(),
                intradayProviders = emptyList(),
                dailyProviders = emptyList(),
                profileProviders = emptyList(),
                metricsProviders = emptyList(),
                sentimentProviders = emptyList()
            )
        }
    }
}
