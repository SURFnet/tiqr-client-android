package org.tiqr.authenticator.enrollment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;

import org.tiqr.Constants;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.TiqrApplication;
import org.tiqr.authenticator.auth.EnrollmentChallenge;
import org.tiqr.authenticator.datamodel.Identity;
import org.tiqr.authenticator.exceptions.SecurityFeaturesException;
import org.tiqr.authenticator.general.AbstractActivityGroup;
import org.tiqr.authenticator.general.AbstractPincodeActivity;
import org.tiqr.authenticator.general.ErrorActivity;
import org.tiqr.authenticator.security.Encryption;
import org.tiqr.authenticator.security.Secret;
import org.tiqr.service.authentication.AuthenticationService;
import org.tiqr.service.enrollment.EnrollmentError;
import org.tiqr.service.enrollment.EnrollmentService;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.SecretKey;
import javax.inject.Inject;

/**
 * Verify enrollment PIN and start enrollment process.
 */
public class EnrollmentPincodeVerificationActivity extends AbstractPincodeActivity {

    // Logging tag
    private static final String TAG = EnrollmentPincodeVerificationActivity.class.getSimpleName();

    protected
    @Inject
    Context _context;

    protected
    @Inject
    EnrollmentService _enrollmentService;

    protected
    @Inject
    AuthenticationService _authenticationService;

    protected String firstPin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TiqrApplication.Companion.component().inject(this);

        firstPin = getIntent().getStringExtra("org.tiqr.firstPin");

        // Update the text.
        title.setText(R.string.entroll_pin_verify_title);
        setIntoText(R.string.login_verify_intro);
    }

    @Override
    public void process() {
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

        EnrollmentChallenge challenge = (EnrollmentChallenge) _getChallenge();
        _enroll(challenge, secondPin);
    }

    /**
     * Enroll user
     * <p>
     * We run this in a new thread here because otherwise, the activity dialog wouldn't show
     *
     * @param challenge Challenge.
     * @param pin       PIN code.
     */
    private void _enroll(EnrollmentChallenge challenge, final String pin) {
        _showProgressDialog(getString(R.string.enrolling));

        _enrollmentService.enroll(challenge, pin, new EnrollmentService.OnEnrollmentListener() {
            @Override
            public void onEnrollmentSuccess() {
                _cancelProgressDialog();
                FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(EnrollmentPincodeVerificationActivity.this);
                if (fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints() && getIdentity().getShowFingerprintUpgrade()) {
                    _showFingerPrintUpgradeDialog(pin);
                } else {
                    startEnrollmentSummaryActivity();
                }
            }

            @Override
            public void onEnrollmentError(EnrollmentError error) {
                _cancelProgressDialog();
                new ErrorActivity.ErrorBuilder()
                        .setTitle(error.getTitle())
                        .setMessage(error.getMessage())
                        .setException(error.getException())
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
            EnrollmentActivityGroup group = (EnrollmentActivityGroup) getParent();
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
        EnrollmentActivityGroup group = (EnrollmentActivityGroup) getParent();
        group.goToRoot();
    }

    private Identity getIdentity() {
        AbstractActivityGroup parent = (AbstractActivityGroup) getParent();
        return parent.getChallenge().getIdentity();
    }

    /**
     * Show a dialog to ask if the user wants to upgrade to fingerprint authentication.
     */
    private void _showFingerPrintUpgradeDialog(final String pin) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.upgrade_to_touch_id_title))
                .setMessage(getString(R.string.upgrade_to_touch_id_message))
                .setPositiveButton(getString(R.string.upgrade_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        _upgradeToFingerprint(pin);
                    }
                })
                .setCancelable(false)
                .setNegativeButton(getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        _authenticationService.shouldShowFingerprintUpgradeForIdentitiy(getIdentity(), false);
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        startEnrollmentSummaryActivity();
                        _cancelProgressDialog();
                    }
                })
                .create()
                .show();
    }

    private void _upgradeToFingerprint(String pincode) {
        try {
            AbstractActivityGroup parent = (AbstractActivityGroup) getParent();
            EnrollmentChallenge challenge = (EnrollmentChallenge) parent.getChallenge();
            if (pincode != null) {
                SecretKey sessionKey = Encryption.INSTANCE.keyFromPassword(getParent(), pincode);
                Secret secret = Secret.Companion.secretForIdentity(challenge.getIdentity(), _context);
                //Check if sessionKey is correct
                secret.getSecret(sessionKey, Secret.Type.PINCODE);
                SecretKey newSessionKey = Encryption.INSTANCE.keyFromPassword(getParent(), Constants.INSTANCE.getAUTHENTICATION_FINGERPRINT_KEY());
                secret.storeInKeyStore(newSessionKey, Secret.Type.FINGERPRINT);
            }
            _authenticationService.useFingerPrintAsAuthenticationForIdentity(challenge.getIdentity());
        } catch (SecurityFeaturesException | InvalidKeyException | CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException | UnrecoverableEntryException e) {
            // No user action required
            Log.e(TAG, "Not able to save the key to the keystore", e);
        }
    }

    private void startEnrollmentSummaryActivity() {
        EnrollmentActivityGroup group = (EnrollmentActivityGroup) getParent();
        Intent summaryIntent = new Intent(EnrollmentPincodeVerificationActivity.this, EnrollmentSummaryActivity.class);
        group.startChildActivity("EnrollmentSummaryActivity", summaryIntent);
    }
}
