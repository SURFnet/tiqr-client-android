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

package org.tiqr.data.service

import android.content.Context
import android.util.Base64
import org.tiqr.data.model.SecretType
import org.tiqr.data.model.Identity
import org.tiqr.data.model.Secret
import org.tiqr.data.model.SecretIdentity
import org.tiqr.data.model.SessionKey
import org.tiqr.data.model.asSecret
import org.tiqr.data.model.asSessionKey
import org.tiqr.data.security.CipherPayload
import org.tiqr.data.security.SecurityFeaturesException
import org.tiqr.data.util.extension.CompatType
import org.tiqr.data.util.extension.toCharArray
import org.tiqr.data.util.extension.toCharArrayCompat
import timber.log.Timber
import java.io.IOException
import java.security.*
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Service to securely save and retrieve secrets
 */
class SecretService(context: Context, preferenceService: PreferenceService) {
    internal val encryption = Encryption(preferenceService)
    private val store = Store(context, encryption)

    /**
     * Create a wrapper for a [Identity]
     */
    internal fun createSecretIdentity(identity: Identity, type: SecretType) = SecretIdentity(identity, type)

    /**
     * Create a [SessionKey] from the given [password]
     */
    internal fun createSessionKey(password: String): SessionKey = encryption.keyFromPassword(password)

    /**
     * Create a new [Secret]
      */
    internal fun createSecret(): Secret = encryption.randomSecretKey()

    /**
     * Save the [Secret]
     */
    internal fun save(secretIdentity: SecretIdentity, secret: Secret, sessionKey: SessionKey) {
        val civ = encryption.encrypt(secret.value.encoded, sessionKey.value)
        val deviceKey = encryption.deviceKey().asSessionKey()
        store.setSecretKey(secretIdentity.id, civ, deviceKey)
    }

    /**
     * Delete the [Secret]'s for [identity]
     */
    internal fun delete(identity: Identity) {
        listOf(SecretIdentity(identity, SecretType.PIN).id, SecretIdentity(identity, SecretType.BIOMETRIC).id).run {
            forEach { store.deleteSecretKey(it) }
        }
    }

    /**
     * Load the [Secret]
     *
     * @throws InvalidKeyException when key cannot be found
     * @throws SecurityFeaturesException when upgrading to new key fails
     */
    internal fun load(secretIdentity: SecretIdentity, sessionKey: SessionKey): Secret {
        val deviceKey: SessionKey = encryption.deviceKey().asSessionKey()
        val civ: CipherPayload = store.getSecretKey(secretIdentity.id, deviceKey) ?: throw InvalidKeyException("Requested key not found.")

        val decrypt = encryption.decrypt(civ, sessionKey.value)
        return Secret(SecretKeySpec(decrypt, "RAW")).apply {
            if (civ.iv == null) {
                // Old keys didn't store the iv, so upgrade it to a new key.
                save(secretIdentity, this, sessionKey)
            }
        }
    }

    /**
     * Helpers for storing secrets
     */
    class Store(private val context: Context, private val encryption: Encryption) {
        companion object {
            private const val KEYSTORE_FILENAME = "MobileAuthDb.kstore"
            private const val IV_SUFFIX = "-org.tiqr.iv"
        }

        private val keyStore = KeyStore.getInstance("BKS")

        init {
            if (context.getFileStreamPath(KEYSTORE_FILENAME).exists().not()) {
                // No existing KeyStore file, create it
                context.openFileOutput(KEYSTORE_FILENAME, Context.MODE_PRIVATE).use {
                    // We just created the KeyStore file, initialize the default.
                    keyStore.load(null, null)
                    // And save
                    saveKeystore(encryption.deviceKey().asSessionKey())
                }
            } else {
                var migrateKeystore = false

                // Load keystore from the file
                context.openFileInput(KEYSTORE_FILENAME).use {
                    try {
                        keyStore.load(it, encryption.deviceKey().encoded.toCharArray())
                    } catch (e: IOException) {
                        Timber.e(e, "Loading keystore failed, retrying with fallback")
                        try {
                            keyStore.load(it, encryption.deviceKey().encoded.toCharArrayCompat(CompatType.Fallback))
                            migrateKeystore = true
                        } catch (e: IOException) {
                            Timber.e(e, "Loading keystore failed, retrying with fallback pre pie")
                            try {
                                keyStore.load(it, encryption.deviceKey().encoded.toCharArrayCompat(CompatType.FallbackPrePie))
                                migrateKeystore = true
                            } catch (e: IOException) {
                                Timber.e(e, "Loading keystore failed, retrying with fallback of version 3.0.6")
                                try {
                                    keyStore.load(it, encryption.deviceKey().encoded.toCharArrayCompat(CompatType.FallbackVersion306))
                                    migrateKeystore = true
                                } catch (e: IOException) {
                                    Timber.e("Loading keystore failed and is unusable now")
                                }
                            }
                        }
                    }
                }

                // Save keystore to use the default [CharArray] conversion
                if (migrateKeystore) {
                    saveKeystore(encryption.deviceKey().asSessionKey())
                }
            }
        }

