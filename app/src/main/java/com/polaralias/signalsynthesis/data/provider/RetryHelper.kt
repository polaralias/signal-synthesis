package com.polaralias.signalsynthesis.data.provider

import com.polaralias.signalsynthesis.util.Logger
import kotlinx.coroutines.delay
import java.io.IOException

data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 10000,
    val multiplier: Double = 2.0
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
        
        repeat(config.maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                
                // Only retry on IOExceptions or network-related issues
                val isRetryable = e is IOException
                
                if (!isRetryable || attempt == config.maxRetries - 1) {
                    throw e
                }
                
                Logger.w(tag, "Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms", e)
                
                delay(currentDelay)
                currentDelay = (currentDelay * config.multiplier).toLong().coerceAtMost(config.maxDelayMs)
            }
        }
        
        throw lastException ?: IllegalStateException("Retry failed without exception")
    }
}
