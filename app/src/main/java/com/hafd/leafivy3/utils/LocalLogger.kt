package com.hafd.leafivy3.utils

import android.content.Context
import android.util.Log
import com.hafd.leafivy3.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalLogger {
    private const val LOG_DIR = "logs"
    private const val APP_LOG_FILE = "app.log"
    private const val MAX_APP_LOG_SIZE_BYTES = 1_048_576L
    private const val MAX_ARCHIVED_LOGS = 3
    private val lock = Any()
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun d(tag: String, message: String) = log("D", tag, message, null)
    fun i(tag: String, message: String) = log("I", tag, message, null)
    fun w(tag: String, message: String, throwable: Throwable? = null) = log("W", tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log("E", tag, message, throwable)

    fun logCrash(tag: String, message: String, throwable: Throwable) {
        log("E", tag, message, throwable, crashOnly = true)
    }

    private fun log(
        level: String,
        tag: String,
        message: String,
        throwable: Throwable?,
        crashOnly: Boolean = false
    ) {
        if (shouldEmitLogcat(level)) {
            when (level) {
                "D" -> Log.d(tag, message, throwable)
                "I" -> Log.i(tag, message, throwable)
                "W" -> Log.w(tag, message, throwable)
                "E" -> Log.e(tag, message, throwable)
                else -> Log.d(tag, message, throwable)
            }
        }

        if (!shouldPersistLog(level, crashOnly)) return

        val context = appContext ?: return
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val line = buildString {
            append(timestamp)
            append(" ")
            append(level)
            append("/")
            append(tag)
            append(": ")
            append(message)
            if (throwable != null) {
                append("\n")
                append(Log.getStackTraceString(throwable))
            }
            append("\n")
        }

        synchronized(lock) {
            runCatching {
                val logDir = File(context.filesDir, LOG_DIR)
                if (!logDir.exists()) {
                    logDir.mkdirs()
                }
                if (crashOnly) {
                    val crashFile = File(logDir, "crash_${System.currentTimeMillis()}.log")
                    crashFile.appendText(line)
                } else {
                    val appFile = File(logDir, APP_LOG_FILE)
                    rotateAppLogIfNeeded(logDir, appFile)
                    appFile.appendText(line)
                }
            }.onFailure {
                if (BuildConfig.DEBUG) {
                    Log.w("LocalLogger", "Failed to persist log line", it)
                }
            }
        }
    }

    private fun rotateAppLogIfNeeded(logDir: File, appFile: File) {
        if (!appFile.exists() || appFile.length() < MAX_APP_LOG_SIZE_BYTES) return

        val archivedName = "app_${System.currentTimeMillis()}.log"
        appFile.renameTo(File(logDir, archivedName))

        val archives = logDir.listFiles { file ->
            file.name.startsWith("app_") && file.name.endsWith(".log")
        }?.sortedByDescending { it.lastModified() }.orEmpty()

        archives.drop(MAX_ARCHIVED_LOGS).forEach { it.delete() }
    }

    private fun shouldEmitLogcat(level: String): Boolean {
        return BuildConfig.DEBUG || level == "W" || level == "E"
    }

    private fun shouldPersistLog(level: String, crashOnly: Boolean): Boolean {
        if (crashOnly) return true
        if (BuildConfig.ENABLE_FILE_LOGS) return true
        return level == "W" || level == "E"
    }
}
