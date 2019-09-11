package org.tiqr.authenticator.authentication;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import org.tiqr.authenticator.R;
import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.auth.Challenge;
import org.tiqr.authenticator.datamodel.Identity;
import org.tiqr.authenticator.general.AbstractActivityGroup;
import org.tiqr.authenticator.general.HeaderView;
import org.tiqr.authenticator.identity.AbstractIdentityListActivity;

public class AuthenticationIdentitySelectActivity extends AbstractIdentityListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        _resourceid = R.layout.select_identity_listitem;
        super.onCreate(savedInstanceState);

        HeaderView headerView = findViewById(R.id.headerView);
        headerView.setOnLeftClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        headerView.hideRightButton();

        ListView lv = getListView();

        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Cursor c = getIdentityCursor();
                c.moveToPosition(position);
                Identity identity = _db.createIdentityObjectForCurrentCursorPosition(c);

                AuthenticationChallenge challenge = (AuthenticationChallenge)_getChallenge();
                challenge.setIdentity(identity);

                _doAuthentication();

            }

        });

    }

    /**
     * Returns the challenge.
     *
     * @return challenge
     */
    protected Challenge _getChallenge() {
        AbstractActivityGroup parent = (AbstractActivityGroup)getParent();
        return parent.getChallenge();
    }

    @Override
    public Cursor getIdentityCursor() {
        // The default implementation reuses _identitiesCursor but every time we open the select screen
        // we need to get a fresh cursor.
        String identityProviderIdentifier = _getChallenge().getIdentityProvider().getIdentifier();
        return _db.findIdentitiesByIdentityProviderIdentifier(identityProviderIdentifier);
    }

    private void _doAuthentication() {
        AuthenticationActivityGroup group = (AuthenticationActivityGroup)getParent();
        Intent confirmIntent = new Intent(this, AuthenticationConfirmationActivity.class);
        group.startChildActivity("AuthenticateConfirmationActivity", confirmIntent);
    }

}
