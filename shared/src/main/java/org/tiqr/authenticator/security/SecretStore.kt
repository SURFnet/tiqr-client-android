package org.tiqr.authenticator.security

import android.content.Context
import android.util.Base64
import android.util.Log

import org.tiqr.BuildConfig
import org.tiqr.Utils

import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyStore
import java.security.KeyStore.SecretKeyEntry
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableEntryException
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class SecretStore @Throws(KeyStoreException::class)
constructor(private val ctx: Context) {

    private var keyStore: KeyStore = KeyStore.getInstance("BKS")
    private val filenameKeyStore = "MobileAuthDb.kstore"
    private var initialized = false

    private fun keyStoreExists(): Boolean {
        var input: FileInputStream? = null
        // Try to read the keystore from file
        try {
            input = ctx.openFileInput(filenameKeyStore)
            return true
        } catch (ex: FileNotFoundException) {
            return false
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    // Empty catch.
                }
            }
        }

    }

    @Throws(CertificateException::class, NoSuchAlgorithmException::class, IOException::class)
    private fun createKeyStore() {
        // Load the default Key Store
        keyStore.load(null, null)
    }

    private fun sessionKeyToCharArray(sessionKey: SecretKey): CharArray {
        return Utils.byteArrayToCharArray(sessionKey.encoded)
    }

    private fun sessionKeyToCharArrayAlternative(sessionKey: SecretKey): CharArray {
        return String(sessionKey.encoded).toCharArray()
    }

    /**
     * This method is just an upgrade path for 3.0.6 users which had a faulty char array conversion
     * algorithm, which resulted in an incorrect password for 50% of the users.
     * We keep the old code to enable the upgrade path, so they don't have to go through
     * enrollment again.
     */
    private fun sessionKeyToCharArrayAlternative306Version(sessionKey: SecretKey): CharArray {
        @Suppress("DEPRECATION")
        return Utils.byteArrayToCharArray306Wrong(sessionKey.encoded)
    }


    @Throws(IOException::class, CertificateException::class, NoSuchAlgorithmException::class, KeyStoreException::class)
    private fun saveKeyStore(sessionKey: SecretKey) {
        // Create the file
        var output: FileOutputStream? = null
        try {
            output = ctx.openFileOutput(filenameKeyStore, Context.MODE_PRIVATE)
            // Save the key
            keyStore.store(output, sessionKeyToCharArray(sessionKey))
        } finally {
            // Close the keystore and set the input stream
            output?.close()
        }
    }

    @Throws(UnrecoverableEntryException::class, NoSuchAlgorithmException::class, KeyStoreException::class, CertificateException::class, IOException::class)
    fun getSecretKey(identity: String, sessionKey: SecretKey): CipherPayload {
        initializeKeyStore(sessionKey)
        var ctEntry: SecretKeyEntry?
        var ivEntry: SecretKeyEntry?

        var migrateKeys = false

        try {
            ctEntry = keyStore.getEntry(identity, KeyStore.PasswordProtection(sessionKeyToCharArray(sessionKey))) as SecretKeyEntry
            ivEntry = keyStore.getEntry(identity + IV_SUFFIX, KeyStore.PasswordProtection(sessionKeyToCharArray(sessionKey))) as SecretKeyEntry
        } catch (ex: UnrecoverableKeyException) {
            try {
                // The keystore is still using the old keys?
                ctEntry = keyStore.getEntry(identity, KeyStore.PasswordProtection(sessionKeyToCharArrayAlternative(sessionKey))) as SecretKeyEntry
                ivEntry = keyStore.getEntry(identity + IV_SUFFIX, KeyStore.PasswordProtection(sessionKeyToCharArrayAlternative(sessionKey))) as SecretKeyEntry
                // If we got this far, then yes.
                migrateKeys = true
            } catch (ex: UnrecoverableKeyException) {
                // The keystore might be  created in version 3.0.6, which had a wrong char array conversion algorithm
                ctEntry = keyStore.getEntry(identity, KeyStore.PasswordProtection(sessionKeyToCharArrayAlternative306Version(sessionKey))) as SecretKeyEntry
                ivEntry = keyStore.getEntry(identity + IV_SUFFIX, KeyStore.PasswordProtection(sessionKeyToCharArrayAlternative306Version(sessionKey))) as SecretKeyEntry
                // If we got this far, then yes.
                migrateKeys = true
            }
        }

        val ivBytes: ByteArray?
        // For old keys, we don't store the IV:
        if (ivEntry == null || ivEntry.secretKey == null) {
            ivBytes = null
            Log.i("encryption", "No IV found for: $identity")
        } else {
            ivBytes = ivEntry.secretKey.encoded
            Log.i("encryption", "IV for: " + identity + " is " + String(Base64.encode(ivBytes, Base64.DEFAULT)))
        }
        if (ctEntry == null || ctEntry.secretKey == null) {
            throw UnrecoverableKeyException("Cipher entry has not been found!")
        }
        val cipherPayload = CipherPayload(ctEntry.secretKey.encoded, ivBytes!!)
        if (migrateKeys) {
            setSecretKey(identity, cipherPayload, sessionKey)
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Migration of keys and store has finished.")
            }
        }
        return cipherPayload
    }

    @Throws(CertificateException::class, NoSuchAlgorithmException::class, KeyStoreException::class, IOException::class)
    fun setSecretKey(identity: String, civ: CipherPayload, sessionKey: SecretKey) {
        initializeKeyStore(sessionKey)

        val cipherText = SecretKeySpec(civ.cipherText, "RAW")
        val ctEntry = KeyStore.SecretKeyEntry(
                cipherText)

        val iv = SecretKeySpec(civ.iv, "RAW")
        val ivEntry = KeyStore.SecretKeyEntry(iv)

        keyStore.setEntry(identity, ctEntry,
                KeyStore.PasswordProtection(
                        sessionKeyToCharArray(sessionKey)))
        keyStore.setEntry(identity + IV_SUFFIX, ivEntry,
                KeyStore.PasswordProtection(
                        sessionKeyToCharArray(sessionKey)))

        saveKeyStore(sessionKey)
    }

    @Throws(CertificateException::class, NoSuchAlgorithmException::class, KeyStoreException::class, IOException::class)
    fun removeSecretKey(identity: String, sessionKey: SecretKey) {
        initializeKeyStore(sessionKey)
        keyStore.deleteEntry(identity)
        saveKeyStore(sessionKey)
    }

    @Throws(CertificateException::class, NoSuchAlgorithmException::class, IOException::class, KeyStoreException::class)
    private fun initializeKeyStore(sessionKey: SecretKey) {
        if (initialized) {
            // Already initialized
            return
        }

        if (!keyStoreExists()) {
            createKeyStore()
            saveKeyStore(sessionKey)
        }

        var input: FileInputStream? = null

        try {
            // Try and open the private key store
            input = ctx.openFileInput(filenameKeyStore)
            // Reset the keyStore
            keyStore = KeyStore.getInstance("BKS")
            // Load the store
            try {
                keyStore.load(input, sessionKeyToCharArray(sessionKey))
            } catch (ex: IOException) {
                // It might be an old style password. In this case, the next time we save the keystore
                // it will be on using the new style password
                try {
                    input!!.close()
                } catch (ex2: Exception) { /* Unhandled */
                }

                try {
                    input = ctx.openFileInput(filenameKeyStore)
                    keyStore = KeyStore.getInstance("BKS")
                    keyStore.load(input, sessionKeyToCharArrayAlternative(sessionKey))
                } catch (ex3: IOException) {
                    // Last try: keystore was created with 3.0.6 version, which had a wrong char array
                    // conversion algorithm
                    try {
                        input!!.close()
                    } catch (ex2: Exception) { /* Unhandled */
                    }
                    input = ctx.openFileInput(filenameKeyStore)
                    keyStore = KeyStore.getInstance("BKS")
                    keyStore.load(input, sessionKeyToCharArrayAlternative306Version(sessionKey))
                }
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Opened keystore with alternative password.")
                }

            }

            initialized = true
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    // Empty catch block
                }
            }
        }
    }

    companion object {
        private val IV_SUFFIX = "-org.tiqr.iv"

        private val TAG = SecretStore::class.java.name
    }
}
