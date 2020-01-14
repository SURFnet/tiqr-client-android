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
import org.tiqr.data.model.Identity
import org.tiqr.data.model.IdentityProvider
import timber.log.Timber

@Database(entities = [Identity::class, IdentityProvider::class], version = 8, exportSchema = true)
abstract class TiqrDatabase : RoomDatabase() {
    companion object {
        const val DB_NAME = "identities.db"

        val FROM_4_TO_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version $startVersion to version $endVersion.")
                Timber.d("Adds 2 columns.")
                with(database) {
                    try {
                        beginTransaction()
                        execSQL("ALTER TABLE identity ADD COLUMN showFingerPrintUpgrade INTEGER NOT NULL DEFAULT 1;")
                        execSQL("ALTER TABLE identity ADD COLUMN useFingerPrint INTEGER NOT NULL DEFAULT 0;")
                    } finally {
                        endTransaction()
                    }
                }
            }
        }

        val FROM_5_TO_7: Migration = object : Migration(5, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version $startVersion to version $endVersion.")
                Timber.d("Changes LOGO from BLOB to TEXT.")
                with(database) {
                    try {
                        beginTransaction()
                        execSQL("ALTER TABLE identityprovider RENAME TO identityprovider_old;")
                        execSQL("CREATE TABLE identityprovider (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, displayName TEXT NOT NULL, identifier TEXT NOT NULL, authenticationUrl TEXT NOT NULL, ocraSuite TEXT NOT NULL, infoUrl TEXT, logo TEXT);")
                        execSQL("INSERT INTO identityprovider (_id, displayName, identifier, authenticationUrl, ocraSuite, infoUrl, logo) SELECT _id, displayName, identifier, authenticationUrl, ocraSuite, infoUrl, '' FROM identityprovider_old;")
                        execSQL("DROP TABLE identityprovider_old;")
                    } finally {
                        endTransaction()
                    }
                }
            }
        }

        val FROM_7_TO_8: Migration = object : Migration(7,8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version $startVersion to version $endVersion.")
                Timber.d("Adds FK's and indexes.")
                with(database) {
                    try {
                        beginTransaction()
                        execSQL("ALTER TABLE identity RENAME TO identity_old;")
                        execSQL("CREATE TABLE identity (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, blocked INTEGER NOT NULL DEFAULT 0, displayName TEXT NOT NULL, identifier TEXT NOT NULL, identityProvider INTEGER NOT NULL, sortIndex INTEGER NOT NULL DEFAULT 0, showFingerPrintUpgrade INTEGER NOT NULL DEFAULT 1, useFingerPrint INTEGER NOT NULL DEFAULT 0, FOREIGN KEY (identityProvider) REFERENCES identityprovider(_id) ON DELETE CASCADE);")
                        execSQL("CREATE UNIQUE INDEX id_idx ON identity(_id);")
                        execSQL("CREATE INDEX identity_provider_idx on identity(identityProvider);")
                        execSQL("CREATE INDEX identifier_idx ON identity(identifier);")
                        execSQL("INSERT INTO identity (_id, blocked, displayName, identifier, identityProvider, sortIndex, showFingerPrintUpgrade, useFingerPrint) SELECT _id, blocked, displayName, identifier, identityProvider, sortIndex, showFingerPrintUpgrade, useFingerPrint FROM identity_old;")
                        execSQL("DROP TABLE identity_old;")

                        execSQL("ALTER TABLE identityprovider RENAME TO identityprovider_old;")
                        execSQL("CREATE TABLE identityprovider (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, displayName TEXT NOT NULL, identifier TEXT NOT NULL, authenticationUrl TEXT NOT NULL, ocraSuite TEXT NOT NULL, infoUrl TEXT, logo TEXT);")
                        execSQL("CREATE INDEX ip_identifier_idx on identityprovider(identifier);")
                        execSQL("INSERT INTO identityprovider (_id, displayName, identifier, authenticationUrl, ocraSuite, infoUrl, logo) SELECT _id, displayName, identifier, authenticationUrl, ocraSuite, infoUrl, '' FROM identityprovider_old;")
                        execSQL("DROP TABLE identityprovider_old;")
                    } finally {
                        endTransaction()
                    }
                }
            }
        }
    }

    abstract fun tiqrDao(): TiqrDao
}