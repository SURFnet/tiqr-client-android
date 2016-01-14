package org.tiqr.authenticator.authentication;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.tiqr.Constants;
import org.tiqr.authenticator.Application;
import org.tiqr.authenticator.MainActivity;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.exceptions.SecurityFeaturesException;
import org.tiqr.authenticator.general.AbstractActivityGroup;
import org.tiqr.authenticator.general.FooterView;
import org.tiqr.authenticator.general.HeaderView;
import org.tiqr.authenticator.security.Encryption;
import org.tiqr.authenticator.security.Secret;

import java.security.InvalidKeyException;

import javax.crypto.SecretKey;
import javax.inject.Inject;

public class AuthenticationSummaryActivity extends AbstractActivityGroup {

    public static final String PIN = "PIN";

    // Logging tag
    private static final String TAG = "Wiebe";

    protected
    @Inject
    SharedPreferences _sharedPreferences;

    protected
    @Inject
    Context _context;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((Application)getApplication()).inject(this);

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
        if(fingerprintManager.hasEnrolledFingerprints() && _sharedPreferences.getBoolean(Constants.SHOW_FINGERPRINT_UPGRADE_DIALOG_PREF_KEY, false)) {
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

                        String pincode = getIntent().getStringExtra(PIN);
                        try {
                            if (pincode != null) {
                                SecretKey sessionKey = Encryption.keyFromPassword(getParent(), pincode);
                                AbstractActivityGroup parent = (AbstractActivityGroup)getParent();
                                AuthenticationChallenge challenge = (AuthenticationChallenge)parent.getChallenge();
                                Secret secret = Secret.secretForIdentity(challenge.getIdentity(), _context);

                                //Check if sessionKey is correct
                                secret.getSecret(sessionKey);

                                SecretKey newSessionKey = Encryption.keyFromPassword(getParent(), Constants.AUTHENTICATION_FINGERPRINT_KEY);
                                secret.storeInKeyStore(newSessionKey);
                            }
                            _useFingerPrintAsAuthentication();
                        } catch (SecurityFeaturesException | InvalidKeyException e) {
                            // No user action required
                            Log.e(TAG, "Not able to save the key to the keystore");
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel_button), null)
                .create()
                .show();
    }

    /**
     * Sets the fingerprint authentication method.
     */
    private void _useFingerPrintAsAuthentication() {
        SharedPreferences.Editor editor = _sharedPreferences.edit();
        editor.putBoolean(Constants.USE_AUTHENTICATION_FINGERPRINT_PREF_KEY, true);
        editor.putBoolean(Constants.SHOW_FINGERPRINT_UPGRADE_DIALOG_PREF_KEY, false);
        editor.commit();
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
