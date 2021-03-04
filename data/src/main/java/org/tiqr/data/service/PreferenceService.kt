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

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import timber.log.Timber
import java.io.File

/**
 * Service to save and retrieve data saved in shared preferences.
 */
class PreferenceService(private val context: Context) {
    companion object {
        private const val PREFERENCE_NOTIFICATION = ":notification"
        private const val PREFERENCE_SECURITY = ":security"
        private const val PREFERENCE_VERSION = ":version"

        private const val PREFS_KEY_VERSION = "version"
        private const val PREFS_KEY_TOKEN = "notification_token"
        private const val PREFS_KEY_SALT = "salt"
        private const val PREFS_KEY_DEVICE_KEY = "device_key"

        private const val PREFERENCE_CURRENT_VERSION = 2 // bump to migrate
    }

    private val notificationSharedPreferences = context.getSharedPreferences(context.packageName + PREFERENCE_NOTIFICATION, Context.MODE_PRIVATE)
    private val securitySharedPreferences = context.getSharedPreferences(context.packageName + PREFERENCE_SECURITY, Context.MODE_PRIVATE)
    private val versionSharedPreference = context.getSharedPreferences(context.packageName + PREFERENCE_VERSION, Context.MODE_PRIVATE)

    var notificationToken: String?
        get() = notificationSharedPreferences.getString(PREFS_KEY_TOKEN, null)
        set(value) = notificationSharedPreferences.edit { putString(PREFS_KEY_TOKEN, value) }

    var salt: String?
        get() = securitySharedPreferences.getString(PREFS_KEY_SALT, null)
        set(value) = securitySharedPreferences.edit { putString(PREFS_KEY_SALT, value) }

    var deviceKey: String?
        get() = securitySharedPreferences.getString(PREFS_KEY_DEVICE_KEY, null)
        set(value) = securitySharedPreferences.edit { putString(PREFS_KEY_DEVICE_KEY, value) }

    private var prefsVersion: Int?
        get() {
            val current = versionSharedPreference.getInt(PREFS_KEY_VERSION, 0)
            return if (current > 0) current else null
        }
        set(value) {
            value?.let {
                versionSharedPreference.edit {
                    putInt(PREFS_KEY_VERSION, it)
                }
            }
        }

    init {
        // Run the migration(s) as soon as this gets initialized
        migratePreference()
    }

    //region Migration
    /**
     * Migrate old shared preference to new structure.
     */
    private fun migratePreference() {
        val currentVersion = prefsVersion
        if (currentVersion == null || currentVersion < PREFERENCE_CURRENT_VERSION) {
            Timber.d("Migration needed for preferences")
            migratePreferencesToV2()
        } else {
            Timber.d("No migration needed for preferences, but try to cleanup dangling files")
            cleanupOldFiles()
        }
    }

    private fun migratePreferencesToV2() {
        fun migrateSetting() {
            val oldTokenKey = "sa_notificationToken"
            val oldFile = getSharedPrefsFile("SA_PREFS")
            if (oldFile.exists()) {
                val oldPreferences = context.getSharedPreferences(oldFile.nameWithoutExtension, Context.MODE_PRIVATE)
                if (oldPreferences.contains(oldTokenKey)) {
                    val oldToken = oldPreferences.getString(oldTokenKey, null)
                    oldToken?.let {
                        notificationToken = it
                        oldPreferences.edit {
                            clear()
                        }
                    }
                }

                Timber.d("Preference file %s migrated", oldFile.name)
                val deleteResult = deletePrefsFile(oldFile.nameWithoutExtension)
                Timber.d("Preference file deleted $deleteResult")
            }
        }

        fun migrateSecurity() {
            val oldSaltKey = "salt"
            val oldDeviceKey = "deviceKey"
            val oldFile = getSharedPrefsFile("securitySettings")
            if (oldFile.exists()) {
                val oldPreferences = context.getSharedPreferences(oldFile.nameWithoutExtension, Context.MODE_PRIVATE)
                if (oldPreferences.contains(oldSaltKey)) {
                    val oldSalt = oldPreferences.getString(oldSaltKey, null)
                    oldSalt?.let {
                        salt = it
                        oldPreferences.edit {
                            remove(oldSaltKey)
                        }
                    }
                }

                if (oldPreferences.contains(oldDeviceKey)) {
                    val oldDevice = oldPreferences.getString(oldDeviceKey, null)
                    oldDevice?.let {
                        deviceKey = it
                        oldPreferences.edit {
                            remove(oldDeviceKey)
                        }
                    }
                }

                Timber.d("Preference file %s migrated", oldFile.name)
                val deleteResult = deletePrefsFile(oldFile.nameWithoutExtension)
                Timber.d("Preference file deleted $deleteResult")
            }
        }

        migrateSetting()
        migrateSecurity()

        cleanupOldFiles()

        prefsVersion = 2
    }

    /**
     * Cleanup the old dangling files.
     */
    private fun cleanupOldFiles() {
        // Migrated preference files
        listOf("SA_PREFS", "securitySettings").forEach {
            deletePrefsFile(it)
        }
    }

    /**
     * Delete the preference file with given [name]
     */
    private fun deletePrefsFile(name: String) {
        if (Build.VERSION.SDK_INT >= 24) {
            val deleteResult = context.deleteSharedPreferences(name)
            Timber.d("File $name deleted: $deleteResult")
            return
        }

        val prefsFile = getSharedPrefsFile(name)
        val prefsBackup = getSharedPrefsBackup(prefsFile)

        val fileDeleteResult = prefsFile.delete()
        Timber.d("File $name deleted: $fileDeleteResult")
        val backupDeleteResult = prefsBackup.delete()
        Timber.d("File $name deleted: $backupDeleteResult")
    }

    /**
     * Get the shared preferences file with given [name]
     */
    private fun getSharedPrefsFile(name: String): File {
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        return File(prefsDir, "$name.xml")
    }

    /**
     * Get the backup file for given shared preferences file
     */
    private fun getSharedPrefsBackup(prefsFile: File) = File(prefsFile.path + ".bak")
    //endregion
}