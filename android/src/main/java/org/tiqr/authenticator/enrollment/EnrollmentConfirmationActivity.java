package org.tiqr.authenticator.enrollment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import org.tiqr.authenticator.Application;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.auth.EnrollmentChallenge;
import org.tiqr.authenticator.general.AbstractActivityGroup;
import org.tiqr.authenticator.general.AbstractConfirmationActivity;
import org.tiqr.service.authentication.AuthenticationService;

import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;

/**
 * Dialog for confirming the enrollment for a certain identity / identity_provider.
 */
public class EnrollmentConfirmationActivity extends AbstractConfirmationActivity {

    protected @Inject
    AuthenticationService _authenticationService;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application)getApplication()).inject(this);

        setTitleText(R.string.enrollment_confirmation_title);
        setDescriptionText(R.string.enrollment_confirmation_description);
        setConfirmButtonText(R.string.enrollment_confirm_button);
        setCancelButtonText(R.string.enrollment_cancel_button);

        TextView enrollmentURLDomain = (TextView)findViewById(R.id.enrollment_url_domain);
        try {
            URL enrollmentURL = new URL(((EnrollmentChallenge)_getChallenge()).getEnrollmentURL());
            enrollmentURLDomain.setText(enrollmentURL.getHost());
        } catch (MalformedURLException e) {
            // Nothing
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.tiqr.authenticator.general.AbstractConfirmationActivity#_getLayoutResource()
     */
    @Override
    protected int _getLayoutResource() {
        return R.layout.confirmation_enroll;
    }

    /**
     * Enroll on confirmation.
     */
    @Override
    protected void _onDialogConfirm() {
        _authenticationService.resetFingerPrintAsAuthentication();

        AbstractActivityGroup parent = (AbstractActivityGroup)getParent();
        Intent enrollmentPincodeIntent = new Intent().setClass(this, EnrollmentPincodeActivity.class);
        parent.startChildActivity("EnrollmentPincodeActivity", enrollmentPincodeIntent);

    }

    /**
     * Handle cancel.
     */
    @Override
    protected void _onDialogCancel() {
        EnrollmentActivityGroup group = (EnrollmentActivityGroup)getParent();
        group.goToRoot();
    }

    /**
     * Handle finish.
     */
    @Override
    protected void _onDialogDone(boolean successful, boolean doReturn, boolean doRetry) {
        if (doReturn && _getChallenge().getReturnURL() != null) {
            _returnToChallengeUrl(successful);
        } else {
            EnrollmentActivityGroup group = (EnrollmentActivityGroup)getParent();
            group.finish(); // back to the scanner
        }
    }
}