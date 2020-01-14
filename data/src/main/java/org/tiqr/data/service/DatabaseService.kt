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

package org.tiqr.data.service

import kotlinx.coroutines.flow.Flow
import org.tiqr.data.database.TiqrDao
import org.tiqr.data.database.TiqrDatabase
import org.tiqr.data.model.Identity
import org.tiqr.data.model.IdentityProvider

/**
 * Service to interact with the database.
 */
class DatabaseService(private val dao: TiqrDao) {
    /**
     * Insert a identity and identityprovider
     */
    suspend fun insertIdentityAndIdentityProvider(identity: Identity, identityProvider: IdentityProvider) = dao.insertIdentityAndIdentityProvider(identity, identityProvider)

    //region Identity
    /**
     * Get the count of all identities
     */
    fun getIdentityCount(): Flow<Int> = dao.identityCount()

    /**
     * Get an identity by (row) id.
     */
    suspend fun getIdentity(id: Long): Identity? = dao.getIdentity(id)

    /**
     * Get an identity by identity provider identifier.
     */
    suspend fun getIdentity(identityProviderId: String): Identity? = dao.getIdentity(identityProviderId)

    /**
     * Get an identity by identity identifier and identity provider identifier.
     */
    suspend fun getIdentity(identityId: String, identityProviderId: String): Identity? = dao.getIdentity(identityId, identityProviderId)

    /**
     * Get all identities.
     */
    suspend fun getAllIdentities(): List<Identity> = dao.getIdentities()

    /**
     * Block all identities.
     */
    suspend fun blockAllIdentities(): Int = dao.blockAllIdentities()
    //endregion

    //region IdentityProvider
    /**
     * Get an identity provider by id.
     */
    suspend fun getIdentityProvider(id: Long): IdentityProvider? = dao.getIdentityProvider(id)

    /**
     * Get an identity provider by id.
     */
    suspend fun getIdentityProviderByIdentifier(identifier: String): IdentityProvider? = dao.getIdentityProvider(identifier)

    /**
     * Get all identity providers.
     */
    fun getAllIdentityProviders(): Flow<List<IdentityProvider>> = dao.getIdentityProviders()
    //endregion
}
