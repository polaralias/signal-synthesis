package com.polaralias.signalsynthesis.util

import android.util.Log

object CrashReporter {
    private var isEnabled = false
    
    fun init(enabled: Boolean) {
        isEnabled = enabled
        if (enabled) {
            Log.i("CrashReporter", "Initialized (Mock Mode)")
        }
    }
    
    fun recordException(throwable: Throwable) {
        if (!isEnabled) return
        Log.e("CrashReporter", "Exception recorded", throwable)
    }
    
    fun log(message: String) {
        if (!isEnabled) return
        Log.i("CrashReporter", "Log: $message")
    }
}
