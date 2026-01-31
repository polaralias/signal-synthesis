package com.polaralias.signalsynthesis.data.ai

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"

        fun create(client: OkHttpClient? = null): GeminiService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(client ?: OkHttpClient())
                .build()
            return retrofit.create(GeminiService::class.java)
        }
    }
}

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val tools: List<GeminiTool>? = null,
    val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig()
)

data class GeminiTool(
    @Json(name = "google_search") val googleSearch: GoogleSearchTool? = null
)

class GoogleSearchTool

data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String? = null
)

data class GeminiGenerationConfig(
    val temperature: Double = 0.2,
    val topP: Double = 0.8,
    val topK: Int = 40,
    val maxOutputTokens: Int = 1000,
    val responseMimeType: String = "application/json",
    @Json(name = "thinking_level") val thinkingLevel: String? = null,
    val thinkingBudget: Int? = null
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

data class GeminiCandidate(
    val content: GeminiContent? = null,
    val groundingMetadata: GeminiGroundingMetadata? = null
)

data class GeminiGroundingMetadata(
    val searchEntrypoint: GeminiSearchEntrypoint? = null,
    val groundingChunks: List<GeminiGroundingChunk>? = null,
    val groundingSupports: List<GeminiGroundingSupport>? = null,
    val webSearchQueries: List<String>? = null
)

data class GeminiSearchEntrypoint(
    val renderedContent: String? = null
)

data class GeminiGroundingChunk(
    val web: GeminiWebGroundingChunk? = null
)

data class GeminiWebGroundingChunk(
    val uri: String? = null,
    val title: String? = null
)

data class GeminiGroundingSupport(
    val segment: GeminiSegment? = null,
    val groundingChunkIndices: List<Int>? = null,
    val confidenceScores: List<Double>? = null
)

data class GeminiSegment(
    val startIndex: Int? = null,
    val endIndex: Int? = null,
    val text: String? = null
)
