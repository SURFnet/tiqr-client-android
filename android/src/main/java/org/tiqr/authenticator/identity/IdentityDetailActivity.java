package org.tiqr.authenticator.identity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.tiqr.authenticator.TiqrApplication;
import org.tiqr.authenticator.MainActivity;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.datamodel.DbAdapter;
import org.tiqr.authenticator.datamodel.Identity;
import org.tiqr.authenticator.datamodel.IdentityProvider;
import org.tiqr.authenticator.general.FooterView;
import org.tiqr.authenticator.general.HeaderView;
import org.tiqr.service.authentication.AuthenticationService;

import javax.inject.Inject;

public class IdentityDetailActivity extends Activity {

    private static final String TAG = IdentityDetailActivity.class.getName();

    @Inject
    protected AuthenticationService _authenticationService;

    protected Identity _identity;
    protected IdentityProvider _identityProvider;
    protected DbAdapter _db;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.identity_detail);
        TiqrApplication.Companion.component().inject(this);

        HeaderView headerView = (HeaderView)findViewById(R.id.headerView);
        headerView.setOnLeftClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        headerView.hideRightButton();

        _db = new DbAdapter(this);
        _setIdentityAndIdentityProvider();

        if (_identity != null && _identityProvider != null) {
            TextView identity_displayName = findViewById(R.id.identity_displayName);
            TextView identity_identifier = findViewById(R.id.identity_identifier);
            TextView identity_provider_displayName = findViewById(R.id.identity_provider_displayName);
            TextView identity_provider_identifier = findViewById(R.id.identity_provider_identifier);
            TextView identity_provider_info_url = findViewById(R.id.identity_provider_info_url);

            Switch usesFingerprint = findViewById(R.id.use_fingerprint_switch);
            Switch upgradeToFingerprint = findViewById(R.id.upgrade_fingerprint_switch);
            usesFingerprint.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    _identity.setUsingFingerprint(checked);
                    _db.updateIdentity(_identity);
                    _updateFingerPrintViews();
                }
            });

            upgradeToFingerprint.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    _identity.setShowFingerprintUpgrade(checked);
                    _db.updateIdentity(_identity);
                    _updateFingerPrintViews();
                }
            });

            _updateFingerPrintViews();

            identity_displayName.setText(_identity.getDisplayName());
            identity_identifier.setText(_identity.getIdentifier());
            identity_provider_displayName.setText(_identityProvider.getDisplayName());
            identity_provider_identifier.setText(_identityProvider.getIdentifier());
            final String infoUrl = _identityProvider.getInfoURL();
            try {
                String infoUrlHost = Uri.parse(infoUrl).getHost();
                identity_provider_info_url.setText(getString(R.string.information_with_host, infoUrlHost));
            } catch (Exception ex) {
                Log.w(TAG, "Unable to set URL on the information label.", ex);
                identity_provider_info_url.setText(_identityProvider.getInfoURL());
            }

            // Underline the textview to denote it is clickable
            identity_provider_info_url.setPaintFlags(identity_provider_info_url.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            identity_provider_info_url.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(infoUrl));
                    try {
                        startActivity(browserIntent);
                    } catch (ActivityNotFoundException ex) {
                        Toast.makeText(v.getContext(), R.string.error_browser_not_available, Toast.LENGTH_LONG).show();
                    }
                }
            });

            if (_identity.isBlocked()) {
                TextView identity_blocked_message = (TextView)findViewById(R.id.identity_blocked_message);
                identity_blocked_message.setVisibility(View.VISIBLE);
            }
        }

        Button delete_btn = (Button)findViewById(R.id.delete_button);
        delete_btn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(IdentityDetailActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.delete_confirm_title)
                        .setMessage(R.string.delete_confirm)
                        .setPositiveButton(R.string.delete_button, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                _deleteIdentity();
                            }

                        })
                        .setNegativeButton(R.string.cancel_button, null)
                        .show();
            }
        });

        FooterView footerView = (FooterView)findViewById(R.id.fallbackFooterView);
        footerView.hideInfoIcon();
    }

    protected void _updateFingerPrintViews() {
        View useFingerprintContainer = findViewById(R.id.use_fingerprint_container);
        View upgradeFingerprintContainer = findViewById(R.id.upgrade_fingerprint_container);
        Switch usesFingerprint = findViewById(R.id.use_fingerprint_switch);
        Switch upgradeToFingerprint = findViewById(R.id.upgrade_fingerprint_switch);

        FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(this);
        if(fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints()) {
            if (_identity.getUsingFingerprint()) {
                usesFingerprint.setChecked(true);
                upgradeFingerprintContainer.setVisibility(View.GONE);
                useFingerprintContainer.setVisibility(View.VISIBLE);
            } else if (_authenticationService.hasFingerprintSecret(_identity)) {
                usesFingerprint.setChecked(false);
                upgradeFingerprintContainer.setVisibility(View.GONE);
                useFingerprintContainer.setVisibility(View.VISIBLE);
            } else {
                useFingerprintContainer.setVisibility(View.GONE);
                upgradeFingerprintContainer.setVisibility(View.VISIBLE);
                upgradeToFingerprint.setChecked(_identity.getShowFingerprintUpgrade());
            }
        } else {
            useFingerprintContainer.setVisibility(View.GONE);
            upgradeFingerprintContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Fetch the identity and identity provider
     */
    protected void _setIdentityAndIdentityProvider() {
        long identity_id = getIntent().getLongExtra("org.tiqr.identity.id", 0);

        _identity = _db.getIdentityByIdentityId(identity_id);
        _identityProvider = _db.getIdentityProviderForIdentityId(identity_id);
    }

    /**
     *
     */
    protected void _deleteIdentity() {
        boolean isLastIdentity = false;
        Cursor cursor = _db.getAllIdentitiesWithIdentityProviderData();

        if (cursor != null) {
            if (cursor.getCount() < 2) {
                isLastIdentity = true;
            }
            cursor.close();
        }

        _db.deleteIdentity(_identity.getId());

        if (isLastIdentity) {
            startActivity(new Intent(this, MainActivity.class));
        }

        finish();
    }
}
