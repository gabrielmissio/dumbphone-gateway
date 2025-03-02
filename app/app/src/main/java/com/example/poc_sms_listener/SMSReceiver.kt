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
import java.util.Timer
import java.util.TimerTask

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

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

                            sendSmsMessage(context, phoneNumber, messageBody)
                            sendGetRequest()
                            // Create JSON payload with the sender and message content
                            val jsonPayload = """
                                {
                                    "sender": "$phoneNumber",
                                    "message": "$messageBody"
                                }
                            """.trimIndent()
                            sendWhatsappMessage(jsonPayload)
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

    private fun sendSmsMessage(context: Context?, phoneNumber: String?, text: String) {
        Log.d("SMSReceiver", "Trying to answer")
        // We need a non-null Context to safely use SmsManager
        context?.let {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(
                phoneNumber, // destinationAddress
                null,
                text, // body message
                null,
                null
            )
        }
    }

    private fun sendWhatsappMessage(jsonPayload: String) {
        Log.d("SMSReceiver", "sendPostRequest")
        val url = "http://192.168.0.220:3000/check"

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

    private fun handleCallbackQueue() {
        Log.d("SMSReceiver", "handleCallbackQueue")

        // Data classes representing the expected JSON structure.
        data class CallbackItem(
            @SerializedName("id") val id: String,
            @SerializedName("phoneNumber") val phoneNumber: String,
            @SerializedName("message") val message: String
        )

        data class CallbacksResponse(
            @SerializedName("data") val data: List<CallbackItem>
        )
        // call GET /v1/callbacks/
        // call sendSMSMessage to each callback (field data into the response body, its an array). then call DELETE /v1/callbacks/:id to remove the callback
        val url = "http://192.168.0.220:3000/v1/callbacks"

        // Build the GET request. (GET is the default method.)
        val request = Request.Builder()
            .url(url)
            .build()

        // Execute the request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Log or handle the error appropriately
                e.printStackTrace()
                Log.d("SMSReceiver", e.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        // Log unexpected response codes
                        Log.d("SMSReceiver","Unexpected code: ${it.code}")
                    } else {
                        // Process and log the response body
                        val responseBody = it.body?.string()
                        Log.d("SMSReceiver","Response: $responseBody")

                        // iterate over the array of callbacks

                        try {
                            // Parse the JSON response using Gson.
                            val gson = Gson()
                            val callbacksResponse = gson.fromJson(responseBody, CallbacksResponse::class.java)

                            // Iterate over each callback.
                            callbacksResponse.data.forEach { callback ->
                                // Call sendSMSMessage for each callback.
                                // sendSmsMessage()

                                Log.d("SMSReceiver", "just saying, you good bro!")

                                // Delete the callback after processing.
                                deleteCallback(callback.id)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    }
                }
            }
        })
    }

    fun schedulePeriodicHttpRequest() {
        Log.d("SMSReceiver", "calling schedulePeriodicHttpRequest")
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                Log.d("SMSReceiver", "Checking for callbacks")
                // This function will be called every 5 seconds
                handleCallbackQueue()
            }
        }, 0, 15000)  // delay 0ms, period 5000ms (5 seconds)
    }

    private fun sendGetRequest() {
        val url = "http://192.168.0.220:3000/check"

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
                        Log.d("SMSReceiver", "Unexpected code: ${it.code}")
                    } else {
                        // Process and log the response body
                        val responseBody = it.body?.string()
                        Log.d("SMSReceiver","Response: $responseBody")
                    }
                }
            }
        })
    }

    private fun deleteCallback(callbackId: String) {
        // URL for DELETE /v1/callbacks/:id
        val url = "http://192.168.0.220:3000/v1/callbacks/$callbackId"

        // Build the DELETE request.
        val request = Request.Builder()
            .url(url)
            .delete()
            .build()

        // Execute the DELETE request asynchronously.
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Log or handle the error appropriately
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        println("Failed to delete callback with id $callbackId: ${it.code}")
                    } else {
                        println("Callback with id $callbackId deleted successfully.")
                    }
                }
            }
        })
    }
}
