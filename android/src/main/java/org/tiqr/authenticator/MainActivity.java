package org.tiqr.authenticator;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.webkit.WebView;

import com.google.android.c2dm.C2DMessaging;

import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.auth.EnrollmentChallenge;
import org.tiqr.authenticator.authentication.AuthenticationActivityGroup;
import org.tiqr.authenticator.datamodel.DbAdapter;
import org.tiqr.authenticator.dialog.ActivityDialog;
import org.tiqr.authenticator.enrollment.EnrollmentActivityGroup;
import org.tiqr.authenticator.general.HeaderView;
import org.tiqr.authenticator.qr.CaptureActivity;
import org.tiqr.service.authentication.AuthenticationService;
import org.tiqr.service.authentication.ParseAuthenticationChallengeError;
import org.tiqr.service.enrollment.EnrollmentService;
import org.tiqr.service.enrollment.ParseEnrollmentChallengeError;
import org.tiqr.service.notification.NotificationService;

import javax.inject.Inject;

public class MainActivity extends Activity {

    public static final int MY_PERMISSIONS_REQUEST_CAMERA = 42;


    @Inject
    NotificationService _notificationService;

    protected
    @Inject
    EnrollmentService _enrollmentService;

    protected
    @Inject
    AuthenticationService _authenticationService;

    protected
    @Inject
    DbAdapter _dbAdapter;

    private ActivityDialog activityDialog;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ((Application)getApplication()).inject(this);

        HeaderView headerView = (HeaderView)findViewById(R.id.headerView);
        headerView.hideLeftButton();

        int contentResource = 0;
        if (_dbAdapter.identityCount() > 0) {
            contentResource = R.string.main_text_instructions_for_pincode;
        } else {
            headerView.hideRightButton();
            contentResource = R.string.main_text_welcome;
        }

        loadContentsIntoWebView(contentResource, R.id.webview);

        findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    _startScanning();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    _startScanning();
                }
            }
        }
    }

    private void _startScanning() {
        Intent scanIntent = new Intent(MainActivity.this, CaptureActivity.class);
        startActivity(scanIntent);
    }

    public void loadContentsIntoWebView(int contentResourceId, int webviewResourceId) {
        WebView webView = (WebView)findViewById(webviewResourceId);
        String data = getString(contentResourceId);
        //needed to render chars correctly
        webView.loadDataWithBaseURL(null, data, "text/html", "utf-8", null);
        webView.setBackgroundColor(Color.TRANSPARENT);
    }

    public void onStart() {
        super.onStart();

        String deviceToken = C2DMessaging.getRegistrationId(this);
        if (deviceToken != null && !"" .equals(deviceToken)) {
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
