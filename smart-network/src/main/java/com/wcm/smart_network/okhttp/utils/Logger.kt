package com.wcm.smart_network.okhttp.utils

import android.util.Log

object Logger {
    private const val TAG = "SmartNetwork"

    fun debug(message: String) {
        Log.d(TAG, "[DEBUG] $message")
    }

    fun info(message: String) {
        Log.i(TAG, "[INFO] $message")
    }

    fun warn(message: String, e: Exception? = null) {
        if (e != null) Log.w(TAG, "[WARN] $message", e)
        else Log.w(TAG, "[WARN] $message")
    }

    fun error(message: String, e: Exception) {
        Log.e(TAG, "[ERROR] $message", e)
    }
}
