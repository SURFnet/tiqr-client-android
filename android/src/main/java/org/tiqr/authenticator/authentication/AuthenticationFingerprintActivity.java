package org.tiqr.authenticator.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.util.Log;

import org.tiqr.Constants;
import org.tiqr.authenticator.Application;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.general.AbstractAuthenticationActivity;
import org.tiqr.authenticator.general.ErrorActivity;
import org.tiqr.service.authentication.AuthenticationError;
import org.tiqr.service.authentication.AuthenticationService;

import javax.inject.Inject;

/**
 * Enter fingerprint and confirm.
 */
public class AuthenticationFingerprintActivity extends AbstractAuthenticationActivity {

    private static final String TAG = AuthenticationFingerprintActivity.class.getSimpleName();

    protected
    @Inject
    AuthenticationService _authenticationService;

    private FingerprintManagerCompat _fingerprintManager;

    private AuthenticationCallback _authenticationCallback = new AuthenticationCallback();
    private CancellationSignal _cancellationSignal = null;

    /**
     * Create.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application)getApplication()).inject(this);

        _fingerprintManager = FingerprintManagerCompat.from(this);
    }

    @Override
    protected void onResume() {
        setContentView(R.layout.fingerprint);
        super.onResume();

        _login();
    }

    @Override
    protected void onPause() {
        if (_cancellationSignal != null) {
            _cancellationSignal.cancel();
        }
        super.onPause();
    }

    private void _login() {
        _cancellationSignal = new CancellationSignal();
        _fingerprintManager.authenticate(null, 0, _cancellationSignal, _authenticationCallback, null);
    }

    /**
     * Try to authenticate the user.
     */
    private void _login(AuthenticationChallenge challenge, String password) {
        _showProgressDialog(getString(R.string.authenticating));

        _authenticationService.authenticate(challenge, password, new AuthenticationService.OnAuthenticationListener() {
            @Override
            public void onAuthenticationSuccess() {
                _cancelProgressDialog();
                AuthenticationActivityGroup group = (AuthenticationActivityGroup)getParent();
                Intent summaryIntent = new Intent(AuthenticationFingerprintActivity.this, AuthenticationSummaryActivity.class);
                group.startChildActivity("AuthenticationSummaryActivity", summaryIntent);
            }

            @Override
            public void onAuthenticationError(AuthenticationError error) {
                _cancelProgressDialog();
                _processError(error);
            }
        });
    }

    /**
     * Fingerprint Authentication callback
     */
    private class AuthenticationCallback extends FingerprintManagerCompat.AuthenticationCallback {
        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
            AuthenticationChallenge challenge = (AuthenticationChallenge)_getChallenge();
            _login(challenge, Constants.AUTHENTICATION_FINGERPRINT_KEY);
        }

        @Override
        public void onAuthenticationFailed() {
            // No user action required
            Log.w(TAG, "Fingerprint authentication failed");
        }
    }

    /**
     * Process error.
     *
     * @param error Error details.
     */
    protected void _processError(AuthenticationError error) {
        switch (error.getType()) {
            case CONNECTION:
            case UNKNOWN:
                finish(); // Clear current from the stack so back goes back one deeper.
                AuthenticationActivityGroup group = (AuthenticationActivityGroup)getParent();
                Intent fallbackIntent = new Intent(this, AuthenticationFallbackActivity.class);
                fallbackIntent.putExtra(Constants.AUTHENTICATION_PINCODE_KEY, Constants.AUTHENTICATION_FINGERPRINT_KEY);
                group.startChildActivity("AuthenticationFallbackActivity", fallbackIntent);
                break;
            case INVALID_RESPONSE:
                if (!error.getExtras().containsKey("attemptsLeft") || error.getExtras().getInt("attemptsLeft") > 0) {
                    new ErrorActivity.ErrorBuilder()
                            .setTitle(error.getTitle())
                            .setMessage(error.getMessage())
                            .show(this);
                    break;
                }
            default:
                new ErrorActivity.ErrorBuilder()
                        .setTitle(error.getTitle())
                        .setMessage(error.getMessage())
                        .show(this);
                break;
        }
    }

}