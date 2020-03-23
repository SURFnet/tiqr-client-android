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
import androidx.collection.SimpleArrayMap
import org.tiqr.data.model.Identity
import org.tiqr.data.security.CipherPayload
import org.tiqr.data.security.SecurityFeaturesException
import org.tiqr.data.util.extension.toCharArray
import org.tiqr.data.util.extension.toCharArrayFallback
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
@Suppress("MemberVisibilityCanBePrivate")
class SecretService(context: Context, preferenceService: PreferenceService) {
    companion object {
        private const val BIOMETRIC_SUFFIX = "org.tiqr.FP"
    }

    internal val encryption = Encryption(preferenceService)
    private val store = Store(context, encryption)

    private val secrets = SimpleArrayMap<String, SecretKey>()

    enum class Type {
        PIN_CODE,
        BIOMETRIC
    }

    // TODO: check access modifiers

    /**
     * Create a new secret and ad it to the [secrets] collection.
     * Only to be used for enrollment.
     */
    fun createSecret(identity: Identity, type: Type = Type.PIN_CODE) {
        addSecret(identity.toId(type), encryption.randomSecretKey())
    }

    /**
     * Add this new [secret] to the [secrets] collection.
     */
    private fun addSecret(id: String, secret: SecretKey) {
        secrets.put(id, secret)
    }

    /**
     * Get the [SecretKey]
     *
     * @throws InvalidKeyException when key cannot be found
     * @throws SecurityFeaturesException when upgrading to new key fails
     */
    private fun getSecret(identity: Identity, type: Type, sessionKey: SecretKey): SecretKey {
        return secrets[identity.toId(type)] ?: load(identity, type, sessionKey)
    }


    /**
     * Save the key
     */
    fun save(identity: Identity, sessionKey: SecretKey, type: Type = Type.PIN_CODE) {
        val secret = getSecret(identity, type, sessionKey)
        val civ = encryption.encrypt(secret.encoded, sessionKey)
        store.setSecretKey(identity.toId(type), civ, encryption.deviceKey())
    }

    /**
     * Load the [SecretKey]
     *
     * @throws InvalidKeyException when key cannot be found
     * @throws SecurityFeaturesException when upgrading to new key fails
     */
    private fun load(identity: Identity, type: Type, sessionKey: SecretKey): SecretKey {
        val id: String = identity.toId(type)
        val civ: CipherPayload = store.getSecretKey(id, encryption.deviceKey())
                ?: throw InvalidKeyException("Requested key not found.")

        return SecretKeySpec(encryption.decrypt(civ, sessionKey), "RAW").apply {
            addSecret(id, this)

            if (civ.iv == null) {
                // Old keys didn't store the iv, so upgrade it to a new key.
                save(identity, sessionKey, type)
            }
        }
    }

