package org.tiqr.authenticator.security

import android.content.Context
import android.util.Log

import org.tiqr.authenticator.datamodel.Identity
import org.tiqr.authenticator.exceptions.SecurityFeaturesException

import java.io.IOException
import java.security.InvalidKeyException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableEntryException
import java.security.cert.CertificateException

import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class Secret @Throws(KeyStoreException::class)
private constructor(val identity: Identity, val context: Context) {

    private var secret: SecretKey? = null
    private val store = SecretStore(context)

    enum class Type {
        PINCODE,
        FINGERPRINT
    }

    @Throws(InvalidKeyException::class, SecurityFeaturesException::class, CertificateException::class, UnrecoverableEntryException::class, NoSuchAlgorithmException::class, KeyStoreException::class, IOException::class)
    fun getSecret(sessionKey: SecretKey, type: Type): SecretKey? {
        if (secret == null) {
            loadFromKeyStore(sessionKey, type)
        }
        return secret
    }

    fun setSecret(secret: SecretKey) {
        this.secret = secret
    }

    private fun getId(type: Type): String {
        return if (type == Type.PINCODE) {
            "${identity.id}"
        } else {
            "${identity.id}$FINGERPRINT_SUFFIX"
        }
    }

    @Throws(SecurityFeaturesException::class, InvalidKeyException::class, CertificateException::class, NoSuchAlgorithmException::class, KeyStoreException::class, IOException::class, UnrecoverableEntryException::class)
    private fun loadFromKeyStore(sessionKey: SecretKey, type: Type): SecretKey? {
        val deviceKey = Encryption.getDeviceKey(context)
        val civ = store.getSecretKey(getId(type), deviceKey)

        secret = SecretKeySpec(Encryption.decrypt(civ, sessionKey), "RAW")

        if (civ.iv == null) {
            // Old keys didn't store the iv, so upgrade it to a new key.
            Log.i("encryption", "Found old style key; upgrading to new key.")
            storeInKeyStore(sessionKey, type)
        }
        return secret
    }

    @Throws(SecurityFeaturesException::class, CertificateException::class, NoSuchAlgorithmException::class, KeyStoreException::class, IOException::class)
    fun storeInKeyStore(sessionKey: SecretKey, type: Type) {
        val civ = Encryption.encrypt(secret!!.encoded, sessionKey)
        store.setSecretKey(getId(type), civ, Encryption.getDeviceKey(context))
    }

    companion object {

        private val FINGERPRINT_SUFFIX = "org.tiqr.FP"

        @Throws(KeyStoreException::class)
        fun secretForIdentity(identity: Identity, context: Context): Secret {
            return Secret(identity, context)
        }
    }
}