        /**
         * Save the keystore with the [sessionKey] as password
         *
         * @throws SecurityFeaturesException if saving fails
         */
        private fun saveKeystore(sessionKey: SessionKey) {
            context.openFileOutput(KEYSTORE_FILENAME, Context.MODE_PRIVATE).use {
                try {
                    sessionKey.value.encoded.toCharArray()
                    keyStore.store(it, sessionKey.value.encoded.toCharArray())
                } catch (e: Exception) {
                    Timber.e(e)
                    throw SecurityFeaturesException(message = "Saving the secret key failed", cause = e)
                }
            }
        }

        /**
         * Get the saved [SecretKey] and optionally migrates to new style password
         *
         * @throws SecurityFeaturesException if saving fails
         */
        internal fun getSecretKey(id: String, sessionKey: SessionKey): CipherPayload? {
            var ctEntry: KeyStore.SecretKeyEntry?
            var ivEntry: KeyStore.SecretKeyEntry?
            var migrateKeys = false

            try {
                val protection = KeyStore.PasswordProtection(sessionKey.value.encoded.toCharArray())
                ctEntry = keyStore.getEntry(id, protection) as? KeyStore.SecretKeyEntry
                ivEntry = keyStore.getEntry(id + IV_SUFFIX, protection) as? KeyStore.SecretKeyEntry
            } catch (e: UnrecoverableKeyException) {
                Timber.e(e, "Getting secretkey failed, retrying with fallback")
                try {
                    val protection = KeyStore.PasswordProtection(sessionKey.value.encoded.toCharArrayCompat(CompatType.Fallback))
                    ctEntry = keyStore.getEntry(id, protection) as? KeyStore.SecretKeyEntry
                    ivEntry = keyStore.getEntry(id + IV_SUFFIX, protection) as? KeyStore.SecretKeyEntry
                    migrateKeys = true
                } catch (e: UnrecoverableKeyException) {
                    Timber.e(e, "Getting secretkey failed, retrying with fallback pre pie")
                    try {
                        val protection = KeyStore.PasswordProtection(sessionKey.value.encoded.toCharArrayCompat(CompatType.FallbackPrePie))
                        ctEntry = keyStore.getEntry(id, protection) as? KeyStore.SecretKeyEntry
                        ivEntry = keyStore.getEntry(id + IV_SUFFIX, protection) as? KeyStore.SecretKeyEntry
                        migrateKeys = true
                    } catch (e: UnrecoverableKeyException) {
                        Timber.e(e, "Getting secretkey failed, retrying with fallback of version 3.0.6")
                        try {
                            val protection = KeyStore.PasswordProtection(sessionKey.value.encoded.toCharArrayCompat(CompatType.FallbackVersion306))
                            ctEntry = keyStore.getEntry(id, protection) as? KeyStore.SecretKeyEntry
                            ivEntry = keyStore.getEntry(id + IV_SUFFIX, protection) as? KeyStore.SecretKeyEntry
                            migrateKeys = true
                        } catch (e: UnrecoverableKeyException) {
                            ctEntry = null
                            ivEntry = null
                        }
                    }
                }
            }

            if (ctEntry == null || ctEntry.secretKey == null) {
                return null
            }

            return CipherPayload(ctEntry.secretKey.encoded, ivEntry?.secretKey?.encoded).apply {
                if (migrateKeys) {
                    setSecretKey(id, this, sessionKey)
                }
            }
        }

        /**
         * Set the [SecretKey] and save it into the [KeyStore]
         *
         * @throws SecurityFeaturesException if saving fails
         */
        internal fun setSecretKey(id: String, payload: CipherPayload, sessionKey: SessionKey) {
            val ctEntry = KeyStore.SecretKeyEntry(SecretKeySpec(payload.cipherText, "RAW"))
            val ivEntry = KeyStore.SecretKeyEntry(SecretKeySpec(payload.iv, "RAW"))

            with(KeyStore.PasswordProtection(sessionKey.value.encoded.toCharArray())) {
                keyStore.setEntry(id, ctEntry, this)
                keyStore.setEntry(id + IV_SUFFIX, ivEntry, this)
            }
            saveKeystore(sessionKey)
        }

        /**
         * Delete the [SecretKey]
          */
        internal fun deleteSecretKey(alias: String) {
            listOf(alias, alias + IV_SUFFIX).run {
                forEach {
                    if (keyStore.containsAlias(it)) {
                        keyStore.deleteEntry(it)
                    }
                }
            }
            saveKeystore(encryption.deviceKey().asSessionKey())
        }
    }

