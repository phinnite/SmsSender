package com.example.smsforwarder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var gmailAddressField: EditText
    private lateinit var appPasswordField: EditText
    private lateinit var destEmailField: EditText
    private lateinit var keywordsField: EditText
    private lateinit var lineLabelField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gmailAddressField = findViewById(R.id.gmailAddressField)
        appPasswordField = findViewById(R.id.appPasswordField)
        destEmailField = findViewById(R.id.destEmailField)
        keywordsField = findViewById(R.id.keywordsField)
        lineLabelField = findViewById(R.id.lineLabelField)

        gmailAddressField.setText(Prefs.gmailAddress(this))
        appPasswordField.setText(Prefs.appPassword(this))
        destEmailField.setText(Prefs.destEmail(this))
        keywordsField.setText(Prefs.keywords(this))
        lineLabelField.setText(Prefs.lineLabel(this))

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            Prefs.save(
                this,
                gmailAddressField.text.toString().trim(),
                appPasswordField.text.toString().trim(),
                destEmailField.text.toString().trim(),
                keywordsField.text.toString().trim(),
                lineLabelField.text.toString().trim()
            )
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.requestPermissionsButton).setOnClickListener {
            requestSmsPermissions()
        }

        findViewById<Button>(R.id.batteryOptimizationButton).setOnClickListener {
            requestIgnoreBatteryOptimizations()
        }

        requestSmsPermissions()
    }

    private fun requestIgnoreBatteryOptimizations() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(this, "Already exempted from battery optimization", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun requestSmsPermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.RECEIVE_SMS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.READ_SMS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.READ_PHONE_STATE)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.READ_PHONE_NUMBERS)

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
        }
    }
}
