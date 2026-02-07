package com.polaralias.signalsynthesis.data.provider

import com.polaralias.signalsynthesis.util.Logger
import kotlinx.coroutines.delay
import java.io.IOException
import retrofit2.HttpException

data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 10000,
    val multiplier: Double = 2.0,
    val rateLimitDelayMs: Long = 60_000
)

object RetryHelper {
    private val defaultConfig = RetryConfig()
    
    suspend fun <T> withRetry(
        tag: String,
        config: RetryConfig = defaultConfig,
        block: suspend () -> T
    ): T {
        var currentDelay = config.initialDelayMs
        var lastException: Throwable? = null
        var attempts = 0
        
        while (attempts < config.maxRetries) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e

                if (e is HttpException && e.code() == 429) {
                    if (attempts == config.maxRetries - 1) {
                        throw e
                    }
                    attempts += 1
                    val retryAfterHeader = e.response()?.headers()?.get("Retry-After")?.toLongOrNull()
                    val delayMs = retryAfterHeader?.times(1000) ?: config.rateLimitDelayMs
                    Logger.w(tag, "Rate limited (429). Attempt $attempts/${config.maxRetries}; waiting ${delayMs}ms before retrying.", e)
                    delay(delayMs)
                    continue
                }
                
                // Only retry on IOExceptions or network-related issues
                val isRetryable = e is IOException
                
                if (!isRetryable || attempts == config.maxRetries - 1) {
                    throw e
                }
                
                Logger.w(tag, "Attempt ${attempts + 1} failed, retrying in ${currentDelay}ms", e)
                
                delay(currentDelay)
                currentDelay = (currentDelay * config.multiplier).toLong().coerceAtMost(config.maxDelayMs)
                attempts += 1
            }
        }
        
        throw lastException ?: IllegalStateException("Retry failed without exception")
    }
}
