package org.tiqr.authenticator.security;

import android.content.Context;
import android.util.Log;

import org.tiqr.authenticator.datamodel.Identity;
import org.tiqr.authenticator.exceptions.SecurityFeaturesException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Secret {

    public enum Type {
        PINCODE,
        FINGERPRINT
    }

    private final static String FINGERPRINT_SUFFIX = "org.tiqr.FP";

    private Identity _identity = null;
    private SecretKey _secret = null;
    private SecretStore _store = null;
    private Context _ctx = null;

    public static Secret secretForIdentity(Identity identity, Context context) throws KeyStoreException {
        return new Secret(identity, context);
    }

    private Secret(Identity identity, Context context) throws KeyStoreException {
        _identity = identity;
        _store = new SecretStore(context);
        _ctx = context;
    }

    public SecretKey getSecret(SecretKey sessionKey, Type type) throws InvalidKeyException, SecurityFeaturesException, CertificateException, UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException, IOException {
        if (_secret == null) {
            _loadFromKeyStore(sessionKey, type);
        }
        return _secret;
    }

    public void setSecret(SecretKey secret) {
        _secret = secret;
    }

    private String getId(Type type) {
        if (type == Type.PINCODE) {
            return Long.toString(_identity.getId());
        } else {
            return Long.toString(_identity.getId()) + FINGERPRINT_SUFFIX;
        }
    }

    private SecretKey _loadFromKeyStore(SecretKey sessionKey, Type type) throws SecurityFeaturesException, InvalidKeyException, CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableEntryException {
        SecretKey deviceKey = Encryption.getDeviceKey(_ctx);
        CipherPayload civ = _store.getSecretKey(getId(type), deviceKey);
        if (civ == null || civ.cipherText == null) {
            throw new InvalidKeyException("Requested key not found.");
        }
        _secret = new SecretKeySpec(Encryption.decrypt(civ, sessionKey), "RAW");
        if (civ.iv == null) {
            // Old keys didn't store the iv, so upgrade it to a new key.
            Log.i("encryption", "Found old style key; upgrading to new key.");
            storeInKeyStore(sessionKey, type);
        }
        return _secret;
    }

    public void storeInKeyStore(SecretKey sessionKey, Type type) throws SecurityFeaturesException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        CipherPayload civ = Encryption.encrypt(_secret.getEncoded(), sessionKey);
        _store.setSecretKey(getId(type), civ, Encryption.getDeviceKey(_ctx));
    }
}
