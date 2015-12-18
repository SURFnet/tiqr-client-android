package org.tiqr.authenticator.authentication;

import android.app.Activity;
import android.os.Bundle;

import org.tiqr.authenticator.Application;
import org.tiqr.authenticator.R;
import org.tiqr.service.authentication.AuthenticationService;

import javax.inject.Inject;

/**
 * Enter fingerprint and confirm.
 */
public class AuthenticationFingerprintActivity extends Activity {

    private static final String DIALOG_FRAGMENT_TAG = "fpDialogFragment";

    protected
    @Inject
    AuthenticationService _authenticationService;

//    protected
//    @Inject
//    FingerprintAuthenticationDialogFragment _fingerprintDialogFragment;

//    protected
//    @Inject
//    KeyStore _keyStore;

    /**
     * Create.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application)getApplication()).inject(this);
        setContentView(R.layout.fingerprint);

        // Set up the crypto object for later. The object will be authenticated by use
        // of the fingerprint.
//        if (_initCipher()) {
//            // Show the fingerprint dialog. The user has the option to use the fingerprint with
//            // crypto, or you can fall back to using a server-side verified password.
//            _fingerprintDialogFragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
//            _fingerprintDialogFragment.setStage(FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
//            _fingerprintDialogFragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
//        }

    }

    private boolean _initCipher() {
        //TODO
        return true;
    }


}