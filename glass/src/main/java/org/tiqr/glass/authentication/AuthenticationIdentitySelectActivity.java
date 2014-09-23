package org.tiqr.glass.authentication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.google.android.glass.widget.CardScrollView;

import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.datamodel.DbAdapter;
import org.tiqr.authenticator.datamodel.Identity;
import org.tiqr.glass.Application;
import org.tiqr.glass.R;
import org.tiqr.glass.general.ErrorActivity;
import org.tiqr.glass.identity.IdentityScrollAdapter;
import org.tiqr.service.authentication.AuthenticationError;
import org.tiqr.service.authentication.AuthenticationService;

import javax.inject.Inject;

/**
 * Select identity for login.
 */
public class AuthenticationIdentitySelectActivity extends Activity {
    private final static String CHALLENGE = "CHALLENGE";

    protected @Inject AuthenticationService _authenticationService;

    private View _progressView;

    /**
     * Create intent.
     */
    public static Intent createIntent(Context context, AuthenticationChallenge challenge) {
        Intent intent = new Intent(context, AuthenticationIdentitySelectActivity.class);
        intent.putExtra(CHALLENGE, challenge);
        return intent;
    }

    /**
     * Create.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((Application)getApplication()).inject(this);

        setContentView(R.layout.identity_select);

        _progressView = findViewById(R.id.progress);

        final IdentityScrollAdapter scrollAdapter = new IdentityScrollAdapter(this);
        scrollAdapter.setFootnote(getString(R.string.authentication_identity_select_footnote));

        DbAdapter db = new DbAdapter(this);
        AuthenticationChallenge challenge = getIntent().getParcelableExtra(CHALLENGE);
        Identity[] identities = db.findIdentitiesByIdentityProviderIdAsObjects(challenge.getIdentityProvider().getId());
        scrollAdapter.setIdentities(identities);

        CardScrollView scrollView = (CardScrollView)findViewById(R.id.scrollView);
        scrollView.setAdapter(scrollAdapter);
        scrollView.activate();

        scrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                _onIdentityTap(scrollAdapter.getItem(position));
            }
        });
    }

    /**
     * Identity tap.
     *
     * @param identity
     */
    private void _onIdentityTap(Identity identity) {
        _progressView.setVisibility(View.VISIBLE);

        final AuthenticationChallenge challenge = getIntent().getParcelableExtra(CHALLENGE);
        challenge.setIdentity(identity);

        _authenticationService.authenticate(challenge, "0000", new AuthenticationService.OnAuthenticationListener() {
            @Override
            public void onAuthenticationSuccess() {
                finish();
                Intent intent = AuthenticationSummaryActivity.createIntent(AuthenticationIdentitySelectActivity.this, challenge);
                startActivity(intent);
            }

            @Override
            public void onAuthenticationError(AuthenticationError error) {
                finish();
                Intent intent = ErrorActivity.createIntent(AuthenticationIdentitySelectActivity.this, error.getMessage());
                startActivity(intent);
            }
        });
    }
}
