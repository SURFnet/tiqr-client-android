package org.tiqr.authenticator.authentication;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.tiqr.authenticator.MainActivity;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.general.AbstractActivityGroup;
import org.tiqr.authenticator.general.FooterView;
import org.tiqr.authenticator.general.HeaderView;

public class AuthenticationSummaryActivity extends AbstractActivityGroup {

    private FingerprintManagerCompat fingerprintManager;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        fingerprintManager = FingerprintManagerCompat.from(this);

        if (fingerprintManager.hasEnrolledFingerprints()) {
            _showFingerPrintUpgradeDialog();
        }
    }

    private void _showFingerPrintUpgradeDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.upgrade_to_touch_id_title))
                .setMessage(getString(R.string.upgrade_to_touch_id_message))
                .setPositiveButton(getString(R.string.upgrade_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNegativeButton(getString(R.string.cancel_button), null)
                .create()
                .show();
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
