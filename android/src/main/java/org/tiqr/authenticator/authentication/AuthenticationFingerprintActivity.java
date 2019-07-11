package org.tiqr.authenticator.authentication;

import android.content.Intent;
import android.os.Bundle;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.core.os.CancellationSignal;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.tiqr.Constants;
import org.tiqr.authenticator.TiqrApplication;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.general.AbstractActivityGroup;
import org.tiqr.authenticator.general.AbstractAuthenticationActivity;
import org.tiqr.authenticator.general.ErrorActivity;
import org.tiqr.authenticator.general.HeaderView;
import org.tiqr.authenticator.security.Secret;
import org.tiqr.service.authentication.AuthenticationError;
import org.tiqr.service.authentication.AuthenticationService;

import javax.inject.Inject;

/**
 * Enter fingerprint and confirm.
 */
public class AuthenticationFingerprintActivity extends AbstractAuthenticationActivity {

    private static final String TAG = AuthenticationFingerprintActivity.class.getSimpleName();

    protected TextView usePincodeTextView;

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
        TiqrApplication.Companion.component().inject(this);

        _fingerprintManager = FingerprintManagerCompat.from(this);


    }

    @Override
    protected void onResume() {
        setContentView(R.layout.fingerprint);
        super.onResume();

        HeaderView headerView = (HeaderView)findViewById(R.id.headerView);
        headerView.setOnLeftClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        headerView.hideRightButton();

        usePincodeTextView = findViewById(R.id.pincodeButton);
        usePincodeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AbstractActivityGroup parent = (AbstractActivityGroup)getParent();
                Intent authenticationPincodeIntent = new Intent().setClass(parent, AuthenticationPincodeActivity.class);
                parent.startChildActivity("AuthenticationPincodeActivity", authenticationPincodeIntent);
            }
        });

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

        _authenticationService.authenticate(challenge, password, Secret.Type.FINGERPRINT, new AuthenticationService.OnAuthenticationListener() {
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
            _login(challenge, Constants.INSTANCE.getAUTHENTICATION_FINGERPRINT_KEY());
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
                fallbackIntent.putExtra(Constants.INSTANCE.getAUTHENTICATION_PINCODE_KEY(), Constants.INSTANCE.getAUTHENTICATION_FINGERPRINT_KEY());
                fallbackIntent.putExtra(Constants.INSTANCE.getAUTHENTICATION_SECRET_TYPE(), Secret.Type.FINGERPRINT.name());
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