    /**
     * Helpers to secure secrets
     */
    class Encryption(private val preferenceService: PreferenceService) {
        companion object {
            private const val ALGORITHM_MASTER_KEY = "HMACSHA256"
            private const val ALGORITHM_RANDOM = "SHA1PRNG"
            private const val ALGORITHM_PASSWORD_KEY = "PBEWithSHA256And256BitAES-CBC-BC"
            private const val SALT_BYTES_SIZE = 20
            private const val IV_LENGTH = 16
            private const val CIPHER_TRANSFORMATION = "AES/CBC/NoPadding"
            private const val CIPHER_KEY_ITERATIONS = 1500
            private const val CIPHER_KEY_SIZE = 16
        }

        private val random = SecureRandom.getInstance(ALGORITHM_RANDOM)
        private val factory = SecretKeyFactory.getInstance(ALGORITHM_PASSWORD_KEY)
        private val generator = KeyGenerator.getInstance(ALGORITHM_MASTER_KEY).apply { init(random) }

        /**
         * Get the saved device key, or generate (and save) one randomly
         */
        internal fun deviceKey(): SecretKey {
            val value = preferenceService.deviceKey
            return if (value != null) {
                Base64.decode(value, Base64.DEFAULT)
            } else {
                randomBytes().also {
                    preferenceService.deviceKey = Base64.encodeToString(it, Base64.DEFAULT)
                }
            }.run {
                SecretKeySpec(this, "RAW")
            }
        }

        /**
         * Get the saved salt, or generate (and save) one randomly
         */
        private fun salt(): ByteArray {
            val value = preferenceService.salt
            return if (value != null) {
                Base64.decode(value, Base64.DEFAULT)
            } else {
                randomBytes().also {
                    preferenceService.salt = Base64.encodeToString(it, Base64.DEFAULT)
                }
            }
        }

        /**
         * Convert a password to an encryption compatible [SecretKey]
         */
        internal fun keyFromPassword(password: String, salt: ByteArray = salt()): SessionKey =
                PBEKeySpec(
                        password.toCharArray(),
                        salt,
                        CIPHER_KEY_ITERATIONS,
                        CIPHER_KEY_SIZE
                ).run {
                    factory.generateSecret(this)
                }.run {
                    asSessionKey()
                }

        /**
         * Generate a random byte
         */
        private fun randomBytes(size: Int = SALT_BYTES_SIZE): ByteArray = ByteArray(size).apply {
            random.nextBytes(this)
        }

        /**
         * Generate a random secret key
         */
        internal fun randomSecretKey(): Secret {
            return SecretKeySpec(randomKey().encoded, ALGORITHM_MASTER_KEY).asSecret()
        }

        /**
         * Generate a random key
         */
        private fun randomKey(): Key = generator.generateKey()

        /**
         * Generate a Initialisation Vector
         */
        private fun generateIv(): ByteArray = randomBytes(IV_LENGTH)

        /**
         * Encrypt a plaintext using the method defined in [CIPHER_TRANSFORMATION]
         *
         * @throws SecurityFeaturesException
         */
        internal fun encrypt(text: ByteArray, key: Key): CipherPayload {
            try {
                val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).also {
                    it.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(generateIv()))
                }

                val cipherText = ByteArray(cipher.getOutputSize(text.size))
                cipher.update(text, 0, text.size, cipherText, 0).also {
                    cipher.doFinal(cipherText, it)
                }

                return CipherPayload(cipherText, cipher.iv)
            } catch (e: Exception) {
                Timber.e(e, "Encrypt failed")
                throw SecurityFeaturesException()
            }
        }

        /**
         * Decrypts the cipher according to CIPHER_TRANSFORMATION
         *
         * @throws InvalidKeyException
         * @throws SecurityFeaturesException
         */
        internal fun decrypt(payload: CipherPayload, key: Key): ByteArray {
            try {
                val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).also {
                    it.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(payload.iv))
                }

                val plainText = ByteArray(cipher.getOutputSize(payload.cipherText.size))
                cipher.update(payload.cipherText, 0, payload.cipherText.size, plainText, 0).also {
                    cipher.doFinal(plainText, it)
                }

                return plainText
            } catch (e: Exception) {
                Timber.e(e, "Decrypt failed")
                when (e) {
                    is NoSuchAlgorithmException,
                    is NoSuchPaddingException -> throw SecurityFeaturesException()
                    is InvalidKeyException,
                    is ShortBufferException,
                    is IllegalBlockSizeException,
                    is BadPaddingException,
                    is InvalidAlgorithmParameterException -> throw InvalidKeyException()
                    else -> throw SecurityFeaturesException()
                }
            }
        }
    }
}
