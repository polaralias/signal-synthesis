package com.polaralias.signalsynthesis.data.ai

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAiResponsesService {
    @POST("v1/responses")
    suspend fun createResponse(
        @Header("Authorization") authorization: String,
        @Body request: OpenAiResponseRequest
    ): OpenAiResponseResponse

    companion object {
        private const val BASE_URL = "https://api.openai.com/"

        fun create(client: OkHttpClient? = null, baseUrl: String = BASE_URL): OpenAiResponsesService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(client ?: OkHttpClient())
                .build()
            return retrofit.create(OpenAiResponsesService::class.java)
        }
    }
}

data class OpenAiResponseRequest(
    val model: String,
    val input: String,
    val tools: List<OpenAiTool>? = null,
    val include: List<String>? = null,
    @Json(name = "reasoning_effort") val reasoningEffort: String? = null,
    val temperature: Float? = null
)

data class OpenAiTool(
    val type: String
)

data class OpenAiResponseResponse(
    val output: OpenAiResponseOutput? = null
)

data class OpenAiResponseOutput(
    val status: String? = null,
    val text: String? = null,
    @Json(name = "tool_calls") val toolCalls: List<OpenAiToolCall>? = null
)

data class OpenAiToolCall(
    val id: String? = null,
    val type: String? = null,
    @Json(name = "web_search_call") val webSearchCall: OpenAiWebSearchCall? = null
)

data class OpenAiWebSearchCall(
    val result: OpenAiWebSearchResult? = null,
    val action: OpenAiWebSearchAction? = null
)

data class OpenAiWebSearchResult(
    val sources: List<OpenAiWebSearchSource>? = null
)

data class OpenAiWebSearchAction(
    val queries: List<String>? = null,
    val sources: List<OpenAiWebSearchSource>? = null
)

data class OpenAiWebSearchSource(
    val title: String? = null,
    val url: String? = null,
    val publisher: String? = null,
    @Json(name = "published_at") val publishedAt: String? = null
)
