/*
 * Copyright (c) 2010-2020 SURFnet bv
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

import kotlinx.coroutines.flow.Flow
import org.tiqr.data.model.Identity
import org.tiqr.data.model.IdentityWithProvider
import org.tiqr.data.service.DatabaseService
import org.tiqr.data.service.SecretService

/**
 * Repository to interact with [Identity].
 */
class IdentityRepository(private val database: DatabaseService, private val secret: SecretService) {
    /**
     * Get [Identity] with given [identifier]
     */
    fun identity(identifier: String): Flow<IdentityWithProvider?> = database.getIdentityWithProvider(identifier)

    /**
     * Get all [Identity]'s
     */
    fun allIdentities(): Flow<List<IdentityWithProvider>> = database.getIdentitiesWithProviders()

    /**
     * Set [identity] to use (or not use) biometric
     */
    suspend fun useBiometric(identity: Identity, use: Boolean) {
        identity.copy(biometricInUse = use).run {
            database.updateIdentity(this)
        }
    }

    /**
     * Upgrade [identity] to use (or not use) biometric
     */
    suspend fun upgradeToBiometric(identity: Identity, upgrade: Boolean) {
        identity.copy(biometricOfferUpgrade = upgrade).run {
            database.updateIdentity(this)
        }
    }

    /**
     * Delete [identity]
     */
    suspend fun delete(identity: Identity) = database.deleteIdentity(identity)

    /**
     * Check if there is a biometric secret for [identity]
     */
    fun hasBiometricSecret(identity: Identity): Boolean {
        return try {
            secret.encryption.keyFromPassword(password = SecretService.Type.BIOMETRIC.key).apply {
                secret.getSecret(identity = identity, type = SecretService.Type.BIOMETRIC, sessionKey = this)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}