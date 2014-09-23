package org.tiqr.glass.enrollment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import com.google.android.glass.widget.CardBuilder;

import org.tiqr.authenticator.auth.EnrollmentChallenge;
import org.tiqr.glass.R;

/**
 * Enrollment summary.
 */
public class EnrollmentSummaryActivity extends Activity {
    private final static String CHALLENGE = "CHALLENGE";

    /**
     * Create intent.
     */
    public static Intent createIntent(Context context, EnrollmentChallenge challenge) {
        Intent intent = new Intent(context, EnrollmentSummaryActivity.class);
        intent.putExtra(CHALLENGE, challenge);
        return intent;
    }

    /**
     * Create.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EnrollmentChallenge challenge = getIntent().getParcelableExtra(CHALLENGE);

        View cardView =
            new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText(getString(R.string.enrollment_summary_message, challenge.getIdentity().getDisplayName(), challenge.getIdentityProvider().getDisplayName()))
                .getView();

        setContentView(cardView);
    }

    /**
     * Handle gestures.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
