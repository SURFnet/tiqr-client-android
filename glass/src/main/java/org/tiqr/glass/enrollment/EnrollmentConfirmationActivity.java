package org.tiqr.glass.enrollment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.glass.widget.CardBuilder;

import org.tiqr.authenticator.auth.EnrollmentChallenge;
import org.tiqr.glass.Application;
import org.tiqr.glass.R;
import org.tiqr.glass.general.ErrorActivity;
import org.tiqr.service.enrollment.EnrollmentError;
import org.tiqr.service.enrollment.EnrollmentService;

import javax.inject.Inject;

/**
 * Enrollment confirmation.
 */
public class EnrollmentConfirmationActivity extends Activity {
    private final static String CHALLENGE = "CHALLENGE";

    protected @Inject EnrollmentService _enrollmentService;

    private View _progressView;

    /**
     * Create intent.
     */
    public static Intent createIntent(Context context, EnrollmentChallenge challenge) {
        Intent intent = new Intent(context, EnrollmentConfirmationActivity.class);
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

        setContentView(R.layout.enrollment_confirmation);
        _progressView = findViewById(R.id.progress);

        EnrollmentChallenge challenge = getIntent().getParcelableExtra(CHALLENGE);

        View cardView =
            new CardBuilder(this, CardBuilder.Layout.COLUMNS)
                .setIcon(challenge.getIdentityProvider().getLogoBitmap())
                .setText(getString(R.string.enrollment_confirmation_message, challenge.getIdentity().getDisplayName(), challenge.getIdentityProvider().getDisplayName()))
                .setFootnote(R.string.enrollment_confirmation_tap)
                .getView();

        ViewGroup containerView = (ViewGroup)findViewById(R.id.container);
        containerView.addView(cardView);
    }


    /**
     * Handle gestures.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
                _confirm();
                return true;
            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Confirm enrollment.
     */
    private void _confirm() {
        _progressView.setVisibility(View.VISIBLE);

        final EnrollmentChallenge challenge = getIntent().getParcelableExtra(CHALLENGE);
        _enrollmentService.enroll(challenge, "0000", new EnrollmentService.OnEnrollmentListener() {
            @Override
            public void onEnrollmentSuccess() {
                finish();
                Intent intent = EnrollmentSummaryActivity.createIntent(EnrollmentConfirmationActivity.this, challenge);
                startActivity(intent);
            }

            @Override
            public void onEnrollmentError(EnrollmentError error) {
                finish();
                Intent intent = ErrorActivity.createIntent(EnrollmentConfirmationActivity.this, error.getMessage());
                startActivity(intent);
            }
        });
    }
}
