package org.tiqr.service.authentication

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle

import org.json.JSONException
import org.json.JSONObject
import org.tiqr.Constants
import org.tiqr.Utils
import org.tiqr.R
import org.tiqr.authenticator.auth.AuthenticationChallenge
import org.tiqr.authenticator.datamodel.DbAdapter
import org.tiqr.authenticator.datamodel.Identity
import org.tiqr.authenticator.exceptions.InvalidChallengeException
import org.tiqr.authenticator.exceptions.SecurityFeaturesException
import org.tiqr.authenticator.security.Encryption
import org.tiqr.authenticator.security.OCRAProtocol
import org.tiqr.authenticator.security.OCRAWrapper
import org.tiqr.authenticator.security.OCRAWrapper_v1
import org.tiqr.authenticator.security.Secret
import org.tiqr.service.authentication.AuthenticationError.Type
import org.tiqr.service.notification.NotificationService

import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
import java.security.InvalidKeyException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableEntryException
import java.security.cert.CertificateException
import java.util.HashMap
import java.util.Locale

/**
 * Authentication data service.
 */
class AuthenticationService(private val _context: Context, private val _notificationService: NotificationService, private val _dbAdapter: DbAdapter) {

    interface OnParseAuthenticationChallengeListener {
        fun onParseAuthenticationChallengeSuccess(challenge: AuthenticationChallenge)

        fun onParseAuthenticationChallengeError(error: ParseAuthenticationChallengeError)
    }

    interface OnAuthenticationListener {
        fun onAuthenticationSuccess()

        fun onAuthenticationError(error: AuthenticationError?)
    }

    /**
     * Contains an authentication challenge?
     *
     * @param rawChallenge Raw challenge.
     * @return Is authentication challenge?
     */
    fun isAuthenticationChallenge(rawChallenge: String): Boolean {
        return rawChallenge.startsWith("tiqrauth://")
    }

    /**
     * Parses the raw authentication challenge.
     *
     * @param rawChallenge Raw challenge.
     * @param listener     Completion listener.
     */
    fun parseAuthenticationChallenge(rawChallenge: String, listener: OnParseAuthenticationChallengeListener): AsyncTask<*, *, *> {
        val task = object : AsyncTask<Void, Void, Any>() {
            override fun doInBackground(vararg voids: Void): Any {

                if (!rawChallenge.startsWith("tiqrauth://")) {
                    return ParseAuthenticationChallengeError(ParseAuthenticationChallengeError.Type.INVALID_CHALLENGE, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_invalid_qr_code))
                }

                val url: URL

                try {
                    url = URL(rawChallenge.replaceFirst("tiqrauth://".toRegex(), "http://"))
                } catch (ex: MalformedURLException) {
                    return ParseAuthenticationChallengeError(ParseAuthenticationChallengeError.Type.INVALID_CHALLENGE, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_invalid_qr_code))
                }

                val pathComponents = url.path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (pathComponents.size < 3) {
                    return ParseAuthenticationChallengeError(ParseAuthenticationChallengeError.Type.INVALID_CHALLENGE, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_invalid_qr_code))
                }

