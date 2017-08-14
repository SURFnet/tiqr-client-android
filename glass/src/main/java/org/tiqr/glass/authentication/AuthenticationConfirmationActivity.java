package org.tiqr.glass.authentication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.glass.widget.CardBuilder;

import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.auth.Challenge;
import org.tiqr.glass.Application;
import org.tiqr.glass.R;
import org.tiqr.glass.general.ErrorActivity;
import org.tiqr.service.authentication.AuthenticationError;
import org.tiqr.service.authentication.AuthenticationService;

import javax.inject.Inject;

/**
 * Authentication confirmation.
 */
public class AuthenticationConfirmationActivity extends Activity {
    private final static String CHALLENGE = "CHALLENGE";

    protected
    @Inject
    AuthenticationService _authenticationService;

    private View _progressView;

    /**
     * Create intent.
     */
    public static Intent createIntent(Context context, AuthenticationChallenge challenge) {
        Intent intent = new Intent(context, AuthenticationConfirmationActivity.class);
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

        setContentView(R.layout.authentication_confirmation);
        _progressView = findViewById(R.id.progress);

        final AuthenticationChallenge challenge = getIntent().getParcelableExtra(CHALLENGE);
        if (challenge == null) {
            throw new RuntimeException(this.getClass().getName() + " can only start if you provide it a challenge in the intent!");
        }

        RequestOptions requestOptions = new RequestOptions()
                .priority(Priority.IMMEDIATE);
        Glide.with(this)
                .asBitmap()
                .load(challenge.getIdentityProvider().getLogoURL())
                .apply(requestOptions)
                .into(new SimpleTarget<Bitmap>() {

                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        if (resource != null) {
                            _displayCardWithIcon(challenge, new BitmapDrawable(getResources(), resource));
                        } else {
                            _displayCardWithIcon(challenge, getResources().getDrawable(R.drawable.ic_launcher));
                        }
                    }
                });
    }

    /**
     * Displays the glass card with the provided challenge and icon.
     *
     * @param challenge The current challenge.
     * @param icon      The icon for the challenge.
     */
    private void _displayCardWithIcon(Challenge challenge, Drawable icon) {
        View cardView =
                new CardBuilder(this, CardBuilder.Layout.COLUMNS)
                        .setIcon(icon)
                        .setText(getString(R.string.authentication_confirmation_message, challenge.getIdentity().getDisplayName(), challenge.getIdentityProvider().getDisplayName()))
                        .setFootnote(R.string.authentication_confirmation_tap)
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
     * Confirm login.
     */
    private void _confirm() {
        _progressView.setVisibility(View.VISIBLE);

        final AuthenticationChallenge challenge = getIntent().getParcelableExtra(CHALLENGE);
        _authenticationService.authenticate(challenge, "0000", new AuthenticationService.OnAuthenticationListener() {
            @Override
            public void onAuthenticationSuccess() {
                finish();
                Intent intent = AuthenticationSummaryActivity.createIntent(AuthenticationConfirmationActivity.this, challenge);
                startActivity(intent);
            }

            @Override
            public void onAuthenticationError(AuthenticationError error) {
                finish();
                Intent intent = ErrorActivity.createIntent(AuthenticationConfirmationActivity.this, error.getMessage());
                startActivity(intent);
            }
        });
    }
}
