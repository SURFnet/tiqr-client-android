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
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.tiqr.data.BuildConfig
import org.tiqr.data.R
import org.tiqr.data.api.TiqrApi
import org.tiqr.data.model.*
import org.tiqr.data.repository.base.ChallengeRepository
import org.tiqr.data.service.DatabaseService
import org.tiqr.data.service.PreferenceService
import org.tiqr.data.service.SecretService
import timber.log.Timber

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
                protocolVersion = url.pathSegments.getOrNull(4) ?: "1",
                identityProvider = identityProvider,
                identity = identity,
                returnUrl = url.query?.toHttpUrlOrNull()?.toString(),
                sessionKey = url.pathSegments[1],
                challenge = url.pathSegments[2],
                isStepUpChallenge = url.username.isNotEmpty(),
                serviceProviderDisplayName = url.pathSegments.getOrNull(3) ?: resources.getString(R.string.error_auth_empty_service_provider),
                serviceProviderIdentifier = ""
        ).run {
            ChallengeParseResult.success(this)
        }
    }

    override suspend fun completeAuthenticationChallenge(challenge: AuthenticationChallenge, password: String, type: SecretService.Type): ChallengeCompleteResult<ChallengeCompleteFailure> {
        TODO("not implemented")
    }
}