package com.example.smsforwarder

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsMessage
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        val format = bundle.getString("format")
        val receivedOn = resolveReceivingLine(context, bundle)

        // goAsync() lets us keep working briefly after onReceive returns,
        // since network calls can't run directly on this thread in time.
        val pendingResult = goAsync()

        Thread {
            try {
                // A long SMS arrives as multiple concatenated segments in one
                // broadcast (one pdu each, already in order) — join them into
                // a single logical message instead of treating each as its own.
                val segments = pdus.map { SmsMessage.createFromPdu(it as ByteArray, format) }
                val sender = segments.firstOrNull()?.originatingAddress ?: "unknown"
                val body = segments.joinToString("") { it.messageBody ?: "" }

                if (shouldForward(context, sender, body)) {
                    EmailSender.send(context, sender, receivedOn, body)
                }
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    // Best-effort: the actual phone number is often unavailable (many carriers
    // never provision it onto the SIM, even with permission granted), so this
    // falls back to a SIM slot + carrier label to at least distinguish lines.
    private fun resolveReceivingLine(context: Context, bundle: android.os.Bundle): String {
        // A manually configured label always wins — it's the only reliable
        // source when the carrier never provisioned the number anywhere.
        Prefs.lineLabel(context).takeIf { it.isNotBlank() }?.let { return it }

        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return "Unknown line"

        val subId = bundle.getInt("subscription", -1).let {
            if (it != -1) it else SubscriptionManager.getDefaultSmsSubscriptionId()
        }

        return try {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val info = subManager.getActiveSubscriptionInfo(subId) ?: return "Unknown line"

            val number = resolveNumber(subManager, subId, info)
            if (!number.isNullOrBlank()) {
                number
            } else {
                // carrierName/displayName can be empty (not just null) on some
                // devices, which used to render as "SIM 1 ( )".
                val carrier = sequenceOf(info.carrierName, info.displayName)
                    .mapNotNull { it?.toString() }
                    .firstOrNull { it.isNotBlank() } ?: "unknown carrier"
                "SIM ${info.simSlotIndex + 1} ($carrier)"
            }
        } catch (e: SecurityException) {
            "Unknown line"
        }
    }

    private fun resolveNumber(subManager: SubscriptionManager, subId: Int, info: SubscriptionInfo): String? {
        // Android 13+ can pull the number from carrier/IMS sources too, not
        // just the SIM record, so it succeeds on some SIMs where the legacy
        // field is empty.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                subManager.getPhoneNumber(subId).takeIf { it.isNotBlank() }?.let { return it }
            } catch (_: Exception) {
            }
        }
        @Suppress("DEPRECATION")
        return info.number
    }

    private fun shouldForward(context: Context, sender: String, body: String): Boolean {
        val keywordsRaw = Prefs.keywords(context)
        if (keywordsRaw.isBlank()) return true // no filter set = forward everything

        val keywords = keywordsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return keywords.any { sender.contains(it, ignoreCase = true) || body.contains(it, ignoreCase = true) }
    }
}
