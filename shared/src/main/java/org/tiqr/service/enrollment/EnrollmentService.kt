package org.tiqr.service.enrollment

import android.content.Context
import android.os.AsyncTask
import android.util.Log

import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import org.tiqr.Constants
import org.tiqr.Utils
import org.tiqr.R
import org.tiqr.authenticator.auth.EnrollmentChallenge
import org.tiqr.authenticator.datamodel.DbAdapter
import org.tiqr.authenticator.datamodel.Identity
import org.tiqr.authenticator.datamodel.IdentityProvider
import org.tiqr.authenticator.exceptions.SecurityFeaturesException
import org.tiqr.authenticator.exceptions.UserException
import org.tiqr.authenticator.security.Encryption
import org.tiqr.authenticator.security.Secret
import org.tiqr.service.enrollment.EnrollmentError.Type
import org.tiqr.service.notification.NotificationService

import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.util.HashMap
import java.util.Locale

import javax.crypto.SecretKey

/**
 * Enrollment data service.
 */
class EnrollmentService(internal val context: Context,
                        internal val notificationService: NotificationService,
                        internal val dbAdapter: DbAdapter) {

    private val TAG = EnrollmentService::class.java.name

    interface OnParseEnrollmentChallengeListener {
        fun onParseEnrollmentChallengeSuccess(challenge: EnrollmentChallenge)

        fun onParseEnrollmentChallengeError(error: ParseEnrollmentChallengeError)
    }

    interface OnEnrollmentListener {
        fun onEnrollmentSuccess()

        fun onEnrollmentError(error: EnrollmentError?)
    }

    /**
     * Contains an enrollment challenge?
     *
     * @param rawChallenge Raw challenge.
     * @return Is enrollment challenge?
     */
    fun isEnrollmentChallenge(rawChallenge: String): Boolean {
        return rawChallenge.startsWith("tiqrenroll://")
    }

    /**
     * Parses the raw enrollment challenge.
     *
     * @param rawChallenge Raw challenge.
     * @param listener     Completion listener.
     */
    fun parseEnrollmentChallenge(rawChallenge: String, listener: OnParseEnrollmentChallengeListener): AsyncTask<*, *, *> {
        val task = object : AsyncTask<Void, Void, Any>() {
            override fun doInBackground(vararg voids: Void): Any {

                if (!rawChallenge.startsWith("tiqrenroll://")) {
                    return ParseEnrollmentChallengeError(ParseEnrollmentChallengeError.Type.INVALID_CHALLENGE, context.getString(R.string.enrollment_failure_title), context.getString(R.string.error_enroll_invalid_qr_code))
                }

                val url: URL
                try {
                    url = URL(rawChallenge.substring(13))
                } catch (ex: MalformedURLException) {
                    return ParseEnrollmentChallengeError(ParseEnrollmentChallengeError.Type.INVALID_CHALLENGE, context.getString(R.string.enrollment_failure_title), context.getString(R.string.error_enroll_invalid_qr_code))
                }

                if (url.protocol != "http" && url.protocol != "https") {
                    return ParseEnrollmentChallengeError(ParseEnrollmentChallengeError.Type.INVALID_CHALLENGE, context.getString(R.string.enrollment_failure_title), context.getString(R.string.error_enroll_invalid_qr_code))
                }

                val metadata: JSONObject

                try {
                    val urlConnection = url.openConnection() as HttpURLConnection
                    urlConnection.addRequestProperty("ACCEPT", "application/json")
                    urlConnection.addRequestProperty("X-TIQR-Protocol-Version", Constants.PROTOCOL_VERSION)
                    urlConnection.requestMethod = "GET"
                    urlConnection.connect()
                    val json = Utils.urlConnectionResponseAsString(urlConnection)
                    Log.d(javaClass.simpleName, "Enrollment server response: " + json!!)
                    val tokener = JSONTokener(json)
                    val value = tokener.nextValue() as? JSONObject
                            ?: throw UserException(context.getString(R.string.error_enroll_invalid_response))

                    metadata = value
                    val identityProvider = getIdentityProviderForMetadata(metadata.getJSONObject("service"))
                    return EnrollmentChallenge(
                        enrollmentURL = metadata.getJSONObject("service").getString("enrollmentUrl"),
                        protocolVersion = null,
                        identityProvider = identityProvider,
                        identity = getIdentityForMetadata(metadata.getJSONObject("identity"), identityProvider),
                        returnURL = null // TODO: FIXME
                    )
                } catch (ex: IOException) {
                    return ParseEnrollmentChallengeError(ParseEnrollmentChallengeError.Type.CONNECTION, context.getString(R.string.enrollment_failure_title), context.getString(R.string.error_enroll_connect_error))
                } catch (ex: JSONException) {
                    return ParseEnrollmentChallengeError(ParseEnrollmentChallengeError.Type.INVALID_RESPONSE, context.getString(R.string.enrollment_failure_title), context.getString(R.string.error_enroll_invalid_response))
                } catch (ex: UserException) {
                    return ParseEnrollmentChallengeError(ParseEnrollmentChallengeError.Type.INVALID_CHALLENGE, context.getString(R.string.enrollment_failure_title), ex.message ?: "no message available")
                }
            }

            override fun onPostExecute(result: Any) {
                if (result is EnrollmentChallenge) {
                    listener.onParseEnrollmentChallengeSuccess(result)
                } else {
                    val error = result as ParseEnrollmentChallengeError
                    listener.onParseEnrollmentChallengeError(error)
                }
            }
        }

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        return task
    }

    /**
     * Send enrollment request to server.
     *
     * @param challenge Enrollment challenge.
     * @param password  Password / PIN.
     * @param listener  Completion listener.
     */
    fun enroll(challenge: EnrollmentChallenge, password: String, listener: OnEnrollmentListener): AsyncTask<*, *, *> {
        val task = object : AsyncTask<Void, Void, EnrollmentError>() {
            override fun doInBackground(vararg voids: Void): EnrollmentError? {
                try {
                    val sessionKey = Encryption.keyFromPassword(context, password)

                    val secret = generateSecret()


                    val nameValuePairs = HashMap<String, String>()
                    nameValuePairs["secret"] = keyToHex(secret)
                    nameValuePairs["language"] = Locale.getDefault().language
                    val notificationAddress = notificationService.notificationToken
                    if (notificationAddress != null) {
                        nameValuePairs["notificationType"] = "GCM"
                        nameValuePairs["notificationAddress"] = notificationAddress
                    }

                    nameValuePairs["operation"] = "register"

                    val enrollmentURL = URL(challenge.enrollmentURL)
                    val httpURLConnection = enrollmentURL.openConnection() as HttpURLConnection
                    httpURLConnection.requestMethod = "POST"
                    httpURLConnection.setRequestProperty("ACCEPT", "application/json")
                    httpURLConnection.setRequestProperty("X-TIQR-Protocol-Version", Constants.PROTOCOL_VERSION)
                    httpURLConnection.doOutput = true
                    val postData = Utils.keyValueMapToByteArray(nameValuePairs)
                    httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    httpURLConnection.setRequestProperty("Content-Length", postData.size.toString())
                    httpURLConnection.outputStream.write(postData)
                    val response = Utils.urlConnectionResponseAsString(httpURLConnection)

                    val versionHeader = httpURLConnection.getHeaderField("X-TIQR-Protocol-Version")

                    val error: EnrollmentError?
                    if (versionHeader == null || versionHeader == "1") {
                        // v1 protocol (ascii)
                        error = parseV1Response(response)
                    } else {
                        // v2 protocol (json)
                        error = parseV2Response(response)
                    }

                    if (error == null) {
                        storeIdentityAndIdentityProvider(challenge, secret, sessionKey)
                        return null
                    } else {
                        return error
                    }
                } catch (ex: Exception) {
                    return EnrollmentError(Type.CONNECTION, context.getString(R.string.error_enroll_connect_error), context.getString(R.string.error_enroll_connect_error), ex)
                }

            }

            override fun onPostExecute(error: EnrollmentError?) {
                if (error == null) {
                    listener.onEnrollmentSuccess()
                } else {
                    listener.onEnrollmentError(error)
                }
            }
        }

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        return task
    }


    /**
     * Parse v1 response format (ascii), return error object when unsuccessful.
     *
     * @param response
     * @return Error object on failure.
     */
    private fun parseV1Response(response: String?): EnrollmentError? {
        return if (response != null && response == "OK") {
            null
        } else {
            EnrollmentError(Type.UNKNOWN, context.getString(R.string.enrollment_failure_title), response!!, Exception("Unexpected response: $response"))
        }
    }

    /**
     * Parse v2 response format (json), return error object when unsuccessful.
     *
     * @param response
     * @return Error object on failure.
     */
    private fun parseV2Response(response: String?): EnrollmentError? {
        try {
            val `object` = JSONObject(response)

            val responseCode = `object`.getInt("responseCode")
            if (responseCode == 1) {
                return null // success, no error
            }

            var type = Type.UNKNOWN
            var message = `object`.optString("message", "")

            if (message.length == 0) {
                when (responseCode) {
                    101 -> {
                        type = Type.INVALID_RESPONSE
                        message = context.getString(R.string.error_enroll_general)
                    }
                    else -> {
                        type = Type.UNKNOWN
                        message = context.getString(R.string.error_enroll_general)
                    }
                }
            }
            return EnrollmentError(type, context.getString(R.string.enrollment_failure_title), message, Exception("Unexpected response code: $responseCode; message: $message"))
        } catch (ex: JSONException) {
            return EnrollmentError(Type.INVALID_RESPONSE, context.getString(R.string.enrollment_failure_title), context.getString(R.string.error_enroll_invalid_response), ex)
        }

    }

    /**
     * Generate identity secret.
     *
     * @return secret key
     * @throws UserException
     */
    @Throws(UserException::class)
    private fun generateSecret(): SecretKey {
        try {
            return Encryption.generateRandomSecretKey()
        } catch (ex: Exception) {
            throw UserException(context.getString(R.string.error_enroll_failed_to_generate_secret))
        }

    }

    /**
     * Store identity and identity provider.
     */
    @Throws(UserException::class, CertificateException::class, NoSuchAlgorithmException::class, KeyStoreException::class, IOException::class)
    private fun storeIdentityAndIdentityProvider(challenge: EnrollmentChallenge, secret: SecretKey, sessionKey: SecretKey) {

        if (!dbAdapter.insertIdentityProvider(challenge.identityProvider)) {
            throw UserException(context.getString(R.string.error_enroll_failed_to_store_identity_provider))
        }

        if (!dbAdapter.insertIdentityForIdentityProvider(challenge.identity, challenge.identityProvider)) {
            throw UserException(context.getString(R.string.error_enroll_failed_to_store_identity))
        }

        val secretStore = Secret.secretForIdentity(challenge.identity, context)
        secretStore.setSecret(secret)
        try {
            secretStore.storeInKeyStore(sessionKey, Secret.Type.PINCODE)
            return
        } catch (e: SecurityFeaturesException) {
            throw UserException(context.getString(R.string.error_device_incompatible_with_security_standards))
        }
    }


    /**
     * Download data from the given URL (synchronously).
     *
     * @param url url
     * @return data
     */
    @Throws(IOException::class)
    private fun downloadSynchronously(url: URL): ByteArray {
        return url.readBytes()
    }

    /**
     * Returns a identity provider object based on the given metadata.
     *
     * @param metadata JSON identity provider metadata
     * @return IdentityProvider object
     * @throws JSONException If the JSON could not be parsed.
     */
    @Throws(JSONException::class)
    private fun getIdentityProviderForMetadata(metadata: JSONObject): IdentityProvider {
        val provider = IdentityProvider(
            identifier = metadata.getString("identifier"),
            displayName = metadata.getString("displayName"),
            logoURL = metadata.getString("logoUrl"),
            authenticationURL = metadata.getString("authenticationUrl"),
            infoURL = metadata.getString("infoUrl")
        )
        if (metadata.has("ocraSuite")) {
            provider.ocraSuite = metadata.getString("ocraSuite")
        }
        return provider
    }

    /**
     * Returns an identity object based on the given metadata. If the identity already exists an exception is thrown.
     *
     * @param metadata JSON identity metadata
     * @return identity object
     * @throws JSONException,UserException If the JSON could not be parsed or if the identity is already enrolled
     */
    @Throws(JSONException::class, UserException::class)
    private fun getIdentityForMetadata(metadata: JSONObject, ip: IdentityProvider): Identity {
        var identity = dbAdapter.getIdentityByIdentifierAndIdentityProviderIdentifierAsObject(metadata.getString("identifier"), ip.identifier)
        if (identity != null) {
            throw UserException(context.getString(R.string.error_enroll_already_enrolled, metadata.getString("displayName"), ip.displayName))
        }

        return Identity(
            identifier = metadata.getString("identifier"),
            displayName = metadata.getString("displayName")
        )
    }

    private fun keyToHex(secret: SecretKey): String {
        val buf = secret.encoded
        val strbuf = StringBuffer(buf.size * 2)
        var i: Int

        i = 0
        while (i < buf.size) {
            if (buf[i].toInt().and(0xff) < 0x10)
                strbuf.append("0")

            strbuf.append(java.lang.Long.toString((buf[i].toInt().and(0xff)).toLong(), 16))
            i++
        }

        return strbuf.toString()
    }
}