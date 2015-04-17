package org.tiqr.authenticator;

import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.auth.Challenge;
import org.tiqr.authenticator.auth.EnrollmentChallenge;
import org.tiqr.authenticator.authentication.AuthenticationActivityGroup;
import org.tiqr.authenticator.datamodel.DbAdapter;
import org.tiqr.authenticator.enrollment.EnrollmentActivityGroup;
import org.tiqr.service.authentication.AuthenticationService;
import org.tiqr.service.authentication.ParseAuthenticationChallengeError;
import org.tiqr.service.enrollment.EnrollmentService;
import org.tiqr.service.enrollment.ParseEnrollmentChallengeError;
import org.tiqr.service.notification.NotificationService;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.c2dm.C2DMessaging;

import javax.inject.Inject;

public class MainActivity extends TiqrActivity
{
    protected @Inject NotificationService _notificationService;

    protected @Inject EnrollmentService _enrollmentService;

    protected @Inject AuthenticationService _authenticationService;

    private ActivityDialog activityDialog;



    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState, R.layout.main);
        ((Application)getApplication()).inject(this);

        DbAdapter db = new DbAdapter(this);
        
        int contentResource = 0;
        if (db.identityCount() > 0) {
        	showIdentityButton();
        	contentResource = R.string.main_text_instructions;
        } else {
        	hideIdentityButton();
            contentResource = R.string.main_text_welcome;
        }

        loadContentsIntoWebView(contentResource,  R.id.webview);
    }

    public void onStart()
    {
        super.onStart();

        String deviceToken = C2DMessaging.getRegistrationId(this);
        if (deviceToken != null && !"".equals(deviceToken)) {
            _notificationService.sendRequestWithDeviceToken(deviceToken);
        } else {
            C2DMessaging.register(this, C2DMReceiver.SENDER_ID);
        }
        // Handle tiqrauth:// and tiqrenroll:// URLs
        final Intent intent = getIntent();
        final String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            // Perform the actions needed to parse the raw challenge
            activityDialog = ActivityDialog.show(this);
            String rawChallenge = intent.getDataString();
            if (_enrollmentService.isEnrollmentChallenge(rawChallenge)) {
                _enroll(rawChallenge);
            } else {
                _authenticate(rawChallenge);
            }
        }
    }

    public void showIncompatibilityDialog()
    {
        new IncompatibilityDialog().show(this);

    }

    /**
     * Parse authentication challenge and start authentication process.
     *
     * @param challenge The raw authentication challenge.
     */
    private void _authenticate(String challenge) {
        _authenticationService.parseAuthenticationChallenge(challenge, new AuthenticationService.OnParseAuthenticationChallengeListener() {
            @Override
            public void onParseAuthenticationChallengeSuccess(AuthenticationChallenge challenge) {
                activityDialog.cancel();
                Intent intent = new Intent(getApplicationContext(), AuthenticationActivityGroup.class);
                intent.putExtra("org.tiqr.challenge", challenge);
                intent.putExtra("org.tiqr.protocolVersion", "2");
                startActivity(intent);
            }

            @Override
            public void onParseAuthenticationChallengeError(ParseAuthenticationChallengeError error) {
                activityDialog.cancel();
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(error.getTitle())
                        .setMessage(error.getMessage())
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok_button, null)
                        .show();
            }
        });
    }

    /**
     * Parse enrollment challenge and start enrollment process.
     *
     * @param challenge The raw challenge to enroll for.
     */
    private void _enroll(String challenge) {
        _enrollmentService.parseEnrollmentChallenge(challenge, new EnrollmentService.OnParseEnrollmentChallengeListener() {
            @Override
            public void onParseEnrollmentChallengeSuccess(EnrollmentChallenge challenge) {
                activityDialog.cancel();
                Intent intent = new Intent(getApplicationContext(), EnrollmentActivityGroup.class);
                intent.putExtra("org.tiqr.challenge", challenge);
                intent.putExtra("org.tiqr.protocolVersion", "2");
                startActivity(intent);
            }

            @Override
            public void onParseEnrollmentChallengeError(ParseEnrollmentChallengeError error) {
                activityDialog.cancel();

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(error.getTitle())
                        .setMessage(error.getMessage())
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok_button, null)
                        .show();
            }
        });
    }
}
