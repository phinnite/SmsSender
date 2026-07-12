package com.example.smsforwarder

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val FILE = "sms_forwarder_prefs"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun save(context: Context, gmailAddress: String, appPassword: String, destEmail: String, keywords: String, lineLabel: String) {
        prefs(context).edit()
            .putString("gmail_address", gmailAddress)
            .putString("app_password", appPassword)
            .putString("dest_email", destEmail)
            .putString("keywords", keywords)
            .putString("line_label", lineLabel)
            .apply()
    }

    fun gmailAddress(context: Context) = prefs(context).getString("gmail_address", "") ?: ""
    fun appPassword(context: Context) = prefs(context).getString("app_password", "") ?: ""
    fun destEmail(context: Context) = prefs(context).getString("dest_email", "") ?: ""
    fun keywords(context: Context) = prefs(context).getString("keywords", "") ?: ""
    fun lineLabel(context: Context) = prefs(context).getString("line_label", "") ?: ""
}
