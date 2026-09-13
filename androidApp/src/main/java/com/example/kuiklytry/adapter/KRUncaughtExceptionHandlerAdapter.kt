package com.example.kuiklytry.adapter

import android.content.pm.ApplicationInfo
import android.util.Log
import com.example.kuiklytry.KRApplication
import com.tencent.kuikly.core.render.android.adapter.IKRUncaughtExceptionHandlerAdapter

object KRUncaughtExceptionHandlerAdapter : IKRUncaughtExceptionHandlerAdapter {

    private const val TAG = "KRExceptionHandler"

    override fun uncaughtException(throwable: Throwable) {
        if (isDebuggable()) {
            throw throwable
        } else {
            Log.e(TAG, "KR error: ${throwable.stackTraceToString()}")
        }
    }

    /**
     * 是否为可调试（debug）构建。
     * AGP 8.0 起默认不再生成 BuildConfig，这里用 ApplicationInfo.FLAG_DEBUGGABLE 替代 BuildConfig.DEBUG。
     */
    private fun isDebuggable(): Boolean {
        val app = KRApplication.application
        return (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}
