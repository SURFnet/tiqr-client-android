package org.tiqr.authenticator.authentication;

import android.content.Intent;
import android.os.Bundle;

import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.general.AbstractActivityGroup;

/**
 * Authentication activity group.
 */
public class AuthenticationActivityGroup extends AbstractActivityGroup {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthenticationChallenge challenge = getIntent().getParcelableExtra("org.tiqr.challenge");
        setChallenge(challenge);

        if (challenge.getIdentity() == null) {
            Intent selectIntent = new Intent(this, AuthenticationIdentitySelectActivity.class);
            startChildActivity("AuthenticationIdentitySelectActivity", selectIntent);
        } else {
            Intent confirmIntent = new Intent(this, AuthenticationConfirmationActivity.class);
            startChildActivity("AuthenticateConfirmationActivity", confirmIntent);
        }
    }
}
