package org.tiqr.authenticator.enrollment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import org.tiqr.authenticator.Application;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.auth.EnrollmentChallenge;
import org.tiqr.authenticator.general.AbstractActivityGroup;
import org.tiqr.authenticator.general.AbstractPincodeActivity;
import org.tiqr.authenticator.general.ErrorActivity;
import org.tiqr.service.enrollment.EnrollmentError;
import org.tiqr.service.enrollment.EnrollmentService;

import javax.inject.Inject;

/**
 * Verify enrollment PIN and start enrollment process.
 */
public class EnrollmentPincodeVerificationActivity extends AbstractPincodeActivity {
    protected
    @Inject
    EnrollmentService _enrollmentService;
    protected String firstPin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application)getApplication()).inject(this);

        firstPin = getIntent().getStringExtra("org.tiqr.firstPin");

        // Update the text.
        title.setText(R.string.entroll_pin_verify_title);
        setIntoText(R.string.login_verify_intro);
    }

    @Override
    public void process(View v) {
        String secondPin = pincode.getText().toString();
        if (!firstPin.equals(secondPin)) {
            _clear();
            new ErrorActivity.ErrorBuilder()
                    .setTitle(getString(R.string.passwords_dont_match_title))
                    .setMessage(getString(R.string.passwords_dont_match))
                    .show(this);
            return;
        }

        _hideSoftKeyboard(pincode);

        EnrollmentChallenge challenge = (EnrollmentChallenge)_getChallenge();
        _enroll(challenge, secondPin);
    }

    /**
     * Enroll user
     *
     * We run this in a new thread here because otherwise, the activity dialog wouldn't show
     *
     * @param challenge Challenge.
     * @param pin       PIN code.
     */
    private void _enroll(EnrollmentChallenge challenge, String pin) {
        _showProgressDialog(getString(R.string.enrolling));

        _enrollmentService.enroll(challenge, pin, new EnrollmentService.OnEnrollmentListener() {
            @Override
            public void onEnrollmentSuccess() {
                progressDialog.cancel();
                EnrollmentActivityGroup group = (EnrollmentActivityGroup)getParent();
                Intent summaryIntent = new Intent(EnrollmentPincodeVerificationActivity.this, EnrollmentSummaryActivity.class);
                group.startChildActivity("EnrollmentSummaryActivity", summaryIntent);
            }

            @Override
            public void onEnrollmentError(EnrollmentError error) {
                progressDialog.cancel();
                new ErrorActivity.ErrorBuilder()
                        .setTitle(error.getTitle())
                        .setMessage(error.getMessage())
                        .show(EnrollmentPincodeVerificationActivity.this);
            }
        });
    }

    /**
     * Handle finish.
     */
    protected void _onDialogDone(boolean successful, boolean doReturn, boolean doRetry) {
        if (doReturn && _getChallenge().getReturnURL() != null) {
            _returnToChallengeUrl(successful);
        } else {
            EnrollmentActivityGroup group = (EnrollmentActivityGroup)getParent();
            group.finish(); // back to the scanner
        }
    }

    /**
     * Returns to the challenge return URL.
     *
     * @param successful successful?
     */
    protected void _returnToChallengeUrl(boolean successful) {
        String url = _getChallenge().getReturnURL();

        if (url.indexOf("?") >= 0) {
            url = url + "&succesful=" + successful;
        } else {
            url = url + "?succesful=" + successful;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception ex) {
            _onDialogCancel();
        }
    }

    /**
     * Handle cancel.
     */
    protected void _onDialogCancel() {
        EnrollmentActivityGroup group = (EnrollmentActivityGroup)getParent();
        group.goToRoot();
    }
}
