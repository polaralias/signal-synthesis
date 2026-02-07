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
    val input: List<OpenAiInputMessage>,
    val instructions: String? = null,
    @Json(name = "max_output_tokens") val maxOutputTokens: Int? = null,
    val text: OpenAiTextConfig? = null,
    val tools: List<OpenAiTool>? = null,
    val include: List<String>? = null,
    val reasoning: OpenAiReasoning? = null,
    val temperature: Float? = null
)

data class OpenAiTextConfig(
    val format: OpenAiTextFormat? = null
)

data class OpenAiTextFormat(
    val type: String
)

data class OpenAiInputMessage(
    val role: String,
    val content: String
)

data class OpenAiReasoning(
    val effort: String? = null
)

data class OpenAiTool(
    val type: String
)

data class OpenAiResponseResponse(
    val output: List<OpenAiResponseOutputItem> = emptyList()
)

data class OpenAiResponseOutputItem(
    val type: String? = null,
    val text: String? = null,
    val content: List<OpenAiResponseContentItem>? = null,
    @Json(name = "web_search_call") val webSearchCall: OpenAiWebSearchCall? = null,
    val action: OpenAiWebSearchAction? = null
)

data class OpenAiResponseContentItem(
    val type: String? = null,
    val text: String? = null,
    val annotations: List<OpenAiAnnotation>? = null
)

data class OpenAiAnnotation(
    val type: String? = null,
    val title: String? = null,
    val url: String? = null
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

fun OpenAiResponseResponse.extractText(): String {
    val outputText = output
        .asSequence()
        .filter { it.type == "message" }
        .flatMap { item -> (item.content ?: emptyList()).asSequence() }
        .firstOrNull { it.type == "output_text" && !it.text.isNullOrBlank() }
        ?.text
        .orEmpty()

    if (outputText.isNotBlank()) {
        return outputText
    }

    val genericMessageText = output
        .asSequence()
        .filter { it.type == "message" }
        .flatMap { item -> (item.content ?: emptyList()).asSequence() }
        .firstOrNull { !it.text.isNullOrBlank() }
        ?.text
        .orEmpty()

    if (genericMessageText.isNotBlank()) {
        return genericMessageText
    }

    return output
        .asSequence()
        .mapNotNull { it.text }
        .firstOrNull()
        .orEmpty()
}

fun OpenAiResponseResponse.extractSources(): List<OpenAiWebSearchSource> {
    val fromToolCalls = output.flatMap { item ->
        val viaCall = item.webSearchCall?.result?.sources.orEmpty() + item.webSearchCall?.action?.sources.orEmpty()
        val viaAction = item.action?.sources.orEmpty()
        viaCall + viaAction
    }

    val fromAnnotations = output
        .flatMap { it.content.orEmpty() }
        .flatMap { content ->
            content.annotations.orEmpty().mapNotNull { annotation ->
                val url = annotation.url ?: return@mapNotNull null
                OpenAiWebSearchSource(
                    title = annotation.title ?: "Source",
                    url = url,
                    publisher = null,
                    publishedAt = null
                )
            }
        }

    return (fromToolCalls + fromAnnotations)
        .filter { !it.url.isNullOrBlank() }
        .distinctBy { it.url }
}
