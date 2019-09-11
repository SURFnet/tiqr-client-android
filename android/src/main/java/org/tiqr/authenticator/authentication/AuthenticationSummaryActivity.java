package org.tiqr.authenticator.authentication;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.tiqr.Constants;
import org.tiqr.authenticator.TiqrApplication;
import org.tiqr.authenticator.MainActivity;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.exceptions.SecurityFeaturesException;
import org.tiqr.authenticator.general.AbstractActivityGroup;
import org.tiqr.authenticator.general.FooterView;
import org.tiqr.authenticator.general.HeaderView;
import org.tiqr.authenticator.security.Encryption;
import org.tiqr.authenticator.security.Secret;
import org.tiqr.service.authentication.AuthenticationService;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.SecretKey;
import javax.inject.Inject;

public class AuthenticationSummaryActivity extends AbstractActivityGroup {

    public static final String PIN = "PIN";

    // Logging tag
    private static final String TAG = AuthenticationSummaryActivity.class.getSimpleName();

    protected
    @Inject
    Context _context;

    protected
    @Inject
    SharedPreferences _sharedPreferences;

    protected
    @Inject
    AuthenticationService _authenticationService;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TiqrApplication.Companion.component().inject(this);

        setContentView(R.layout.authentication_summary);

        HeaderView headerView = (HeaderView)findViewById(R.id.headerView);
        headerView.hideLeftButton();
        headerView.hideRightButton();

        AbstractActivityGroup parent = (AbstractActivityGroup)getParent();

        TextView dn = (TextView)findViewById(R.id.display_name);
        dn.setText(parent.getChallenge().getIdentity().getDisplayName());

        TextView ipdn = (TextView)findViewById(R.id.identity_provider_name);
        ipdn.setText(parent.getChallenge().getIdentity().getIdentifier());

        // TODO: When a service provider identifier is available, switch these 2 around.
        TextView spdn = (TextView)findViewById(R.id.service_provider_display_name);
        spdn.setText(((AuthenticationChallenge)parent.getChallenge()).getServiceProviderDisplayName());

        TextView spi = (TextView)findViewById(R.id.service_provider_identifier);
        spi.setText("");

        final Button ok = (Button)findViewById(R.id.confirm_button);
        if (ok != null) {
            ok.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    _returnToHome();
                }
            });
        }

        FooterView footer = (FooterView)findViewById(R.id.fallbackFooterView);

        if (footer != null) {
            footer.hideInfoIcon();
        }
        FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(this);
        if(fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints() && parent.getChallenge().getIdentity().getShowFingerprintUpgrade()) {
            _showFingerPrintUpgradeDialog();
        }
    }

    /**
     * Show a dialog to ask if the user wants to upgrade to fingerprint authentication.
     */
    private void _showFingerPrintUpgradeDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.upgrade_to_touch_id_title))
                .setMessage(getString(R.string.upgrade_to_touch_id_message))
                .setPositiveButton(getString(R.string.upgrade_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        _upgradeToFingerprint();
                    }
                })
                .setCancelable(false)
                .setNegativeButton(getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AbstractActivityGroup parent = (AbstractActivityGroup)getParent();
                        AuthenticationChallenge challenge = (AuthenticationChallenge)parent.getChallenge();
                        _authenticationService.shouldShowFingerprintUpgradeForIdentitiy(challenge.getIdentity(), false);
                    }
                })
                .create()
                .show();
    }

    private void _upgradeToFingerprint() {
        String pincode = getIntent().getStringExtra(PIN);
        try {
            AbstractActivityGroup parent = (AbstractActivityGroup)getParent();
            AuthenticationChallenge challenge = (AuthenticationChallenge)parent.getChallenge();
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

    /**
     * Return to the home screen
     */
    protected void _returnToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
