package com.example.poc_sms_listener

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.Timer
import java.util.TimerTask

class CallbackPoller(private val context: Context) {
    private val client = OkHttpClient()

    private fun deleteCallback(callbackId: String) {
        // URL for DELETE /v1/callbacks/:id
        val url = "http://192.168.0.195:3000/v1/callbacks/$callbackId"

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

    data class Payload(
        @SerializedName("phoneNumber") val phoneNumber: String,
        @SerializedName("message") val message: String
    )

    data class CallbackItem(
        @SerializedName("id") val id: String,
        @SerializedName("payload") val payload: Payload,
    )

    data class CallbacksResponse(
        @SerializedName("data") val data: List<CallbackItem>
    )

    private fun handleCallbackQueue() {
        Log.d("SMSReceiver", "handleCallbackQueue")
        val url = "http://192.168.0.195:3000/v1/callbacks"

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
                                Log.d("SMSReceiver", "just saying, you good bro!")
                                handleCallBack(callback)
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

    fun handleCallBack(callbackItem: CallbackItem?) {
        Log.d("SMSReceiver", "handleCallBack")

        if (callbackItem == null) {
            Log.d("SMSReceiver", "callbackItem is null")
            return
        }

        Log.d("SMSReceiver", "${callbackItem.payload}")

        sendSmsMessage(context, "+5554999999999", "sender: ${callbackItem.payload.phoneNumber} \n msg: ${callbackItem.payload.message}")
        Log.d("SMSReceiver", "handleCallBack end")
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

        if (context == null) {
            Log.d("SMSReceiver", "Context is null")
        }
    }
}