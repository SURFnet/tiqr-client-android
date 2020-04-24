/*
 * Copyright (c) 2010-2019 SURFnet bv
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of SURFnet bv nor the names of its contributors
 *    may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.tiqr.data.repository

import android.content.res.Resources
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.tiqr.data.BuildConfig
import org.tiqr.data.R
import org.tiqr.data.algorithm.Ocra
import org.tiqr.data.api.TiqrApi
import org.tiqr.data.api.interceptor.HeaderInjector
import org.tiqr.data.model.*
import org.tiqr.data.model.AuthenticationResponse.Code.*
import org.tiqr.data.repository.base.ChallengeRepository
import org.tiqr.data.security.SecurityFeaturesException
import org.tiqr.data.service.DatabaseService
import org.tiqr.data.service.PreferenceService
import org.tiqr.data.service.SecretService
import timber.log.Timber
import java.io.IOException
import java.security.InvalidKeyException
import java.util.*
import javax.crypto.SecretKey

/**
 * Repository to handle authentication challenges.
 */
class AuthenticationRepository(
        private val api: TiqrApi,
        private val resources: Resources,
        private val database: DatabaseService,
        private val secretService: SecretService,
        private val preferences: PreferenceService
) : ChallengeRepository<AuthenticationChallenge>() {
    override val challengeScheme: String = BuildConfig.TIQR_AUTH_SCHEME

    /**
     * Validate the [rawChallenge] and request authentication.
     */
    override suspend fun parseChallenge(rawChallenge: String): ChallengeParseResult<AuthenticationChallenge, AuthenticationParseFailure> {
        // Check challenge validity
        val isValid = isValidChallenge(rawChallenge)
        val url = rawChallenge.replaceFirst(challengeScheme, "http://").toHttpUrlOrNull()

        if (isValid.not() || url == null || url.pathSize < 3) {
            return AuthenticationParseFailure(
                    reason = AuthenticationParseFailure.Reason.INVALID_CHALLENGE,
                    title = resources.getString(R.string.error_auth_title),
                    message = resources.getString(R.string.error_auth_invalid_qr)
            ).run {
                Timber.e("Invalid QR: $url")
                ChallengeParseResult.failure(this)
            }
        }

        // Check if identity provider is known
        val identityProvider = database.getIdentityProviderByIdentifier(url.host)
                ?: return AuthenticationParseFailure(
                        reason = AuthenticationParseFailure.Reason.INVALID_IDENTITY_PROVIDER,
                        title = resources.getString(R.string.error_auth_title),
                        message = resources.getString(R.string.error_auth_unknown_identity_provider)
                ).run {
                    Timber.e("Unknown identity provider: ${url.host}")
                    ChallengeParseResult.failure(this)
                }

        // Check if identity is known
        val identity = if (url.username.isNotBlank()) {
            database.getIdentity(url.username, identityProvider.identifier)
                    ?: return AuthenticationParseFailure(
                            reason = AuthenticationParseFailure.Reason.INVALID_IDENTITY,
                            title = resources.getString(R.string.error_auth_title),
                            message = resources.getString(R.string.error_auth_unknown_identity)
                    ).run {
                        Timber.e("Unknown identity: ${url.username}")
                        ChallengeParseResult.failure(this)
                    }
        } else {
            database.getIdentity(identityProvider.identifier)
                    ?: return AuthenticationParseFailure(
                            reason = AuthenticationParseFailure.Reason.NO_IDENTITIES,
                            title = resources.getString(R.string.error_auth_title),
                            message = resources.getString(R.string.error_auth_no_identities)
                    ).run {
                        Timber.e("No identities for identity provider: ${identityProvider.identifier}")
                        ChallengeParseResult.failure(this)
                    }
        }

        return AuthenticationChallenge(
                protocolVersion = url.pathSegments.getOrElse(3) { "1" } ,
                identityProvider = identityProvider,
                identity = identity,
                returnUrl = url.query?.toHttpUrlOrNull()?.toString(),
                sessionKey = url.pathSegments[0],
                challenge = url.pathSegments[1],
                isStepUpChallenge = url.username.isNotEmpty(),
                serviceProviderDisplayName = url.pathSegments.getOrElse(2) { resources.getString(R.string.error_auth_empty_service_provider) },
                serviceProviderIdentifier = ""
        ).run {
            ChallengeParseResult.success(this)
        }
    }

    override suspend fun completeChallenge(request: ChallengeCompleteRequest<AuthenticationChallenge>): ChallengeCompleteResult<ChallengeCompleteFailure> {
        if (request !is AuthenticationCompleteRequest) {
            return ChallengeCompleteResult.failure(AuthenticationCompleteFailure(
                    reason = AuthenticationCompleteFailure.Reason.INVALID_RESPONSE,
                    title = resources.getString(R.string.error_auth_title),
                    message = resources.getString(R.string.error_auth_invalid_response)
            ))
        }

        try {
            val secret: SecretKey = secretService.getSecret(
                    identity = request.challenge.identity,
                    sessionKey = secretService.encryption.keyFromPassword(request.password)
            )

            val otp: String = Ocra.generate(
                    suite = request.challenge.identityProvider.ocraSuite,
                    key = secret.encoded,
                    question = request.challenge.challenge,
                    session = request.challenge.sessionKey
            )

            api.authenticate(
                    url = request.challenge.identityProvider.authenticationUrl,
                    sessionKey = request.challenge.sessionKey,
                    userId = request.challenge.identity.identifier,
                    response = otp,
                    language = Locale.getDefault().language,
                    notificationAddress = preferences.notificationToken
            ).run {
                if (isSuccessful) {
                    val result = body() ?: return AuthenticationCompleteFailure(
                            reason = AuthenticationCompleteFailure.Reason.INVALID_RESPONSE,
                            title = resources.getString(R.string.error_auth_title),
                            message = resources.getString(R.string.error_auth_invalid_response)
                    ).run {
                        Timber.e("Error completing authentication, API response is empty")
                        ChallengeCompleteResult.failure(this)
                    }

                    when (val protocol = headers()[HeaderInjector.HEADER_PROTOCOL]) {
                        null, "1" -> { // Unsupported Ascii-response
                            return AuthenticationCompleteFailure(
                                    reason = AuthenticationCompleteFailure.Reason.INVALID_RESPONSE,
                                    title = resources.getString(R.string.error_auth_title),
                                    message = resources.getString(R.string.error_auth_invalid_protocol, protocol)
                            ).run {
                                Timber.e("Error completing authentication, unsupported protocol version: v$protocol")
                                ChallengeCompleteResult.failure(this)
                            }
                        }
                    }

                    return when (result.code) {
                        AUTH_RESULT_SUCCESS -> ChallengeCompleteResult.success()
                        AUTH_RESULT_INVALID_RESPONSE -> {
                            val remainingAttempts = result.attemptsLeft ?: 0
                            when {
                                remainingAttempts > 1 -> {
                                    if (request.type == SecretService.Type.BIOMETRIC) {
                                        AuthenticationCompleteFailure(
                                                AuthenticationCompleteFailure.Reason.INVALID_RESPONSE,
                                                title = resources.getString(R.string.error_auth_title_biometric),
                                                message = resources.getString(R.string.error_auth_biometric_x_attempts_left, remainingAttempts),
                                                remainingAttempts = remainingAttempts
                                        )
                                    } else {
                                        AuthenticationCompleteFailure(
                                                AuthenticationCompleteFailure.Reason.INVALID_RESPONSE,
                                                title = resources.getString(R.string.error_auth_title_pin),
                                                message = resources.getString(R.string.error_auth_pin_x_attempts_left, remainingAttempts),
                                                remainingAttempts = remainingAttempts
                                        )
                                    }
                                }
                                remainingAttempts == 1 -> {
                                    if (request.type == SecretService.Type.BIOMETRIC) {
                                        AuthenticationCompleteFailure(
                                                AuthenticationCompleteFailure.Reason.INVALID_RESPONSE,
                                                title = resources.getString(R.string.error_auth_title_biometric),
                                                message = resources.getString(R.string.error_auth_biometric_one_attempt_left),
                                                remainingAttempts = remainingAttempts
                                        )
                                    } else {
                                        AuthenticationCompleteFailure(
                                                AuthenticationCompleteFailure.Reason.INVALID_RESPONSE,
                                                title = resources.getString(R.string.error_auth_title_pin),
                                                message = resources.getString(R.string.error_auth_pin_one_attempt_left),
                                                remainingAttempts = remainingAttempts
                                        )
                                    }
                                }
                                else -> {
                                    AuthenticationCompleteFailure(
                                            AuthenticationCompleteFailure.Reason.INVALID_RESPONSE,
                                            title = resources.getString(R.string.error_auth_title_blocked),
                                            message = resources.getString(R.string.error_auth_blocked),
                                            remainingAttempts = remainingAttempts
                                    )
                                }
                            }.run {
                                ChallengeCompleteResult.failure(this)
                            }
                        }
                        AUTH_RESULT_INVALID_REQUEST -> {
                            AuthenticationCompleteFailure(
                                    reason = AuthenticationCompleteFailure.Reason.INVALID_REQUEST,
                                    title = resources.getString(R.string.error_auth_title_request),
                                    message = resources.getString(R.string.error_auth_request_invalid)
                            ).run {
                                ChallengeCompleteResult.failure(this)
                            }
                        }
                        AUTH_RESULT_INVALID_CHALLENGE -> {
                            AuthenticationCompleteFailure(
                                    reason = AuthenticationCompleteFailure.Reason.INVALID_CHALLENGE,
                                    title = resources.getString(R.string.error_auth_title_challenge),
                                    message = resources.getString(R.string.error_auth_challenge_invalid)
                            ).run {
                                ChallengeCompleteResult.failure(this)
                            }
                        }
                        AUTH_RESULT_ACCOUNT_BLOCKED -> {
                            if (result.duration != null) {
                                AuthenticationCompleteFailure(
                                        reason = AuthenticationCompleteFailure.Reason.ACCOUNT_TEMPORARY_BLOCKED,
                                        title = resources.getString(R.string.error_auth_title_blocked_temporary),
                                        message = resources.getString(R.string.error_auth_blocked_temporary, result.duration),
                                        duration = result.duration
                                )
                            } else {
                                AuthenticationCompleteFailure(
                                        reason = AuthenticationCompleteFailure.Reason.ACCOUNT_BLOCKED,
                                        title = resources.getString(R.string.error_auth_title_blocked),
                                        message = resources.getString(R.string.error_auth_blocked)
                                )
                            }.run {
                                ChallengeCompleteResult.failure(this)
                            }
                        }
                        AUTH_RESULT_INVALID_USER_ID -> {
                            AuthenticationCompleteFailure(
                                    reason = AuthenticationCompleteFailure.Reason.INVALID_USER,
                                    title = resources.getString(R.string.error_auth_title_account),
                                    message = resources.getString(R.string.error_auth_account_invalid)
                            ).run {
                                ChallengeCompleteResult.failure(this)
                            }
                        }
                    }
                } else {
                    return AuthenticationCompleteFailure(
                            reason = AuthenticationCompleteFailure.Reason.CONNECTION,
                            title = resources.getString(R.string.error_title_unknown),
                            message = resources.getString(R.string.error_auth_unknown_error)
                    ).run {
                        Timber.e("Error completing authentication, API response failed")
                        ChallengeCompleteResult.failure(this)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Authentication failed")
            return when (e) {
                is Ocra.OcraException ->
                    AuthenticationCompleteFailure(
                            reason = AuthenticationCompleteFailure.Reason.INVALID_CHALLENGE,
                            title = resources.getString(R.string.error_auth_title),
                            message = resources.getString(R.string.error_auth_invalid_challenge)
                    )
                is InvalidKeyException ->
                    AuthenticationCompleteFailure(
                            reason = AuthenticationCompleteFailure.Reason.UNKNOWN,
                            title = resources.getString(R.string.error_auth_title),
                            message = resources.getString(R.string.error_auth_invalid_key)
                    )
                is SecurityFeaturesException ->
                    AuthenticationCompleteFailure(
                            reason = AuthenticationCompleteFailure.Reason.UNKNOWN,
                            title = resources.getString(R.string.error_auth_title),
                            message = resources.getString(R.string.error_security_standards)
                    )
                is IOException ->
                    AuthenticationCompleteFailure(
                            reason = AuthenticationCompleteFailure.Reason.CONNECTION,
                            title = resources.getString(R.string.error_auth_title),
                            message = resources.getString(R.string.error_auth_connect_error)
                    )
                is JsonDataException,
                is JsonEncodingException ->
                    //FIXME: when server responds with just text 'false' with http 200 instead of http 4xx.
                    // this means the server cannot handle this challenge because it is invalid (?).
                    // Ideally the server should respond with an error http 4xx.
                    AuthenticationCompleteFailure(
                            reason = AuthenticationCompleteFailure.Reason.UNKNOWN,
                            title = resources.getString(R.string.error_title_unknown),
                            message = resources.getString(R.string.error_auth_unknown_error)
                    )
                else ->
                    AuthenticationCompleteFailure(
                            reason = AuthenticationCompleteFailure.Reason.UNKNOWN,
                            title = resources.getString(R.string.error_title_unknown),
                            message = resources.getString(R.string.error_auth_unknown_error)
                    )
            }.run {
                ChallengeCompleteResult.failure(this)
            }
        }
    }
}