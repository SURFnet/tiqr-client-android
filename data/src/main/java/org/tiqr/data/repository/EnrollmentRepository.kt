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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.tiqr.data.BuildConfig
import org.tiqr.data.R
import org.tiqr.data.api.TiqrApi
import org.tiqr.data.api.response.ApiResponse
import org.tiqr.data.model.Challenge
import org.tiqr.data.model.ChallengeCompleteFailure
import org.tiqr.data.model.ChallengeCompleteRequest
import org.tiqr.data.model.ChallengeCompleteResult
import org.tiqr.data.model.ChallengeParseResult
import org.tiqr.data.model.EnrollmentChallenge
import org.tiqr.data.model.EnrollmentCompleteFailure
import org.tiqr.data.model.EnrollmentParseFailure
import org.tiqr.data.model.EnrollmentResponse
import org.tiqr.data.model.Identity
import org.tiqr.data.model.IdentityProvider
import org.tiqr.data.model.Secret
import org.tiqr.data.model.SecretType
import org.tiqr.data.repository.base.ChallengeRepository
import org.tiqr.data.service.DatabaseService
import org.tiqr.data.service.PreferenceService
import org.tiqr.data.service.SecretService
import org.tiqr.data.util.extension.isHttpOrHttps
import org.tiqr.data.util.extension.tiqrProtocol
import org.tiqr.data.util.extension.toDecodedUrlStringOrNull
import org.tiqr.data.util.extension.toHexString
import org.tiqr.data.util.extension.toUrlOrNull
import timber.log.Timber
import java.io.IOException
import java.util.*

/**
 * Repository to handle enrollment challenges.
 */
