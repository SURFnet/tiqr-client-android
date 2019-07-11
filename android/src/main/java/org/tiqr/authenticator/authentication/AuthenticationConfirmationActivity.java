package org.tiqr.authenticator.authentication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import org.tiqr.authenticator.TiqrApplication;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.general.AbstractActivityGroup;
import org.tiqr.authenticator.general.AbstractConfirmationActivity;
import org.tiqr.authenticator.inject.TiqrComponent;
import org.tiqr.service.authentication.AuthenticationService;

import javax.inject.Inject;

/**
 * Confirmation dialog for authentication challenge.
 */
public class AuthenticationConfirmationActivity extends AbstractConfirmationActivity {
    protected
    @Inject
    AuthenticationService _authenticationService;

    protected
    @Inject
    SharedPreferences _sharedPreferences;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TiqrApplication.Companion.component().inject(this);

        setTitleText(R.string.authentication_confirmation_title);
        setDescriptionText(R.string.authentication_confirmation_description);
        setConfirmButtonText(R.string.authentication_confirm_button);

        // TODO: When a service provider identifier is available, switch these 2 around.
        TextView spdn = (TextView)findViewById(R.id.service_provider_display_name);
        spdn.setText(((AuthenticationChallenge)_getChallenge()).getServiceProviderDisplayName());

        TextView spi = (TextView)findViewById(R.id.service_provider_identifier);
        spi.setText("");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.tiqr.authenticator.general.AbstractConfirmationActivity#_getLayoutResource()
     */
    @Override
    protected int _getLayoutResource() {
        return R.layout.confirmation_auth;
    }

    /**
     * Confirm login.
     */
    @Override
    protected void _onDialogConfirm() {
        AbstractActivityGroup parent = (AbstractActivityGroup)getParent();
        if (parent.getChallenge().getIdentity().getUseFingerprint()) {
            Intent authenticationFingerprintIntent = new Intent().setClass(this, AuthenticationFingerprintActivity.class);
            parent.startChildActivity("AuthenticationFingerprintActivity", authenticationFingerprintIntent);
        } else {
            Intent authenticationPincodeIntent = new Intent().setClass(this, AuthenticationPincodeActivity.class);
            parent.startChildActivity("AuthenticationPincodeActivity", authenticationPincodeIntent);
        }
    }

    /**
     * Cancel dialog.
     */
    @Override
    protected void _onDialogCancel() {
        AuthenticationActivityGroup group = (AuthenticationActivityGroup)getParent();
        group.goToRoot();
    }

    /**
     * Handle finish.
     */
    @Override
    protected void _onDialogDone(boolean successful, boolean doReturn, boolean doRetry) {
        if (doReturn && _getChallenge().getReturnURL() != null) {
            AuthenticationActivityGroup group = (AuthenticationActivityGroup)getParent();
            group.goToRoot();
            _returnToChallengeUrl(successful);
        } else if (doRetry) {
            _onDialogConfirm();
        } else {
            AuthenticationActivityGroup group = (AuthenticationActivityGroup)getParent();
            group.finish(); // back to the scanner
        }
    }
}