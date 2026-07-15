package com.example.smsforwarder

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Mirrors Log.d/i/w/e but also appends to a small file in app-private storage,
// so entries survive to be read from the settings screen later — Logcat alone
// is only visible while a PC is actively watching via adb at the moment the
// SMS arrives, which isn't realistic for a phone in daily use elsewhere.
object FileLog {
    private const val FILE_NAME = "debug_log.txt"
    private const val MAX_SIZE_BYTES = 200_000L
    private val timestampFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)

    fun d(context: Context, tag: String, msg: String) {
        Log.d(tag, msg)
        write(context, "D", tag, msg)
    }

    fun i(context: Context, tag: String, msg: String) {
        Log.i(tag, msg)
        write(context, "I", tag, msg)
    }

    fun w(context: Context, tag: String, msg: String) {
        Log.w(tag, msg)
        write(context, "W", tag, msg)
    }

    fun e(context: Context, tag: String, msg: String, throwable: Throwable? = null) {
        Log.e(tag, msg, throwable)
        val full = if (throwable != null) "$msg\n${Log.getStackTraceString(throwable)}" else msg
        write(context, "E", tag, full)
    }

    fun read(context: Context): String {
        val file = File(context.filesDir, FILE_NAME)
        return if (file.exists()) file.readText() else "(no log entries yet)"
    }

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }

    private fun write(context: Context, level: String, tag: String, msg: String) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            // Simple size cap so a personal device never accumulates an unbounded
            // log file — just start over rather than truncating mid-line.
            if (file.exists() && file.length() > MAX_SIZE_BYTES) {
                file.delete()
            }
            file.appendText("${timestampFormat.format(Date())} $level/$tag: $msg\n")
        } catch (_: Exception) {
        }
    }
}
