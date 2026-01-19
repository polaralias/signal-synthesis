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

interface OpenAiService {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse

    companion object {
        private const val BASE_URL = "https://api.openai.com/"

        fun create(client: OkHttpClient? = null, baseUrl: String = BASE_URL): OpenAiService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(client ?: OkHttpClient())
                .build()
            return retrofit.create(OpenAiService::class.java)
        }
    }
}

data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    @Json(name = "max_output_tokens") val maxOutputTokens: Int? = null,
    val reasoning: OpenAiReasoning? = null,
    val text: OpenAiText? = null
)

data class OpenAiReasoning(
    val effort: String
)

data class OpenAiText(
    val verbosity: String
)

data class OpenAiMessage(
    val role: String,
    val content: String
)

data class OpenAiChatResponse(
    val choices: List<OpenAiChoice> = emptyList()
)

data class OpenAiChoice(
    val message: OpenAiMessage? = null
)
