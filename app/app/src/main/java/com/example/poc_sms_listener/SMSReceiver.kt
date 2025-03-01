package com.example.poc_sms_listener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class SMSReceiver : BroadcastReceiver() {
    val client = OkHttpClient()

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("SMSReceiver", "NEW SMS!!!")

        if (intent?.action.equals("android.provider.Telephony.SMS_RECEIVED")) {
            val bundle: Bundle? = intent?.extras
            try {
                if (bundle != null) {
                    // SMS messages are stored in the 'pdus' field
                    val pdus = bundle["pdus"] as? Array<*>
                    if (!pdus.isNullOrEmpty()) {
                        for (pdu in pdus) {
                            val format = bundle["format"] as? String
                            val sms = SmsMessage.createFromPdu(pdu as ByteArray, format)
                            val phoneNumber = sms.originatingAddress
                            val messageBody = sms.messageBody

                            Log.d("SMSReceiver", "SMS from: $phoneNumber, body: $messageBody")
                            // Send a local broadcast to update the UI in MainActivity
                            val localIntent = Intent("com.example.poc_sms_listener.NEW_SMS").apply {
                                putExtra("EXTRA_SENDER", phoneNumber)
                                putExtra("EXTRA_BODY", messageBody)
                            }
                            context?.sendBroadcast(localIntent)

                            sendSmsResponse(context, phoneNumber, messageBody)

                            sendGetRequest()

                            // Create JSON payload with the sender and message content
                            val jsonPayload = """
                                {
                                    "sender": "$phoneNumber",
                                    "message": "$messageBody"
                                }
                            """.trimIndent()
                            sendPostRequest(jsonPayload)
                            // TODO: Perform your custom actions based on message data
                            // For example, call a function that handles this SMS logic
                            // handleIncomingSms(context, phoneNumber, messageBody)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SMSReceiver", "Exception in onReceive: ${e.message}")
            }
        }
    }

    // Helper to send an SMS response
    private fun sendSmsResponse(context: Context?, phoneNumber: String?, text: String) {
        Log.d("SMSReceiver", "Trying to answer")
        // We need a non-null Context to safely use SmsManager
        context?.let {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(
                phoneNumber,   // destinationAddress
                null,          // scAddress (service center, null uses default)
                text,          // text
                null,          // sentIntent
                null           // deliveryIntent
            )
        }
    }

    private fun sendGetRequest() {
        val url = "http://192.168.0.195:3000/check"

        // Build the GET request. (GET is the default method.)
        val request = Request.Builder()
            .url(url)
            .build()

        // Execute the request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Log or handle the error appropriately
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        // Log unexpected response codes
                        println("Unexpected code: ${it.code}")
                    } else {
                        // Process and log the response body
                        val responseBody = it.body?.string()
                        println("Response: $responseBody")
                    }
                }
            }
        })
    }

    private fun sendPostRequest(jsonPayload: String) {
        Log.d("SMSReceiver", "sendPostRequest")
        val url = "http://192.168.0.195:3000/check"

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("SMSReceiver HTTP", e.toString())
                // Handle the error appropriately in your app
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        // Handle error response code
                        Log.d("SMSReceiver HTTP", "Unexpected code $response")
                    } else {
                        Log.d("SMSReceiver HTTP", "Response: ${it.body?.string()}")
                    }
                }
            }
        })
    }
}
