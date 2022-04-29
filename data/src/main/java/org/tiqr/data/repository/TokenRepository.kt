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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tiqr.data.api.TokenApi
import org.tiqr.data.repository.base.TokenRegistrarRepository
import org.tiqr.data.service.PreferenceService
import timber.log.Timber

/**
 * Repository to handle token exchange.
 *
 *
 */
@Deprecated("Deprecated because we do not use the token exchange anymore. Instead we send the FCM token directly to the server.")
class TokenRepository(private val api: TokenApi, private val preferences: PreferenceService): TokenRegistrarRepository {
    companion object {
        private const val NOT_FOUND = "NOT FOUND"
    }

    /**
     * Register the device token (received from Firebase) and save the resulting notification token.
     *
     * @param deviceToken The token received from Firebase.
     * @return The token exchanged via the token exchange.
     */
    override suspend fun registerDeviceToken(deviceToken: String) {
        try {
            val newToken = api.registerDeviceToken(deviceToken = deviceToken, notificationToken = preferences.notificationToken)
            if (newToken != NOT_FOUND) {
                withContext(Dispatchers.Main) {
                    preferences.notificationToken = newToken
                }
            } else {
                Timber.w("Token from exchange is invalid")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to register deviceToken")
        }
    }
}