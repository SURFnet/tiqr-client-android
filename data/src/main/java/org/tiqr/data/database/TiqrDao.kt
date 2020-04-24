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

package org.tiqr.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.tiqr.data.model.Identity
import org.tiqr.data.model.IdentityProvider
import org.tiqr.data.model.IdentityWithProvider

@Dao
interface TiqrDao {
    //region Identity
    @Insert
    suspend fun insertIdentity(identity: Identity): Long

    @Update
    suspend fun updateIdentity(identity: Identity)

    @Delete
    suspend fun deleteIdentity(identity: Identity)

    @Query(value = "SELECT * FROM identity ORDER BY sortIndex;")
    suspend fun getIdentities(): List<Identity>

    @Query(value = "SELECT * FROM identity WHERE _id = :id;")
    suspend fun getIdentity(id: Long): Identity?

    @Query(value = """
        SELECT identity.* FROM identity 
        INNER JOIN identityprovider ON identityprovider._id = identity.identityProvider 
        WHERE identityprovider.identifier = :identityProviderId;
        """)
    suspend fun getIdentity(identityProviderId: String): Identity?

    @Query(value = """
        SELECT identity.* FROM identity 
        INNER JOIN identityprovider ON identityprovider._id = identity.identityProvider 
        WHERE identity.identifier = :identityId AND identityprovider.identifier = :identityProviderId LIMIT 1;
        """)
    suspend fun getIdentity(identityId: String, identityProviderId: String): Identity?

    @Query(value = "UPDATE identity SET blocked = 1 WHERE _id = :id;")
    suspend fun blockIdentity(id: Long): Int

    @Query(value = "SELECT COUNT(DISTINCT blocked) AS allblocked FROM identity WHERE blocked = 1;")
    fun allIdentitiesBlocked(): Flow<Boolean>

    @Query(value = "UPDATE identity SET blocked = 1;")
    suspend fun blockAllIdentities(): Int

    @Query(value = "SELECT COUNT(_id) FROM identity;")
    fun identityCount(): Flow<Int>
    //endregion

    //region IdentityProvider
    @Insert
    suspend fun insertIdentityProvider(identityProvider: IdentityProvider): Long

    @Query("SELECT * FROM identityprovider")
    fun getIdentityProviders(): Flow<List<IdentityProvider>>

    @Query(value = "SELECT * FROM identityprovider WHERE _id = :id")
    suspend fun getIdentityProvider(id: Long): IdentityProvider?

    @Query(value = "SELECT * FROM identityprovider WHERE identifier = :identifier LIMIT 1")
    suspend fun getIdentityProvider(identifier: String): IdentityProvider?
    //endregion

    @Transaction
    @Query(value = "SELECT * FROM identity")
    fun identitiesWithProvider(): Flow<List<IdentityWithProvider>>

    @Transaction
    @Query(value = "SELECT * FROM identity WHERE identifier = :identifier")
    fun identityWithProvider(identifier: String): Flow<IdentityWithProvider?>
}