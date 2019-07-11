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
private constructor(identity: Identity, context: Context) {

    private var _identity: Identity? = null
    private var _secret: SecretKey? = null
    private var _store: SecretStore? = null
    private var _ctx: Context? = null

    enum class Type {
        PINCODE,
        FINGERPRINT
    }

    init {
        _identity = identity
        _store = SecretStore(context)
        _ctx = context
    }

    @Throws(InvalidKeyException::class, SecurityFeaturesException::class, CertificateException::class, UnrecoverableEntryException::class, NoSuchAlgorithmException::class, KeyStoreException::class, IOException::class)
    fun getSecret(sessionKey: SecretKey, type: Type): SecretKey? {
        if (_secret == null) {
            _loadFromKeyStore(sessionKey, type)
        }
        return _secret
    }

    fun setSecret(secret: SecretKey) {
        _secret = secret
    }

    private fun getId(type: Type): String {
        return if (type == Type.PINCODE) {
            java.lang.Long.toString(_identity!!.id)
        } else {
            java.lang.Long.toString(_identity!!.id) + FINGERPRINT_SUFFIX
        }
    }

    @Throws(SecurityFeaturesException::class, InvalidKeyException::class, CertificateException::class, NoSuchAlgorithmException::class, KeyStoreException::class, IOException::class, UnrecoverableEntryException::class)
    private fun _loadFromKeyStore(sessionKey: SecretKey, type: Type): SecretKey? {
        val deviceKey = Encryption.getDeviceKey(_ctx!!)
        val civ = _store!!.getSecretKey(getId(type), deviceKey)

        _secret = SecretKeySpec(Encryption.decrypt(civ, sessionKey), "RAW")

        // Old keys didn't store the iv, so upgrade it to a new key.
        Log.i("encryption", "Found old style key; upgrading to new key.")
        storeInKeyStore(sessionKey, type)

        return _secret
    }

    @Throws(SecurityFeaturesException::class, CertificateException::class, NoSuchAlgorithmException::class, KeyStoreException::class, IOException::class)
    fun storeInKeyStore(sessionKey: SecretKey, type: Type) {
        val civ = Encryption.encrypt(_secret!!.encoded, sessionKey)
        _store!!.setSecretKey(getId(type), civ, Encryption.getDeviceKey(_ctx!!))
    }

    companion object {

        private val FINGERPRINT_SUFFIX = "org.tiqr.FP"

        @Throws(KeyStoreException::class)
        fun secretForIdentity(identity: Identity, context: Context): Secret {
            return Secret(identity, context)
        }
    }
}
