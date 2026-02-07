package com.polaralias.signalsynthesis.data.ai

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface OpenAiCompatibleService {
    @POST
    suspend fun createChatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse

    companion object {
        private const val PLACEHOLDER_URL = "https://localhost/"

        fun create(client: OkHttpClient? = null): OpenAiCompatibleService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(PLACEHOLDER_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(client ?: OkHttpClient())
                .build()
            return retrofit.create(OpenAiCompatibleService::class.java)
        }
    }
}
