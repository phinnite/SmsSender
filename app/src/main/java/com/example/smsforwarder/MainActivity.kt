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
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var gmailAddressField: EditText
    private lateinit var appPasswordField: EditText
    private lateinit var keywordsField: EditText
    private lateinit var lineLabelField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gmailAddressField = findViewById(R.id.gmailAddressField)
        appPasswordField = findViewById(R.id.appPasswordField)
        keywordsField = findViewById(R.id.keywordsField)
        lineLabelField = findViewById(R.id.lineLabelField)

        // The last field sits low enough that the keyboard covers it. The
        // framework's auto-scroll-on-focus fires before the keyboard finishes
        // animating in, so it under-scrolls; re-scroll to the bottom once the
        // keyboard has settled. The generous bottom padding in the layout gives
        // this scroll room to lift the field (and its helper text) clear of the
        // keyboard. smoothScrollTo clamps to the max and doesn't steal focus the
        // way fullScroll() would.
        val settingsScroll = findViewById<ScrollView>(R.id.settingsScroll)
        lineLabelField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                settingsScroll.postDelayed({
                    settingsScroll.smoothScrollTo(0, settingsScroll.getChildAt(0).height)
                }, 100)
            }
        }

        gmailAddressField.setText(Prefs.gmailAddress(this))
        appPasswordField.setText(Prefs.appPassword(this))
        keywordsField.setText(Prefs.keywords(this))
        lineLabelField.setText(Prefs.lineLabel(this))

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            // One email field now feeds both roles: the SMTP sender/auth account
            // and the forwarding destination are always the same address.
            val email = gmailAddressField.text.toString().trim()
            Prefs.save(
                this,
                email,
                appPasswordField.text.toString().trim(),
                email,
                keywordsField.text.toString().trim(),
                lineLabelField.text.toString().trim()
            )
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.requestPermissionsButton).setOnClickListener {
            onGrantPermissionsClicked()
        }

        findViewById<Button>(R.id.batteryOptimizationButton).setOnClickListener {
            requestIgnoreBatteryOptimizations()
        }

        findViewById<Button>(R.id.viewLogButton).setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
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

    private val requiredPermissions = listOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_PHONE_NUMBERS
    )

    private fun neededPermissions(): List<String> =
        requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

    // Called automatically at startup: just fires the system dialog for any
    // missing permission. If a permission was permanently denied, the OS
    // silently ignores this — recovery lives in onGrantPermissionsClicked().
    private fun requestSmsPermissions() {
        val needed = neededPermissions()
        if (needed.isNotEmpty()) {
            Prefs.setPermissionsRequested(this)
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
        }
    }

    // Called by the Grant Permissions button. Unlike startup, this handles the
    // permanently-denied case: once a permission is denied for good, the system
    // dialog never reappears, so the only fix is the app's settings page.
    private fun onGrantPermissionsClicked() {
        val needed = neededPermissions()
        if (needed.isEmpty()) {
            Toast.makeText(this, "All permissions already granted", Toast.LENGTH_SHORT).show()
            return
        }

        // shouldShowRequestPermissionRationale is false both before the first
        // ask AND after a permanent denial — the saved "have we asked" flag is
        // what disambiguates the two. If we've asked before and the OS still
        // won't show a rationale, it's permanently denied.
        val permanentlyDenied = Prefs.permissionsRequested(this) &&
            needed.any { !ActivityCompat.shouldShowRequestPermissionRationale(this, it) }

        if (permanentlyDenied) {
            Toast.makeText(
                this,
                "Permission was denied earlier — enable it here in app settings",
                Toast.LENGTH_LONG
            ).show()
            openAppSettings()
        } else {
            requestSmsPermissions()
        }
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }
}
