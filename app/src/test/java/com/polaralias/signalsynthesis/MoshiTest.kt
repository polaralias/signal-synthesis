package com.polaralias.signalsynthesis

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Test
import java.io.File

data class OpenAiChatRequest(
    val model: String,
    @Json(name = "max_tokens") val maxTokens: Int? = null,
    val temperature: Float? = null
)

class MoshiTest {
    @Test
    fun testMoshiNulls() {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(OpenAiChatRequest::class.java)
        val request = OpenAiChatRequest("gpt-3.5", null, null)
        val json = adapter.toJson(request)
        File("test_moshi_output.txt").writeText(json)
        assert(true)
    }
}
