package org.tiqr.authenticator.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.tiqr.Constants;
import org.tiqr.authenticator.TiqrApplication;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.general.AbstractPincodeActivity;
import org.tiqr.authenticator.general.ErrorActivity;
import org.tiqr.authenticator.security.Secret;
import org.tiqr.service.authentication.AuthenticationError;
import org.tiqr.service.authentication.AuthenticationService;

import javax.inject.Inject;

/**
 * Enter pincode and confirm.
 */
public class AuthenticationPincodeActivity extends AbstractPincodeActivity {
    protected
    @Inject
    AuthenticationService _authenticationService;

    /**
     * Create.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TiqrApplication.Companion.component().inject(this);

        // Update the text.
        title.setText(R.string.login_pin_title);
        setIntoText(R.string.login_intro);
        pintHint.setVisibility(View.GONE);
    }

    /**
     * When the ok button has been pressed, user has entered the pin
     */
    @Override
    public void process() {
        _hideSoftKeyboard(pincode);

        AuthenticationChallenge challenge = (AuthenticationChallenge)_getChallenge();
        String pin = pincode.getText().toString();
        _login(challenge, pin);
    }

    /**
     * Try to authenticate the user.
     */
    private void _login(AuthenticationChallenge challenge, String pin) {
        _showProgressDialog(getString(R.string.authenticating));

        _authenticationService.authenticate(challenge, pin, Secret.Type.PINCODE, new AuthenticationService.OnAuthenticationListener() {
            @Override
            public void onAuthenticationSuccess() {
                _cancelProgressDialog();
                AuthenticationActivityGroup group = (AuthenticationActivityGroup)getParent();
                Intent summaryIntent = new Intent(AuthenticationPincodeActivity.this, AuthenticationSummaryActivity.class);
                summaryIntent.putExtra(AuthenticationSummaryActivity.PIN, pincode.getText().toString());
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
                fallbackIntent.putExtra(Constants.AUTHENTICATION_PINCODE_KEY, pincode.getText().toString());
                fallbackIntent.putExtra(Constants.AUTHENTICATION_SECRET_TYPE, Secret.Type.PINCODE.name());
                group.startChildActivity("AuthenticationFallbackActivity", fallbackIntent);
                break;
            case INVALID_RESPONSE:
                if (!error.getExtras().containsKey("attemptsLeft") || error.getExtras().getInt("attemptsLeft") > 0) {
                    _clear();
                    _initHiddenPincodeField();
                    new ErrorActivity.ErrorBuilder()
                            .setTitle(error.getTitle())
                            .setMessage(error.getMessage())
                            .setException(error.getException())
                            .show(this);
                    break;
                }
            default:
                new ErrorActivity.ErrorBuilder()
                        .setTitle(error.getTitle())
                        .setMessage(error.getMessage())
                        .setException(error.getException())
                        .show(this);
                break;
        }
    }
}