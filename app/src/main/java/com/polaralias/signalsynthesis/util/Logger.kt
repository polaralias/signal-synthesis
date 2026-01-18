package com.polaralias.signalsynthesis.util

import android.util.Log

object Logger {
    private const val TAG = "SignalSynthesis"
    
    fun d(tag: String, message: String) {
        Log.d("$TAG:$tag", message)
    }
    
    fun i(tag: String, message: String) {
        Log.i("$TAG:$tag", message)
    }
    
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w("$TAG:$tag", message, throwable)
        } else {
            Log.w("$TAG:$tag", message)
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$TAG:$tag", message, throwable)
            CrashReporter.recordException(throwable)
        } else {
            Log.e("$TAG:$tag", message)
        }
    }
    
    fun event(name: String, params: Map<String, Any> = emptyMap()) {
        Log.d("$TAG:Event", "$name: $params")
    }
}