                val ip = _dbAdapter.getIdentityProviderByIdentifierAsObject(url.host)
                        ?: return ParseAuthenticationChallengeError(ParseAuthenticationChallengeError.Type.INVALID_IDENTITY_PROVIDER, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_unknown_identity_provider))

                var identity: Identity?

                if (url.userInfo != null) {
                    var userInfo = url.userInfo
                    try {
                        userInfo = URLDecoder.decode(userInfo, "UTF-8")
                    } catch (e: UnsupportedEncodingException) {
                        // never happens...
                    }

                    identity = _dbAdapter.getIdentityByIdentifierAndIdentityProviderIdentifierAsObject(userInfo, ip.identifier)
                    if (identity == null) {
                        return ParseAuthenticationChallengeError(ParseAuthenticationChallengeError.Type.INVALID_IDENTITY, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_unknown_identity))
                    }

                } else {
                    val identities = _dbAdapter.findIdentitiesByIdentityProviderIdentifierAsObjects(ip.identifier)

                    if (identities.size == 0) {
                        return ParseAuthenticationChallengeError(ParseAuthenticationChallengeError.Type.NO_IDENTITIES, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_no_identities_for_identity_provider))
                    }

                    identity = if (identities.size >= 1) identities[0] else null
                }

                val identityProvider = if (identity == null) ip else _dbAdapter.getIdentityProviderForIdentityId(identity.id)
                val isStepUpChallenge = url.userInfo != null && url.userInfo.length > 0

                val serviceProviderDisplayName = when {
                    pathComponents.size > 3 -> pathComponents[3]
                    else -> _context.getString(R.string.unknown)
                }

                val protocolVersion = when {
                    pathComponents.size > 4 -> pathComponents[4]
                    else -> "1"
                }

                var returnURL = if (url.query == null || url.query.length == 0) null else url.query
                if (returnURL != null && returnURL.matches("^http(s)?://.*".toRegex())) {
                    try {
                        returnURL = URLDecoder.decode(returnURL, "UTF-8")
                    } catch (e: UnsupportedEncodingException) {
                        // never happens...
                    }

                }

                if (identityProvider != null && identity != null) {
                    return AuthenticationChallenge(
                            sessionKey = pathComponents[1],
                            challenge = pathComponents[2],
                            serviceProviderDisplayName = serviceProviderDisplayName,
                            serviceProviderIdentifier = "",
                            isStepUpChallenge = isStepUpChallenge,
                            protocolVersion = protocolVersion,
                            identityProvider = identityProvider,
                            identity = identity,
                            returnURL = returnURL
                    )
                } else if (identity == null) {
                    return ParseAuthenticationChallengeError(ParseAuthenticationChallengeError.Type.INVALID_IDENTITY, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_unknown_identity))
                } else {
                    return ParseAuthenticationChallengeError(ParseAuthenticationChallengeError.Type.INVALID_IDENTITY_PROVIDER, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_unknown_identity_provider))
                }
            }

            override fun onPostExecute(result: Any) {
                if (result is AuthenticationChallenge) {
                    listener.onParseAuthenticationChallengeSuccess(result)
                } else {
                    val error = result as ParseAuthenticationChallengeError
                    listener.onParseAuthenticationChallengeError(error)
                }
            }
        }

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        return task

    }

    /**
     * Authenticate.
     *
     * @param challenge Challenge.
     * @param password  Password / pin.
     * @param listener  Completion listener.
     * @return async task
     */
    fun authenticate(challenge: AuthenticationChallenge, password: String, type: Secret.Type, listener: OnAuthenticationListener): AsyncTask<*, *, *> {
        val task = object : AsyncTask<Void, Void, AuthenticationError>() {
            override fun doInBackground(vararg voids: Void): AuthenticationError? {
                var otp: String?

                try {
                    val sessionKey = Encryption.keyFromPassword(_context, password)
                    val secret = Secret.secretForIdentity(challenge.identity, _context)
                    val secretKey = secret.getSecret(sessionKey, type)

                    val ocra: OCRAProtocol
                    if (challenge.protocolVersion == "1") {
                        ocra = OCRAWrapper_v1()
                    } else {
                        ocra = OCRAWrapper()
                    }

                    otp = ocra.generateOCRA(challenge.identityProvider.ocraSuite, secretKey!!.encoded, challenge.challenge, challenge.sessionKey)

                    // Add your dNameValuePair
                    val nameValuePairs = HashMap<String, String>()
                    nameValuePairs["sessionKey"] = challenge.sessionKey
                    nameValuePairs["userId"] = challenge.identity.identifier
                    nameValuePairs["response"] = otp
                    nameValuePairs["language"] = Locale.getDefault().language

                    val notificationAddress = _notificationService.notificationToken
                    if (notificationAddress != null) {
                        // communicate latest notification type and address
                        nameValuePairs["notificationType"] = "GCM"
                        nameValuePairs["notificationAddress"] = notificationAddress
                    }

                    nameValuePairs["operation"] = "login"

                    val postData = Utils.keyValueMapToByteArray(nameValuePairs)

                    val authenticationURL = URL(challenge.identityProvider.authenticationURL)
                    val httpURLConnection = authenticationURL.openConnection() as HttpURLConnection
                    httpURLConnection.requestMethod = "POST"
                    httpURLConnection.setRequestProperty("ACCEPT", "application/json")
                    httpURLConnection.setRequestProperty("X-TIQR-Protocol-Version", Constants.PROTOCOL_VERSION)
                    httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    httpURLConnection.setRequestProperty("Content-Length", postData.size.toString())
                    httpURLConnection.doOutput = true
                    httpURLConnection.outputStream.write(postData)

                    val response = Utils.urlConnectionResponseAsString(httpURLConnection)
                    val versionHeader = httpURLConnection.getHeaderField("X-TIQR-Protocol-Version")
                    return if (versionHeader == null || versionHeader == "1") {
                        // v1 protocol (ascii)
                        _parseV1Response(response, type)
                    } else {
                        // v2 protocol (json)
                        _parseV2Response(response, type)
                    }
                } catch (ex: InvalidChallengeException) {
                    return AuthenticationError(ex, Type.INVALID_CHALLENGE, _context.getString(R.string.error_auth_invalid_challenge_title), _context.getString(R.string.error_auth_invalid_challenge))
                } catch (ex: InvalidKeyException) {
                    return AuthenticationError(ex, Type.UNKNOWN, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_invalid_key))
                } catch (ex: SecurityFeaturesException) {
                    return AuthenticationError(ex, Type.UNKNOWN, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_device_incompatible_with_security_standards))
                } catch (ex: CertificateException) {
                    return AuthenticationError(ex, Type.UNKNOWN, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_device_incompatible_with_security_standards))
                } catch (ex: UnrecoverableEntryException) {
                    return AuthenticationError(ex, Type.UNKNOWN, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_device_incompatible_with_security_standards))
                } catch (ex: NoSuchAlgorithmException) {
                    return AuthenticationError(ex, Type.UNKNOWN, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_device_incompatible_with_security_standards))
                } catch (ex: KeyStoreException) {
                    return AuthenticationError(ex, Type.UNKNOWN, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_device_incompatible_with_security_standards))
                } catch (ex: IOException) {
                    return AuthenticationError(ex, Type.CONNECTION, _context.getString(R.string.authentication_failure_title), _context.getString(R.string.error_auth_connect_error))
                }

            }

            override fun onPostExecute(error: AuthenticationError?) {
                if (error == null) {
                    listener.onAuthenticationSuccess()
                } else {
                    if (error.type === Type.ACCOUNT_BLOCKED) {
                        challenge.identity.isBlocked = true
                        _dbAdapter.updateIdentity(challenge.identity)
                    } else if (error.type === Type.INVALID_RESPONSE) {
                        if (error.extras.containsKey("attemptsLeft") && error.extras.getInt("attemptsLeft") == 0) {
                            _dbAdapter.blockAllIdentities()
                        }
                    }

                    listener.onAuthenticationError(error)
                }
            }
        }

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        return task
    }

    /**
     * Parse authentication response from server (v1, plain string).
     *
     * @param response authentication response
     * @param identity the corresponding identity
     * @param secretType The secret type used for authenticating
     * @return Error or null on success.
     */
    private fun _parseV1Response(response: String?, secretType: Secret.Type): AuthenticationError? {
        try {
            if (response != null && response == "OK") {
                return null
            } else if (response == "ACCOUNT_BLOCKED") {
                return AuthenticationError(Type.ACCOUNT_BLOCKED, _context.getString(R.string.error_auth_account_blocked_title), _context.getString(R.string.error_auth_account_blocked_message))
            } else if (response == "INVALID_CHALLENGE") {
                return AuthenticationError(Type.INVALID_CHALLENGE, _context.getString(R.string.error_auth_invalid_challenge_title), _context.getString(R.string.error_auth_invalid_challenge_message))
            } else if (response == "INVALID_REQUEST") {
                return AuthenticationError(Type.INVALID_REQUEST, _context.getString(R.string.error_auth_invalid_request_title), _context.getString(R.string.error_auth_invalid_request_message))
            } else if (response!!.substring(0, 17) == "INVALID_RESPONSE:") {
                val attemptsLeft = Integer.parseInt(response.substring(17, 18))
                val extras = Bundle()
                extras.putInt("attemptsLeft", attemptsLeft)

                return if (secretType == Secret.Type.FINGERPRINT) {
                    if (attemptsLeft > 1) {
                        AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_wrong_fingerprint), String.format(_context.getString(R.string.error_fingerprint_auth_x_attempts_left), attemptsLeft), extras)
                    } else if (attemptsLeft == 1) {
                        AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_wrong_fingerprint), _context.getString(R.string.error_fingerprint_auth_one_attempt_left), extras)
                    } else {
                        AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_account_blocked_title), _context.getString(R.string.error_auth_account_blocked_message), extras)
                    }
                } else {
                    if (attemptsLeft > 1) {
                        AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_wrong_pin), String.format(_context.getString(R.string.error_auth_x_attempts_left), attemptsLeft), extras)
                    } else if (attemptsLeft == 1) {
                        AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_wrong_pin), _context.getString(R.string.error_auth_one_attempt_left), extras)
                    } else {
                        AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_account_blocked_title), _context.getString(R.string.error_auth_account_blocked_message), extras)
                    }
                }
            } else return if (response == "INVALID_USERID") {
                AuthenticationError(Type.INVALID_USER, _context.getString(R.string.error_auth_invalid_account), _context.getString(R.string.error_auth_invalid_account_message))
            } else {
                AuthenticationError(null, Type.UNKNOWN, _context.getString(R.string.unknown_error), _context.getString(R.string.error_auth_unknown_error))
            }
        } catch (ex: NumberFormatException) {
            return AuthenticationError(ex, Type.INVALID_CHALLENGE, _context.getString(R.string.error_auth_invalid_challenge_title), _context.getString(R.string.error_auth_invalid_challenge_message))
        } catch (ex: Exception) {
            return AuthenticationError(ex, Type.INVALID_CHALLENGE, _context.getString(R.string.error_auth_invalid_challenge_title), _context.getString(R.string.error_auth_invalid_challenge_message))
        }

    }

    /**
     * Parse authentication response from server (v2, json).
     *
     * @param response authentication response
     * @return Error or null on success.
     */
    private fun _parseV2Response(response: String?, secretType: Secret.Type): AuthenticationError? {
        try {
            val `object` = JSONObject(response)

            val responseCode = `object`.getInt("responseCode")

            if (responseCode == 1) {
                return null
            } else if (responseCode == 204) {
                if (`object`.has("duration")) {
                    val duration = `object`.getInt("duration")
                    val extras = Bundle()
                    extras.putInt("duration", duration)
                    return AuthenticationError(Type.ACCOUNT_TEMPORARY_BLOCKED, _context.getString(R.string.error_auth_account_blocked_temporary_title), _context.getString(R.string.error_auth_account_blocked_temporary_message, duration), extras)
                } else {
                    return AuthenticationError(Type.ACCOUNT_BLOCKED, _context.getString(R.string.error_auth_account_blocked_title), _context.getString(R.string.error_auth_account_blocked_message))
                }
            } else if (responseCode == 203) {
                return AuthenticationError(Type.INVALID_CHALLENGE, _context.getString(R.string.error_auth_invalid_challenge_title), _context.getString(R.string.error_auth_invalid_challenge_message))
            } else if (responseCode == 202) {
                return AuthenticationError(Type.INVALID_REQUEST, _context.getString(R.string.error_auth_invalid_request_title), _context.getString(R.string.error_auth_invalid_request_message))
            } else if (responseCode == 201) {
                val attemptsLeft = `object`.getInt("attemptsLeft")
                val extras = Bundle()
                extras.putInt("attemptsLeft", attemptsLeft)

                return if (secretType == Secret.Type.FINGERPRINT) {
                    if (attemptsLeft > 1) {
                        AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_wrong_fingerprint), String.format(_context.getString(R.string.error_fingerprint_auth_x_attempts_left), attemptsLeft), extras)
                    } else if (attemptsLeft == 1) {
                        AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_wrong_fingerprint), _context.getString(R.string.error_fingerprint_auth_one_attempt_left), extras)
                    } else {
                        AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_account_blocked_title), _context.getString(R.string.error_auth_account_blocked_message), extras)
                    }
                } else {
                    if (attemptsLeft > 1) {
                        AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_wrong_pin), String.format(_context.getString(R.string.error_auth_x_attempts_left), attemptsLeft), extras)
                    } else if (attemptsLeft == 1) {
                        AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_wrong_pin), _context.getString(R.string.error_auth_one_attempt_left), extras)
                    } else {
                        AuthenticationError(Type.INVALID_RESPONSE, _context.getString(R.string.error_auth_account_blocked_title), _context.getString(R.string.error_auth_account_blocked_message), extras)
                    }
                }
            } else return if (responseCode == 205) {
                AuthenticationError(Type.INVALID_USER, _context.getString(R.string.error_auth_invalid_account), _context.getString(R.string.error_auth_invalid_account_message))
            } else {
                AuthenticationError(Type.UNKNOWN, _context.getString(R.string.unknown_error), _context.getString(R.string.error_auth_unknown_error))
            }
        } catch (e: JSONException) {
            return AuthenticationError(Type.UNKNOWN, _context.getString(R.string.unknown_error), _context.getString(R.string.error_auth_unknown_error))
        } catch (e: NumberFormatException) {
            return AuthenticationError(Type.INVALID_CHALLENGE, _context.getString(R.string.error_auth_invalid_challenge_title), _context.getString(R.string.error_auth_invalid_challenge_message))
        } catch (e: Exception) {
            return AuthenticationError(Type.INVALID_CHALLENGE, _context.getString(R.string.error_auth_invalid_challenge_title), _context.getString(R.string.error_auth_invalid_challenge_message))
        }

    }


    /**
     * Sets the fingerprint authentication method.
     *
     * @param identity the Identity that will be using the fingerprint authentication
     */
    fun useFingerPrintAsAuthenticationForIdentity(identity: Identity) {
        identity.setUseFingerprint(true)
        identity.setShowFingerprintUpgrade(false)
        _dbAdapter.updateIdentity(identity)

    }

    /**
     * Stores the user option to not use fingerprint upgrade
     *
     * @param identity the Identity for which the fingerprint update dialog will not be shown anymore.
     */
    fun shouldShowFingerprintUpgradeForIdentitiy(identity: Identity, showFingerprintUpgraded: Boolean) {
        identity.setShowFingerprintUpgrade(showFingerprintUpgraded)
        _dbAdapter.updateIdentity(identity)
    }

    /**
     * Tests if the user has a valid fingerprint signature  for the identity secret
     *
     * @param identity
     * @return
     */
    fun hasFingerprintSecret(identity: Identity): Boolean {
        try {
            val sessionKey = Encryption.keyFromPassword(_context, Constants.AUTHENTICATION_FINGERPRINT_KEY)
            val secret = Secret.secretForIdentity(identity, _context)
            secret.getSecret(sessionKey, Secret.Type.FINGERPRINT)
            return true
        } catch (ex: Exception) {
            return false
        }

    }
}
