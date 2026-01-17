package com.signalsynthesis.domain.provider

import com.signalsynthesis.domain.model.CompanyProfile

interface ProfileProvider {
    suspend fun getProfile(symbol: String): CompanyProfile?
}
