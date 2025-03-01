package com.example.poc_sms_listener

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.poc_sms_listener.ui.theme.POCSMSlistenerTheme

class MainActivity : ComponentActivity() {

    // Minimal permission request code
    private val SMS_PERMISSION_CODE = 100

    // Compose state to hold the last received SMS
    // We start with a default "No messages yet".
    private val _lastSmsState = mutableStateOf("No messages yet")

    // BroadcastReceiver that listens for new SMS broadcast from SMSReceiver
    private val smsDebugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.poc_sms_listener.NEW_SMS") {
                val sender = intent.getStringExtra("EXTRA_SENDER") ?: "unknown"
                val body = intent.getStringExtra("EXTRA_BODY") ?: "no content"
                _lastSmsState.value = "From: $sender\n$body"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request SMS permissions if needed
        requestSmsPermissionsIfNeeded()

        enableEdgeToEdge()
        setContent {
            POCSMSlistenerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Pass the current value of our state to the UI
                    GreetingWithSms(
                        name = "Satoshi",
                        latestSms = _lastSmsState.value,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter("com.example.poc_sms_listener.NEW_SMS")

        // For Android 13+ (API 33), we must specify if the receiver is exported or not
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // We don't want external apps sending this broadcast to us, so we choose NOT_EXPORTED.
            // If you do want to allow other apps to send this broadcast, use RECEIVER_EXPORTED.
            registerReceiver(smsDebugReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(smsDebugReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister to avoid leaks
        unregisterReceiver(smsDebugReceiver)
    }

    // Minimal helper method to check and request SMS permissions
    private fun requestSmsPermissionsIfNeeded() {
        val sendSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
        val readSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
        val receiveSms = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)

        if (sendSms != PackageManager.PERMISSION_GRANTED ||
            readSms != PackageManager.PERMISSION_GRANTED ||
            receiveSms != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS
                ),
                SMS_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All SMS-related permissions are granted
                Log.d("MainActivity", "Permissions are granted")

            } else {
                // At least one permission was denied
                Log.d("MainActivity", "Permissions was denied")
            }
        }
    }
}

@Composable
fun GreetingWithSms(name: String, latestSms: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Hello $name!")
        Text(text = "Last SMS:\n$latestSms")
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    POCSMSlistenerTheme {
        GreetingWithSms(name = "Android", latestSms = "No messages yet")
    }
}
