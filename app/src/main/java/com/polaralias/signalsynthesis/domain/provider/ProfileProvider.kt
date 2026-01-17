package com.polaralias.signalsynthesis.domain.provider

import com.polaralias.signalsynthesis.domain.model.CompanyProfile

interface ProfileProvider {
    suspend fun getProfile(symbol: String): CompanyProfile?
}
