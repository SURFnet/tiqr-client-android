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

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.transaction
import org.tiqr.data.model.Identity
import org.tiqr.data.model.IdentityProvider
import timber.log.Timber

@Database(entities = [Identity::class, IdentityProvider::class], version = 9, exportSchema = true)
abstract class TiqrDatabase : RoomDatabase() {
    companion object {
        const val DB_NAME = "identities.db"

        val FROM_4_TO_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version $startVersion to version $endVersion.")
                Timber.d("Adds 2 columns.")

                database.transaction {
                    execSQL("ALTER TABLE identity ADD COLUMN showFingerPrintUpgrade INTEGER NOT NULL DEFAULT 1;")
                    execSQL("ALTER TABLE identity ADD COLUMN useFingerPrint INTEGER NOT NULL DEFAULT 0;")
                }
            }
        }

        val FROM_5_TO_7: Migration = object : Migration(5, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version $startVersion to version $endVersion.")
                Timber.d("Changes LOGO from BLOB to TEXT.")

                database.transaction {
                    execSQL("CREATE TABLE new_identityprovider (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, displayName TEXT NOT NULL, identifier TEXT NOT NULL, authenticationUrl TEXT NOT NULL, ocraSuite TEXT NOT NULL, infoUrl TEXT, logo TEXT);")
                    execSQL("INSERT INTO new_identityprovider (_id, displayName, identifier, authenticationUrl, ocraSuite, infoUrl) SELECT _id, displayName, identifier, authenticationUrl, ocraSuite, infoUrl FROM identityprovider;")
                    execSQL("DROP TABLE identityprovider;")
                    execSQL("ALTER TABLE new_identityprovider RENAME TO identityprovider;")
                }
            }
        }

        // From version 8 onwards Room is being used

        val FROM_7_TO_8: Migration = object : Migration(7,8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version $startVersion to version $endVersion.")
                Timber.d("Adds FK's and indexes.")

                database.transaction {
                    execSQL("CREATE TABLE new_identity (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, displayName TEXT NOT NULL, identifier TEXT NOT NULL, identityProvider INTEGER NOT NULL, blocked INTEGER NOT NULL DEFAULT 0, sortIndex INTEGER NOT NULL, showFingerPrintUpgrade INTEGER NOT NULL DEFAULT 1, useFingerPrint INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(identityProvider) REFERENCES identityprovider(_id) ON UPDATE NO ACTION ON DELETE CASCADE);")
                    execSQL("INSERT INTO new_identity (_id, displayName, identifier, identityProvider, blocked, sortIndex, showFingerPrintUpgrade, useFingerPrint) SELECT _id, displayName, identifier, identityProvider, blocked, sortIndex, showFingerPrintUpgrade, useFingerPrint FROM identity;")
                    execSQL("DROP TABLE identity;")
                    execSQL("ALTER TABLE new_identity RENAME TO identity;")
                    execSQL("CREATE UNIQUE INDEX id_idx ON identity(_id);")
                    execSQL("CREATE INDEX identity_provider_idx ON identity(identityProvider);")
                    execSQL("CREATE INDEX identifier_idx ON identity(identifier);")

                    execSQL("CREATE TABLE new_identityprovider (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, displayName TEXT NOT NULL, identifier TEXT NOT NULL, authenticationUrl TEXT NOT NULL, ocraSuite TEXT NOT NULL, infoUrl TEXT, logo TEXT);")
                    execSQL("INSERT INTO new_identityprovider (_id, displayName, identifier, authenticationUrl, ocraSuite, infoUrl, logo) SELECT _id, displayName, identifier, authenticationUrl, ocraSuite, infoUrl, logo FROM identityprovider;")
                    execSQL("DROP TABLE identityprovider;")
                    execSQL("ALTER TABLE new_identityprovider RENAME TO identityprovider;")
                    execSQL("CREATE INDEX ip_identifier_idx ON identityprovider(identifier)")
                }
            }
        }

        val FROM_8_TO_9: Migration = object : Migration(8,9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version $startVersion to version $endVersion.")
                Timber.d("Renames fingerprint to biometric. Renames indexes.")

                database.transaction {
                    execSQL("CREATE TABLE new_identity (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, displayName TEXT NOT NULL, identifier TEXT NOT NULL, identityProvider INTEGER NOT NULL, blocked INTEGER NOT NULL DEFAULT 0, sortIndex INTEGER NOT NULL, biometricInUse INTEGER NOT NULL DEFAULT 0, biometricOfferUpgrade INTEGER NOT NULL DEFAULT 1, FOREIGN KEY(identityProvider) REFERENCES identityprovider(_id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                    execSQL("INSERT INTO new_identity (_id, displayName, identifier, identityProvider, blocked, sortIndex, biometricInUse, biometricOfferUpgrade) SELECT _id, displayName, identifier, identityProvider, blocked, sortIndex, useFingerPrint, showFingerPrintUpgrade FROM identity;")
                    execSQL("DROP TABLE identity;")
                    execSQL("ALTER TABLE new_identity RENAME TO identity;")
                    execSQL("CREATE UNIQUE INDEX index_identity__id ON identity(_id);")
                    execSQL("CREATE INDEX index_identity_identityProvider on identity(identityProvider);")
                    execSQL("CREATE INDEX index_identity_identifier ON identity(identifier);")

                    execSQL("CREATE TABLE new_identityprovider (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, displayName TEXT NOT NULL, identifier TEXT NOT NULL, authenticationUrl TEXT NOT NULL, ocraSuite TEXT NOT NULL, infoUrl TEXT, logo TEXT);")
                    execSQL("INSERT INTO new_identityprovider (_id, displayName, identifier, authenticationUrl, ocraSuite, infoUrl, logo) SELECT _id, displayName, identifier, authenticationUrl, ocraSuite, infoUrl, logo FROM identityprovider;")
                    execSQL("DROP TABLE identityprovider;")
                    execSQL("ALTER TABLE new_identityprovider RENAME TO identityprovider;")
                    execSQL("CREATE INDEX index_identityprovider_identifier on identityprovider(identifier);")
                }
            }
        }
    }

    abstract fun tiqrDao(): TiqrDao
}