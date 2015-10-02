package org.tiqr.authenticator.identity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.tiqr.authenticator.MainActivity;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.datamodel.DbAdapter;
import org.tiqr.authenticator.datamodel.Identity;
import org.tiqr.authenticator.datamodel.IdentityProvider;
import org.tiqr.authenticator.general.FooterView;
import org.tiqr.authenticator.general.HeaderView;

public class IdentityDetailActivity extends Activity {

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
            TextView identity_displayName = (TextView)findViewById(R.id.identity_displayName);
            TextView identity_identifier = (TextView)findViewById(R.id.identity_identifier);
            TextView identity_provider_displayName = (TextView)findViewById(R.id.identity_provider_displayName);
            TextView identity_provider_identifier = (TextView)findViewById(R.id.identity_provider_identifier);
            TextView identity_provider_info_url = (TextView)findViewById(R.id.identity_provider_infoURL);

            identity_displayName.setText(_identity.getDisplayName());
            identity_identifier.setText(_identity.getIdentifier());
            identity_provider_displayName.setText(_identityProvider.getDisplayName());
            identity_provider_identifier.setText(_identityProvider.getIdentifier());
            identity_provider_info_url.setText(_identityProvider.getInfoURL());

            if (_identity.isBlocked()) {
                TextView identity_blocked_message = (TextView)findViewById(R.id.identity_blocked_message);
                identity_blocked_message.setVisibility(View.VISIBLE);
            }

            // Make the info url clickable
            Linkify.addLinks(identity_provider_info_url, Linkify.WEB_URLS);
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