class EnrollmentRepository(
        override val api: TiqrApi,
        override val resources: Resources,
        override val database: DatabaseService,
        override val secretService: SecretService,
        override val preferences: PreferenceService
) : ChallengeRepository<EnrollmentChallenge>() {
    override val challengeScheme: String = BuildConfig.TIQR_ENROLL_SCHEME

    /**
     * Validate the [rawChallenge] and request enrollment.
     */
    override suspend fun parseChallenge(rawChallenge: String): ChallengeParseResult<EnrollmentChallenge, EnrollmentParseFailure> {
        // Check challenge validity
        val isValid = isValidChallenge(rawChallenge)
        val url = rawChallenge.substring(challengeScheme.length).toHttpUrlOrNull()
        if (isValid.not() || url == null || url.isHttpOrHttps().not()) {
            return EnrollmentParseFailure(
                    reason = EnrollmentParseFailure.Reason.INVALID_CHALLENGE,
                    title = resources.getString(R.string.error_enroll_title),
                    message = resources.getString(R.string.error_enroll_invalid_qr)
            ).run {
                Timber.e("Invalid QR: $url")
                ChallengeParseResult.failure(this)
            }
        }

        return try {
            // Perform API call and return result
            api.requestEnroll(url = url.toString()).run {
                val enroll = body()
                if (isSuccessful && enroll != null) {
                    val identityProvider = enroll.service.run {
                        IdentityProvider(
                                displayName = displayName,
                                identifier = identifier,
                                authenticationUrl = authenticationUrl,
                                infoUrl = infoUrl,
                                ocraSuite = ocraSuite,
                                logo = logoUrl
                        )
                    }

                    val identity = enroll.identity.run {
                        Identity(
                                displayName = displayName,
                                identifier = identifier
                        )
                    }

                    // Check if identity is already enrolled
                    database.getIdentity(
                            identityId = identity.identifier,
                            identityProviderId = identityProvider.identifier
                    )?.let {
                        return EnrollmentParseFailure(
                                reason = EnrollmentParseFailure.Reason.INVALID_CHALLENGE,
                                title = resources.getString(R.string.error_enroll_title),
                                message = resources.getString(R.string.error_enroll_duplicate_identity, identity.displayName, identityProvider.displayName)
                        ).run {
                            ChallengeParseResult.failure(this)
                        }
                    }

                    withContext(Dispatchers.IO) {
                        EnrollmentChallenge(
                                identityProvider = identityProvider,
                                identity = identity,
                                returnUrl = url.query?.toDecodedUrlStringOrNull(),
                                enrollmentUrl = enroll.service.enrollmentUrl,
                                enrollmentHost = enroll.service.enrollmentUrl.toUrlOrNull()?.host ?: enroll.service.enrollmentUrl
                        ).run {
                            ChallengeParseResult.success(this)
                        }
                    }
                } else {
                    return EnrollmentParseFailure(
                            reason = EnrollmentParseFailure.Reason.INVALID_CHALLENGE,
                            title = resources.getString(R.string.error_enroll_title),
                            message = resources.getString(R.string.error_enroll_invalid_qr)
                    ).run {
                        ChallengeParseResult.failure(this)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing challenge")

            return when(e) {
                is IOException ->
                    EnrollmentParseFailure(
                            reason = EnrollmentParseFailure.Reason.CONNECTION,
                            title = resources.getString(R.string.error_enroll_title),
                            message = resources.getString(R.string.error_enroll_connection)
                    )
                is JsonDataException,
                is JsonEncodingException ->
                    EnrollmentParseFailure(
                            reason = EnrollmentParseFailure.Reason.INVALID_CHALLENGE,
                            title = resources.getString(R.string.error_enroll_title),
                            message = resources.getString(R.string.error_enroll_invalid_qr)
                    )
                else ->
                    EnrollmentParseFailure(
                            reason = EnrollmentParseFailure.Reason.INVALID_CHALLENGE,
                            title = resources.getString(R.string.error_enroll_title),
                            message = resources.getString(R.string.error_enroll_invalid_qr)
                    )
            }.run {
                ChallengeParseResult.failure(this)
            }
        }
    }

    /**
     * Complete the [Challenge] and store the Identity.
     */
    override suspend fun completeChallenge(request: ChallengeCompleteRequest<EnrollmentChallenge>): ChallengeCompleteResult<ChallengeCompleteFailure> {
        return try {
            val secret = secretService.createSecret()

            // Perform API call and return result
            api.enroll(
                    url = request.challenge.enrollmentUrl,
                    secret = secret.value.encoded.toHexString(),
                    language = Locale.getDefault().language,
                    directNotificationAddress = preferences.notificationToken
            ).run {
                when (this) {
                    is ApiResponse.Success -> handleResponse(request, body, secret, headers.tiqrProtocol())
                    is ApiResponse.Failure -> handleResponse(request, body, secret, headers.tiqrProtocol())
                    is ApiResponse.NetworkError -> {
                        EnrollmentCompleteFailure(
                                reason = EnrollmentCompleteFailure.Reason.CONNECTION,
                                title = resources.getString(R.string.error_enroll_title),
                                message = resources.getString(R.string.error_enroll_connection)
                        ).run {
                            ChallengeCompleteResult.failure(this)
                        }
                    }
                    is ApiResponse.Error -> {
                        Timber.e("Error completing enrollment, request was unsuccessful")
                        EnrollmentCompleteFailure(
                                reason = EnrollmentCompleteFailure.Reason.INVALID_RESPONSE,
                                title = resources.getString(R.string.error_enroll_title),
                                message = resources.getString(R.string.error_enroll_invalid_response)
                        ).run {
                            ChallengeCompleteResult.failure(this)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error completing enrollment")

            return when (e) {
                is IOException ->
                    EnrollmentCompleteFailure(
                            reason = EnrollmentCompleteFailure.Reason.CONNECTION,
                            title = resources.getString(R.string.error_enroll_title),
                            message = resources.getString(R.string.error_enroll_connection)
                    )
                is JsonDataException,
                is JsonEncodingException ->
                    EnrollmentCompleteFailure(
                            reason = EnrollmentCompleteFailure.Reason.INVALID_RESPONSE,
                            title = resources.getString(R.string.error_enroll_title),
                            message = resources.getString(R.string.error_enroll_invalid_response)
                    )
                else ->
                    EnrollmentCompleteFailure(
                            reason = EnrollmentCompleteFailure.Reason.UNKNOWN,
                            title = resources.getString(R.string.error_enroll_title),
                            message = resources.getString(R.string.error_enroll_invalid_response)
                    )
            }.run {
                ChallengeCompleteResult.failure(this)
            }
        }
    }

    private suspend fun handleResponse(request: ChallengeCompleteRequest<EnrollmentChallenge>, response: EnrollmentResponse?, secret: Secret, protocolVersion: Int): ChallengeCompleteResult<ChallengeCompleteFailure> {
        val result = response ?: return EnrollmentCompleteFailure(
                reason = EnrollmentCompleteFailure.Reason.INVALID_RESPONSE,
                title = resources.getString(R.string.error_enroll_title),
                message = resources.getString(R.string.error_enroll_invalid_response)
        ).run {
            Timber.e("Error completing enrollment, API response is empty")
            ChallengeCompleteResult.failure(this)
        }

        if (!BuildConfig.PROTOCOL_COMPATIBILITY_MODE) {
            if (protocolVersion <= BuildConfig.PROTOCOL_VERSION) {
                return EnrollmentCompleteFailure(
                        reason = EnrollmentCompleteFailure.Reason.INVALID_RESPONSE,
                        title = resources.getString(R.string.error_enroll_title),
                        message = resources.getString(R.string.error_enroll_invalid_protocol, "v$protocolVersion")
                ).run {
                    Timber.e("Error completing enrollment, unsupported protocol version: v$protocolVersion")
                    ChallengeCompleteResult.failure(this)
                }
            }
        }

        if (result.code != EnrollmentResponse.Code.ENROLL_RESULT_SUCCESS) {
            return EnrollmentCompleteFailure(
                    reason = EnrollmentCompleteFailure.Reason.INVALID_RESPONSE,
                    title = resources.getString(R.string.error_enroll_title),
                    message = resources.getString(R.string.error_enroll_invalid_response_code, result.code)
            ).run {
                Timber.e("Error completing enrollment, unexpected response code")
                ChallengeCompleteResult.failure(this)
            }
        }

        // Insert the IdentityProvider first
        val identityProviderId = database.insertIdentityProvider(request.challenge.identityProvider)
        if (identityProviderId == -1L) {
            Timber.e("Error completing enrollment, saving identity provider failed")
            return EnrollmentCompleteFailure(
                    title = resources.getString(R.string.error_enroll_title),
                    message = resources.getString(R.string.error_enroll_saving_identity_provider)
            ).run {
                ChallengeCompleteResult.failure(this)
            }
        }
        // Then insert the Identity using the id from above
        val identityId = database.insertIdentity(request.challenge.identity.copy(identityProvider = identityProviderId))
        if (identityId == -1L) {
            Timber.e("Error completing enrollment, saving identity failed")
            return EnrollmentCompleteFailure(
                    title = resources.getString(R.string.error_enroll_title),
                    message = resources.getString(R.string.error_enroll_saving_identity)
            ).run {
                ChallengeCompleteResult.failure(this)
            }
        }
        // Copy identity with inserted id's
        val identity = request.challenge.identity.copy(id = identityId, identityProvider = identityProviderId)

        // Save secrets
        val sessionKey = secretService.createSessionKey(request.password)
        val secretId = secretService.createSecretIdentity(identity, SecretType.PIN)
        secretService.save(secretId, secret, sessionKey)

        return ChallengeCompleteResult.success()
    }
}