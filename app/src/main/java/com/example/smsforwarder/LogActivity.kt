package com.example.smsforwarder

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LogActivity : AppCompatActivity() {

    private lateinit var logText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        logText = findViewById(R.id.logText)

        findViewById<Button>(R.id.clearLogButton).setOnClickListener {
            FileLog.clear(this)
            refresh()
            Toast.makeText(this, "Log cleared", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    // Re-read on resume so returning to this page (e.g. after a new SMS arrives)
    // always shows the current contents.
    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        logText.text = FileLog.read(this)
    }
}
