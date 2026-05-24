package com.polaralias.signalsynthesis.data.ai

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface AnthropicService {
    @GET("v1/models")
    suspend fun listModels(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") anthropicVersion: String = "2023-06-01"
    ): AnthropicModelListResponse

    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") anthropicVersion: String = "2023-06-01",
        @Body request: AnthropicRequest
    ): AnthropicResponse

    companion object {
        private const val BASE_URL = "https://api.anthropic.com/"

        fun create(client: OkHttpClient? = null, baseUrl: String = BASE_URL): AnthropicService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val httpClient = client ?: OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .callTimeout(120, TimeUnit.SECONDS)
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(httpClient)
                .build()
            return retrofit.create(AnthropicService::class.java)
        }
    }
}

data class AnthropicRequest(
    val model: String,
    @Json(name = "max_tokens") val maxTokens: Int,
    val messages: List<AnthropicMessage>,
    val system: String? = null,
    val temperature: Float? = null
)

data class AnthropicMessage(
    val role: String,
    val content: String
)

data class AnthropicResponse(
    val content: List<AnthropicContentBlock> = emptyList()
)

data class AnthropicContentBlock(
    @Json(name = "type") val type: String? = null,
    val text: String? = null
)

data class AnthropicModelListResponse(
    val data: List<AnthropicModelInfo> = emptyList()
)

data class AnthropicModelInfo(
    val id: String,
    @Json(name = "display_name") val displayName: String? = null,
    val type: String? = null
)
