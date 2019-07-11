package org.tiqr.service.notification

import android.content.Context
import android.util.Log

import org.tiqr.BuildConfig
import org.tiqr.Utils

import java.net.URL
import java.util.HashMap

import javax.net.ssl.HttpsURLConnection

class NotificationService(private val _context: Context) {

    val notificationToken: String?
        get() {
            val settings = Prefs.get(_context)
            return settings.getString("sa_notificationToken", null)
        }

    @Throws(Exception::class)
    private fun _sendRequestWithDeviceToken(deviceToken: String) {
        var notificationToken = notificationToken

        val nameValuePairs = HashMap<String, String>()
        nameValuePairs["deviceToken"] = deviceToken
        if (notificationToken != null) {
            nameValuePairs["notificationToken"] = notificationToken
        }

        val tokenExchangeURL = URL(TOKENEXCHANGE_URL)
        val httpsURLConnection = tokenExchangeURL.openConnection() as HttpsURLConnection
        httpsURLConnection.requestMethod = "POST"
        httpsURLConnection.doOutput = true
        val postData = Utils.keyValueMapToByteArray(nameValuePairs)
        httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        httpsURLConnection.setRequestProperty("Content-Length", postData.size.toString())
        httpsURLConnection.outputStream.write(postData)

        notificationToken = Utils.urlConnectionResponseAsString(httpsURLConnection)

        Log.d(NotificationService::class.java.simpleName, "Notification token: " + notificationToken!!)

        val settings = Prefs.get(_context)
        val editor = settings.edit()
        editor.putString("sa_notificationToken", notificationToken)
        editor.commit()
    }

    fun sendRequestWithDeviceToken(deviceToken: String) {
        Thread(Runnable {
            try {
                _sendRequestWithDeviceToken(deviceToken)
            } catch (ex: Exception) {
                Log.e(NotificationService::class.java.simpleName, "Error retrieving device notification token", ex)
            }
        }).start()
    }

    companion object {

        private val TOKENEXCHANGE_URL = BuildConfig.TOKENEXCHANGE_URL
    }
}