    /**
     * Convert this [Identity] to a [String] representation for the given [Type]
     */
    private fun Identity.toId(type: Type): String {
        return when (type) {
            Type.PIN_CODE -> id.toString()
            Type.BIOMETRIC -> id.toString() + BIOMETRIC_SUFFIX
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
                }
            } else {
                // Load keystore from the file
                context.openFileInput(KEYSTORE_FILENAME).use {
                    try {
                        keyStore.load(it, encryption.deviceKey().toCharArray())
                    } catch (e: IOException) {
                        Timber.e(e, "Loading keystore failed")
                        if (e.cause is UnrecoverableKeyException) {
                            Timber.e(e, "Loading keystore failed, retrying with old style password")
                            // Load KeyStore from the file with old style password
                            keyStore.load(it, encryption.deviceKey().toCharArray(fallback = true))
                        }
                    }
                }
            }
        }

        /**
         * Save the session key in the keystore
         *
         * @throws SecurityFeaturesException if saving fails
         */
        private fun saveSecretKey(sessionKey: SecretKey) {
            context.openFileOutput(KEYSTORE_FILENAME, Context.MODE_PRIVATE).use {
                try {
                    keyStore.store(it, sessionKey.toCharArray())
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
        fun getSecretKey(identity: String, sessionKey: SecretKey): CipherPayload? {
            var ctEntry: KeyStore.SecretKeyEntry?
            var ivEntry: KeyStore.SecretKeyEntry?
            var migrateKeys = false

            try {
                val protection = KeyStore.PasswordProtection(sessionKey.toCharArray())
                ctEntry = keyStore.getEntry(identity, protection) as? KeyStore.SecretKeyEntry
                ivEntry = keyStore.getEntry(identity + IV_SUFFIX, protection) as? KeyStore.SecretKeyEntry
            } catch (e: UnrecoverableKeyException) {
                val protection = KeyStore.PasswordProtection(sessionKey.toCharArray(fallback = true))
                ctEntry = keyStore.getEntry(identity, protection) as? KeyStore.SecretKeyEntry
                ivEntry = keyStore.getEntry(identity + IV_SUFFIX, protection) as? KeyStore.SecretKeyEntry
                migrateKeys = true
            }

            if (ctEntry == null || ctEntry.secretKey == null) {
                return null
            }

            return CipherPayload(ctEntry.secretKey.encoded, ivEntry?.secretKey?.encoded).apply {
                if (migrateKeys) {
                    setSecretKey(identity, this, sessionKey)
                }
            }
        }

        /**
         * Set the [SecretKey] and save it into the [KeyStore]
         *
         * @throws SecurityFeaturesException if saving fails
         */
        fun setSecretKey(identity: String, payload: CipherPayload, sessionKey: SecretKey) {
            val ctEntry = KeyStore.SecretKeyEntry(SecretKeySpec(payload.cipherText, "RAW"))
            val ivEntry = KeyStore.SecretKeyEntry(SecretKeySpec(payload.iv, "RAW"))

            with(KeyStore.PasswordProtection(sessionKey.toCharArray())) {
                keyStore.setEntry(identity, ctEntry, this)
                keyStore.setEntry(identity + IV_SUFFIX, ivEntry, this)
            }
            saveSecretKey(sessionKey)
        }

        /**
         * Convert this [SecretKey.getEncoded] to a [CharArray]
         */
        private fun SecretKey.toCharArray(fallback: Boolean = false) = if (fallback) {
            encoded.toCharArrayFallback()
        } else {
            encoded.toCharArray()
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
            private const val IV_LENGTH = 16
            private const val CIPHER_TRANSFORMATION = "AES/CBC/NoPadding"
            private const val CIPHER_KEY_ITERATIONS = 1500
            private const val CIPHER_KEY_SIZE = 16
        }

        private val random = SecureRandom.getInstance(ALGORITHM_RANDOM)
        private val factory = SecretKeyFactory.getInstance(ALGORITHM_PASSWORD_KEY)
        private val generator = KeyGenerator.getInstance(ALGORITHM_MASTER_KEY)

        init {
            generator.init(SecureRandom())
        }

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
        internal fun salt(): ByteArray {
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
        internal fun keyFromPassword(password: String): SecretKey =
                factory.generateSecret(
                        PBEKeySpec(
                                password.toCharArray(),
                                salt(),
                                CIPHER_KEY_ITERATIONS,
                                CIPHER_KEY_SIZE)
                )

        /**
         * Generate a random byte
         */
        internal fun randomBytes(size: Int = 20): ByteArray = ByteArray(size).also { random.nextBytes(it) }

        /**
         * Generate a random secret key
         */
        internal fun randomSecretKey(): SecretKey = SecretKeySpec(randomKey().encoded, ALGORITHM_MASTER_KEY)

        /**
         * Generate a random key
         */
        internal fun randomKey(): Key = generator.generateKey()

        /**
         * Generate a Initialisation Vector
         */
        internal fun generateIv(): ByteArray = randomBytes(IV_LENGTH)

        /**
         * Encrypt a plaintext using the method defined in [CIPHER_TRANSFORMATION]
         *
         * @throws SecurityFeaturesException
         */
        internal fun encrypt(text: ByteArray, key: Key): CipherPayload {
            try {
                val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
                val generatedIv = generateIv()
                cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(generatedIv))
                val iv = cipher.iv

                val cipherText = ByteArray(cipher.getOutputSize(text.size))
                cipher.update(text, 0, text.size, cipherText, 0).also {
                    cipher.doFinal(cipherText, it)
                }

                return CipherPayload(cipherText, iv)
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
            val original = payload.cipherText
            val iv = payload.iv

            try {
                val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
                if (iv != null) {
                    cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
                } else {
                    //  handle old key types
                    cipher.init(Cipher.DECRYPT_MODE, key)
                }

                val plainText = ByteArray(cipher.getOutputSize(original.size))
                cipher.update(original, 0, original.size, plainText, 0).also {
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
