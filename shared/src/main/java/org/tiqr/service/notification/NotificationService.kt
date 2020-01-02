package org.tiqr.service.notification

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import org.tiqr.BuildConfig
import org.tiqr.Utils
import org.tiqr.service.Token
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

open class NotificationService(private val context: Context) {

    open val notificationToken: String?
        get() {
            val settings = Prefs[context]
            return settings.getString(TOKEN_KEY, null)
        }

    open var shouldValidateExistingToken: Boolean
        get() =
            Prefs[context].getBoolean(SHOULD_VALIDATE_KEY, true)
        set(value) {
            val settings = Prefs[context]
            val editor = settings.edit()
            editor.putBoolean(SHOULD_VALIDATE_KEY, value)
            editor.apply()
        }

    fun requestNewToken(deviceToken: String) =
            Thread(Runnable {
                testableRequestNewToken(deviceToken)
            }).start()

    @VisibleForTesting
    fun testableRequestNewToken(deviceToken: String) {
        try {
            val nameValuePairs = createPostData(deviceToken)
            when (val newToken = requestToken(nameValuePairs)) {
                is Token.Valid -> saveNewToken(newToken.value)
                is Token.Invalid -> Log.e(TAG, "Failed to retrieve a new token")
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error retrieving device notification token", ex)
        }
    }

    @VisibleForTesting
    fun createPostData(deviceToken: String): HashMap<String, String> {
        if (notificationToken != null && shouldValidateExistingToken) {
            validateExistingToken()
            //perform validation only once
            shouldValidateExistingToken = false
        }
        val nameValuePairs = HashMap<String, String>()
        val notificationToken = notificationToken
        nameValuePairs["deviceToken"] = deviceToken
        if (notificationToken != null) {
            nameValuePairs["notificationToken"] = notificationToken
        }
        return nameValuePairs
    }

    @Throws(Exception::class)
    @VisibleForTesting
    open fun requestToken(nameValuePairs: HashMap<String, String>): Token {
        val tokenExchangeURL = URL(BuildConfig.TOKENEXCHANGE_URL)
        val httpsURLConnection = tokenExchangeURL.openConnection() as HttpsURLConnection
        httpsURLConnection.requestMethod = "POST"
        httpsURLConnection.doOutput = true
        val postData = Utils.keyValueMapToByteArray(nameValuePairs)
        httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        httpsURLConnection.setRequestProperty("Content-Length", postData.size.toString())
        httpsURLConnection.outputStream.write(postData)

        return Utils.urlConnectionResponse(httpsURLConnection)
    }

    @VisibleForTesting
    open fun saveNewToken(notificationToken: String?) {
        val settings = Prefs[context]
        val editor = settings.edit()
        editor.putString(TOKEN_KEY, notificationToken)
        editor.apply()
    }

    @VisibleForTesting
    fun validateExistingToken() {
        val nameValuePairs = HashMap<String, String>()
        nameValuePairs["notificationToken"] = notificationToken!!
        when (requestToken(nameValuePairs)) {
            is Token.Invalid -> saveNewToken(null)
            is Token.Valid -> {
                //NOP
            }
        }
    }

    companion object {
        const val TAG = "NotificationService"
        const val TOKEN_KEY = "sa_notificationToken"
        const val SHOULD_VALIDATE_KEY = "sa_shouldValidateExistingToken"
    }
}