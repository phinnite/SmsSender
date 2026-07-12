package com.example.smsforwarder

import android.content.Context
import android.util.Log
import java.util.Properties
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {
    private const val TAG = "EmailSender"

    fun send(context: Context, sender: String, receivedOn: String, body: String) {
        val gmailAddress = Prefs.gmailAddress(context)
        val appPassword = Prefs.appPassword(context)
        val destEmail = Prefs.destEmail(context)

        if (gmailAddress.isBlank() || appPassword.isBlank() || destEmail.isBlank()) {
            Log.e(TAG, "Missing settings, cannot send email")
            return
        }

        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
        }

        val session = Session.getInstance(props, object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(gmailAddress, appPassword)
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(gmailAddress))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(destEmail))
                subject = "SMS from $sender to $receivedOn"
                setText("Received on: $receivedOn\n\n$body")
            }
            Transport.send(message)
            Log.i(TAG, "Email sent for SMS from $sender")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send email", e)
        }
    }
}
