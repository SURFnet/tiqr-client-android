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
import org.tiqr.data.api.HeaderInjector.Companion.HEADER_PROTOCOL
import org.tiqr.data.api.TiqrApi
import org.tiqr.data.model.*
import org.tiqr.data.repository.base.ChallengeRepository
import org.tiqr.data.service.DatabaseService
import org.tiqr.data.service.PreferenceService
import org.tiqr.data.service.SecretService
import org.tiqr.data.util.extension.isHttpOrHttps
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
        private val api: TiqrApi,
        private val resources: Resources,
        private val database: DatabaseService,
        private val secretService: SecretService,
        private val preferences: PreferenceService
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
                                returnUrl = url.query?.toDecodedUrlStringOrNull(), //TODO: check if this should be set
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
                    //FIXME: when server responds with just text 'false' with http 200 instead of http 4xx.
                    // this means the server cannot handle this challenge because it is invalid (?).
                    // Ideally the server should respond with an error http 4xx.
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
     * Complete the [challenge] and store the Identity.
     */
    override suspend fun completeChallenge(challenge: EnrollmentChallenge, password: String): ChallengeCompleteResult<ChallengeCompleteFailure> {
        return try {
            // Perform API call and return result
            api.enroll(
                    url = challenge.enrollmentUrl,
                    secret = secretService.encryption.randomSecretKey().encoded.toHexString(),
                    language = Locale.getDefault().language,
                    notificationAddress = preferences.notificationToken
            ).run {
                if (isSuccessful) {
                    when (val protocol = headers()[HEADER_PROTOCOL]) {
                        null, "1" -> { // Unsupported Ascii-response
                            return EnrollmentCompleteFailure(
                                    reason = EnrollmentCompleteFailure.Reason.INVALID_RESPONSE,
                                    title = resources.getString(R.string.error_enroll_title),
                                    message = resources.getString(R.string.error_enroll_invalid_protocol, "v$protocol")
                            ).run {
                                Timber.e("Error completing enrollment, unsupported protocol version: v$protocol")
                                ChallengeCompleteResult.failure(this)
                            }
                        }
                    }

                    if (body()?.responseCode != 1) {
                        return EnrollmentCompleteFailure(
                                reason = EnrollmentCompleteFailure.Reason.INVALID_RESPONSE,
                                title = resources.getString(R.string.error_enroll_title),
                                message = resources.getString(R.string.error_enroll_invalid_response_code, body()?.responseCode.toString())
                        ).run {
                            Timber.e("Error completing enrollment, unexpected response code")
                            ChallengeCompleteResult.failure(this)
                        }
                    }

                    // Insert the IdentityProvider first
                    val identityProviderId = database.insertIdentityProvider(challenge.identityProvider)
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
                    val identityId = database.insertIdentity(challenge.identity.copy(identityProvider = identityProviderId))
                    if (identityId == -1L) {
                        Timber.e("Error completing enrollment, saving identity failed")
                        return EnrollmentCompleteFailure(
                                title = resources.getString(R.string.error_enroll_title),
                                message = resources.getString(R.string.error_enroll_saving_identity_provider)
                        ).run {
                            ChallengeCompleteResult.failure(this)
                        }
                    }
                    // Copy identity with inserted id's
                    val identity = challenge.identity.copy(id = identityId, identityProvider = identityProviderId)

                    // Save secrets
                    val sessionKey = secretService.encryption.keyFromPassword(password)
                    secretService.createSecret(identity)
                    secretService.save(identity, sessionKey)

                    ChallengeCompleteResult.success()
                } else {
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
                    //FIXME: when server responds with just text 'false' with http 200 instead of http 4xx.
                    // this means the server cannot handle this challenge because it is invalid (?).
                    // Ideally the server should respond with an error http 4xx.
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
}