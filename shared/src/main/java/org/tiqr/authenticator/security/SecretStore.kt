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
constructor(private val _ctx: Context) {
    private var _keyStore: KeyStore? = null
    private val _filenameKeyStore = "MobileAuthDb.kstore"
    private var _initialized = false

    init {
        _keyStore = KeyStore.getInstance("BKS")
    }

    private fun _keyStoreExists(): Boolean {
        var input: FileInputStream? = null
        // Try to read the keystore from file
        try {
            input = _ctx.openFileInput(_filenameKeyStore)
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
    private fun _createKeyStore() {
        // Load the default Key Store
        _keyStore!!.load(null, null)
    }

    private fun _sessionKeyToCharArray(sessionKey: SecretKey): CharArray {
        return Utils.byteArrayToCharArray(sessionKey.encoded)
    }

    private fun _sessionKeyToCharArrayAlternative(sessionKey: SecretKey): CharArray {
        return String(sessionKey.encoded).toCharArray()
    }


    @Throws(IOException::class, CertificateException::class, NoSuchAlgorithmException::class, KeyStoreException::class)
    private fun _saveKeyStore(sessionKey: SecretKey) {
        // Create the file
        var output: FileOutputStream? = null
        try {
            output = _ctx.openFileOutput(_filenameKeyStore, Context.MODE_PRIVATE)
            // Save the key
            _keyStore!!.store(output, _sessionKeyToCharArray(sessionKey))
        } finally {
            // Close the keystore and set the input stream
            output?.close()
        }
    }

    @Throws(UnrecoverableEntryException::class, NoSuchAlgorithmException::class, KeyStoreException::class, CertificateException::class, IOException::class)
    fun getSecretKey(identity: String, sessionKey: SecretKey): CipherPayload {
        _initializeKeyStore(sessionKey)
        var ctEntry: SecretKeyEntry?
        var ivEntry: SecretKeyEntry?

        var migrateKeys = false

        try {
            ctEntry = _keyStore!!.getEntry(identity, KeyStore.PasswordProtection(_sessionKeyToCharArray(sessionKey))) as SecretKeyEntry
            ivEntry = _keyStore!!.getEntry(identity + IV_SUFFIX, KeyStore.PasswordProtection(_sessionKeyToCharArray(sessionKey))) as SecretKeyEntry
        } catch (ex: UnrecoverableKeyException) {
            // The keystore is still using the old keys?
            ctEntry = _keyStore!!.getEntry(identity, KeyStore.PasswordProtection(_sessionKeyToCharArrayAlternative(sessionKey))) as SecretKeyEntry
            ivEntry = _keyStore!!.getEntry(identity + IV_SUFFIX, KeyStore.PasswordProtection(_sessionKeyToCharArrayAlternative(sessionKey))) as SecretKeyEntry
            // If we got this far, then yes.
            migrateKeys = true
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
        _initializeKeyStore(sessionKey)

        val cipherText = SecretKeySpec(civ.cipherText, "RAW")
        val ctEntry = KeyStore.SecretKeyEntry(
                cipherText)

        val iv = SecretKeySpec(civ.iv, "RAW")
        val ivEntry = KeyStore.SecretKeyEntry(iv)

        _keyStore!!.setEntry(identity, ctEntry,
                KeyStore.PasswordProtection(
                        _sessionKeyToCharArray(sessionKey)))
        _keyStore!!.setEntry(identity + IV_SUFFIX, ivEntry,
                KeyStore.PasswordProtection(
                        _sessionKeyToCharArray(sessionKey)))

        _saveKeyStore(sessionKey)
    }

    @Throws(CertificateException::class, NoSuchAlgorithmException::class, KeyStoreException::class, IOException::class)
    fun removeSecretKey(identity: String, sessionKey: SecretKey) {
        _initializeKeyStore(sessionKey)
        _keyStore!!.deleteEntry(identity)
        _saveKeyStore(sessionKey)
    }

    @Throws(CertificateException::class, NoSuchAlgorithmException::class, IOException::class, KeyStoreException::class)
    private fun _initializeKeyStore(sessionKey: SecretKey) {
        if (_initialized) {
            // Already initialized
            return
        }

        if (!_keyStoreExists()) {
            _createKeyStore()
            _saveKeyStore(sessionKey)
        }

        var input: FileInputStream? = null

        try {
            // Try and open the private key store
            input = _ctx.openFileInput(_filenameKeyStore)
            // Reset the keyStore
            _keyStore = KeyStore.getInstance("BKS")
            // Load the store
            try {
                _keyStore!!.load(input, _sessionKeyToCharArray(sessionKey))
            } catch (ex: IOException) {
                // It might be an old style password. In this case, the next time we save the keystore
                // it will be on using the new style password
                try {
                    input!!.close()
                } catch (ex2: Exception) { /* Unhandled */
                }

                input = _ctx.openFileInput(_filenameKeyStore)
                _keyStore = KeyStore.getInstance("BKS")
                _keyStore!!.load(input, _sessionKeyToCharArrayAlternative(sessionKey))
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Opened keystore with alternative password.")
                }
            }

            _initialized = true
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
