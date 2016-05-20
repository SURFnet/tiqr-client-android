package org.tiqr.authenticator.security;

import android.content.Context;
import android.util.Log;

import org.tiqr.authenticator.datamodel.Identity;
import org.tiqr.authenticator.exceptions.SecurityFeaturesException;

import java.security.InvalidKeyException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Secret {
    private Identity _identity = null;
    private SecretKey _secret = null;
    private SecretStore _store = null;
    private Context _ctx = null;

    public static Secret secretForIdentity(Identity identity, Context context) {
        Secret s = new Secret(identity, context);
        return s;
    }

    private Secret(Identity identity, Context context) {
        _identity = identity;
        _store = new SecretStore(context);
        _ctx = context;
    }

    public SecretKey getSecret(SecretKey sessionKey) throws InvalidKeyException, SecurityFeaturesException {
        if (_secret == null) {
            _loadFromKeyStore(sessionKey);
        }
        return _secret;
    }

    public void setSecret(SecretKey secret) {
        _secret = secret;
    }

    private SecretKey _loadFromKeyStore(SecretKey sessionKey) throws SecurityFeaturesException, InvalidKeyException {
        String id = Long.toString(_identity.getId());
        SecretKey deviceKey = Encryption.getDeviceKey(_ctx);
        CipherPayload civ = _store.getSecretKey(id, deviceKey);
        if (civ.cipherText == null) {
            throw new InvalidKeyException("Requested key not found.");
        }
        _secret = new SecretKeySpec(Encryption.decrypt(civ, sessionKey), "RAW");
        if (civ.iv == null) {
            // Old keys didn't store the iv, so upgrade it to a new key.
            Log.i("encryption", "Found old style key; upgrading to new key.");
            storeInKeyStore(sessionKey);
        }
        return _secret;
    }

    public void storeInKeyStore(SecretKey sessionKey) throws SecurityFeaturesException {
        CipherPayload civ = Encryption.encrypt(_secret.getEncoded(), sessionKey);
        _store.setSecretKey(Long.toString(_identity.getId()), civ, Encryption.getDeviceKey(_ctx));
    }
}
