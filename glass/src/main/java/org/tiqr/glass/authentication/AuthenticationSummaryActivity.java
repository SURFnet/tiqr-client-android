package org.tiqr.glass.authentication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import com.google.android.glass.widget.CardBuilder;

import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.glass.R;

/**
 * Authentication summary.
 */
public class AuthenticationSummaryActivity extends Activity {
    private final static String CHALLENGE = "CHALLENGE";

    public static Intent createIntent(Context context, AuthenticationChallenge challenge) {
        Intent intent = new Intent(context, AuthenticationSummaryActivity.class);
        intent.putExtra(CHALLENGE, challenge);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthenticationChallenge challenge = getIntent().getParcelableExtra(CHALLENGE);

        View cardView =
            new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText(getString(R.string.authentication_summary_message, challenge.getIdentity().getDisplayName(), challenge.getIdentityProvider().getDisplayName()))
                .getView();

        setContentView(cardView);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
                return true;
            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
