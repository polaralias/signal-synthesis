package com.polaralias.signalsynthesis.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

enum class LogLevel { DEBUG, INFO, WARN, ERROR, EVENT }

data class LogEntry(
    val timestamp: Instant = Instant.now(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: String? = null
)

object Logger {
    private const val TAG = "SignalSynthesis"
    private const val MAX_LOGS = 200
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    
    fun d(tag: String, message: String) {
        Log.d("$TAG:$tag", message)
        addLog(LogLevel.DEBUG, tag, message)
    }
    
    fun i(tag: String, message: String) {
        Log.i("$TAG:$tag", message)
        addLog(LogLevel.INFO, tag, message)
    }
    
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w("$TAG:$tag", message, throwable)
        } else {
            Log.w("$TAG:$tag", message)
        }
        addLog(LogLevel.WARN, tag, message, throwable?.stackTraceToString())
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$TAG:$tag", message, throwable)
            CrashReporter.recordException(throwable)
        } else {
            Log.e("$TAG:$tag", message)
        }
        addLog(LogLevel.ERROR, tag, message, throwable?.stackTraceToString())
    }
    
    fun event(name: String, params: Map<String, Any> = emptyMap()) {
        val message = "$name: $params"
        Log.d("$TAG:Event", message)
        addLog(LogLevel.EVENT, "Event", message)
    }

    fun clear() {
        _logs.update { emptyList() }
    }

    private fun addLog(level: LogLevel, tag: String, message: String, throwable: String? = null) {
        _logs.update { current ->
            val newEntry = LogEntry(level = level, tag = tag, message = message, throwable = throwable)
            (listOf(newEntry) + current).take(MAX_LOGS)
        }
    }
}
