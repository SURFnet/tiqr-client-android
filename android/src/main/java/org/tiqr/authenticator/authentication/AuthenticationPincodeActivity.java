package org.tiqr.authenticator.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.tiqr.authenticator.Application;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.general.AbstractActivityGroup;
import org.tiqr.authenticator.general.AbstractPincodeActivity;
import org.tiqr.authenticator.general.ErrorActivity;
import org.tiqr.service.authentication.AuthenticationError;
import org.tiqr.service.authentication.AuthenticationService;

import javax.inject.Inject;

/**
 * Enter pincode and confirm.
 */
public class AuthenticationPincodeActivity extends AbstractPincodeActivity {
    protected @Inject AuthenticationService _authenticationService;

    /**
     * Create.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application)getApplication()).inject(this);
    }

    /**
     * When the ok button has been pressed, user has entered the pin
     */
    @Override
    public void process(final View v) {
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

        _authenticationService.authenticate(challenge, pin, new AuthenticationService.OnAuthenticationListener() {
            @Override
            public void onAuthenticationSuccess() {
                progressDialog.cancel();
                AuthenticationActivityGroup group = (AuthenticationActivityGroup)getParent();
                Intent summaryIntent = new Intent(AuthenticationPincodeActivity.this, AuthenticationSummaryActivity.class);
                group.startChildActivity("AuthenticationSummaryActivity", summaryIntent);
            }

            @Override
            public void onAuthenticationError(AuthenticationError error) {
                progressDialog.cancel();
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
                AuthenticationActivityGroup group = (AuthenticationActivityGroup) getParent();
                Intent fallbackIntent = new Intent(this, AuthenticationFallbackActivity.class);
                fallbackIntent.putExtra("org.tiqr.authentication.pincode", pincode.getText().toString());
                group.startChildActivity("AuthenticationFallbackActivity", fallbackIntent);
                break;
            case INVALID_RESPONSE:
                if (!error.getExtras().containsKey("attemptsLeft") || error.getExtras().getInt("attemptsLeft") > 0) {
                    _clear();
                    _showErrorView(error.getTitle(), error.getMessage());
                    _initHiddenPincodeField();
                    break;
                }
            default:
                AbstractActivityGroup parent = (AbstractActivityGroup) getParent();
                Intent intent = new Intent().setClass(this, ErrorActivity.class);
                intent.putExtra("org.tiqr.error.title", error.getTitle());
                intent.putExtra("org.tiqr.error.message", error.getMessage());
                if (error.getExtras().containsKey("attemptsLeft")) {
                    intent.putExtra("org.tiqr.error.attemptsLeft", error.getExtras().getInt("attemptsLeft"));
                }
                parent.startChildActivity("ErrorActivity", intent);
                break;
        }
    }
}