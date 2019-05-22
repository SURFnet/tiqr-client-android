package org.tiqr.authenticator.security;

import android.content.Context;
import android.util.Log;

import org.tiqr.BuildConfig;
import org.tiqr.Utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import biz.source_code.base64Coder.Base64Coder;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class SecretStore {
    private KeyStore _keyStore;
    private String _filenameKeyStore = "MobileAuthDb.kstore";
    private Context _ctx;
    private boolean _initialized = false;
    private final static String IV_SUFFIX = "-org.tiqr.iv";

    private static final String TAG = SecretStore.class.getName();

    public SecretStore(Context ctx) throws KeyStoreException {
        _ctx = ctx;
        _keyStore = KeyStore.getInstance("BKS");
    }

    private boolean _keyStoreExists() {
        FileInputStream input = null;
        // Try to read the keystore from file
        try {
            input = _ctx.openFileInput(_filenameKeyStore);
            return true;
        } catch (FileNotFoundException ex) {
            return false;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Empty catch.
                }
            }
        }

    }

    private void _createKeyStore() throws CertificateException, NoSuchAlgorithmException, IOException {
        // Load the default Key Store
        _keyStore.load(null, null);
    }

    private char[] _sessionKeyToCharArray(SecretKey sessionKey) {
        return Utils.byteArrayToCharArray(sessionKey.getEncoded());
    }

    private char[] _sessionKeyToCharArrayAlternative(SecretKey sessionKey) {
        return new String(sessionKey.getEncoded()).toCharArray();
    }


    private void _saveKeyStore(SecretKey sessionKey) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        // Create the file
        FileOutputStream output = null;
        try {
            output = _ctx.openFileOutput(_filenameKeyStore, Context.MODE_PRIVATE);
            // Save the key
            _keyStore.store(output, _sessionKeyToCharArray(sessionKey));
        } finally {
            // Close the keystore and set the input stream
            if (output != null) {
                output.close();
            }
        }
    }

    public CipherPayload getSecretKey(String identity, SecretKey sessionKey) throws UnrecoverableEntryException,
            NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        _initializeKeyStore(sessionKey);
        SecretKeyEntry ctEntry;
        SecretKeyEntry ivEntry;

        boolean migrateKeys = false;

        try {
            ctEntry = (SecretKeyEntry) _keyStore.getEntry(identity, new KeyStore.PasswordProtection(_sessionKeyToCharArray(sessionKey)));
             ivEntry = (SecretKeyEntry) _keyStore.getEntry(identity + IV_SUFFIX, new KeyStore.PasswordProtection(_sessionKeyToCharArray(sessionKey)));
        } catch (UnrecoverableKeyException ex) {
            // The keystore is still using the old keys?
            ctEntry = (SecretKeyEntry) _keyStore.getEntry(identity, new KeyStore.PasswordProtection(_sessionKeyToCharArrayAlternative(sessionKey)));
            ivEntry = (SecretKeyEntry) _keyStore.getEntry(identity + IV_SUFFIX, new KeyStore.PasswordProtection(_sessionKeyToCharArrayAlternative(sessionKey)));
            // If we got this far, then yes.
            migrateKeys = true;
        }
        byte[] ivBytes;
        // For old keys, we don't store the IV:
        if (ivEntry == null || ivEntry.getSecretKey() == null) {
            ivBytes = null;
            Log.i("encryption", "No IV found for: " + identity);
        } else {
            ivBytes = ivEntry.getSecretKey().getEncoded();
            Log.i("encryption", "IV for: " + identity + " is " + new String(Base64Coder.encode(ivBytes)));
        }
        if (ctEntry == null || ctEntry.getSecretKey() == null) {
            throw new UnrecoverableKeyException("Cipher entry has not been found!");
        }
        CipherPayload cipherPayload = new CipherPayload(ctEntry.getSecretKey().getEncoded(), ivBytes);
        if (migrateKeys) {
            setSecretKey(identity, cipherPayload, sessionKey);
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Migration of keys and store has finished.");
            }
        }
        return cipherPayload;
    }

    public void setSecretKey(String identity, CipherPayload civ, SecretKey sessionKey) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        _initializeKeyStore(sessionKey);

        SecretKeySpec cipherText = new SecretKeySpec(civ.cipherText, "RAW");
        KeyStore.SecretKeyEntry ctEntry = new KeyStore.SecretKeyEntry(
                cipherText);

        SecretKeySpec iv = new SecretKeySpec(civ.iv, "RAW");
        KeyStore.SecretKeyEntry ivEntry = new KeyStore.SecretKeyEntry(iv);

        _keyStore.setEntry(identity, ctEntry,
                new KeyStore.PasswordProtection(
                        _sessionKeyToCharArray(sessionKey)));
        _keyStore.setEntry(identity + IV_SUFFIX, ivEntry,
                new KeyStore.PasswordProtection(
                        _sessionKeyToCharArray(sessionKey)));

        _saveKeyStore(sessionKey);
    }

    public void removeSecretKey(String identity, SecretKey sessionKey) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        _initializeKeyStore(sessionKey);
        _keyStore.deleteEntry(identity);
        _saveKeyStore(sessionKey);
    }

    private void _initializeKeyStore(SecretKey sessionKey) throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        if (_initialized) {
            // Already initialized
            return;
        }

        if (!_keyStoreExists()) {
            _createKeyStore();
            _saveKeyStore(sessionKey);
        }

        FileInputStream input = null;

        try {
            // Try and open the private key store
            input = _ctx.openFileInput(_filenameKeyStore);
            // Reset the keyStore
            _keyStore = KeyStore.getInstance("BKS");
            // Load the store
            try {
                _keyStore.load(input, _sessionKeyToCharArray(sessionKey));
            } catch (IOException ex) {
                // It might be an old style password. In this case, the next time we save the keystore
                // it will be on using the new style password
                try { input.close(); } catch (Exception ex2) { /* Unhandled */ }
                input = _ctx.openFileInput(_filenameKeyStore);
                _keyStore = KeyStore.getInstance("BKS");
                _keyStore.load(input, _sessionKeyToCharArrayAlternative(sessionKey));
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Opened keystore with alternative password.");
                }
            }
            _initialized = true;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Empty catch block
                }
            }
        }
    }
}
