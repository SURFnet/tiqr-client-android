package org.tiqr.glass.scan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;

import com.google.android.glass.media.Sounds;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraConfigurationUtils;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;

import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.auth.EnrollmentChallenge;
import org.tiqr.glass.Application;
import org.tiqr.glass.authentication.AuthenticationIdentitySelectActivity;
import org.tiqr.glass.general.ErrorActivity;
import org.tiqr.glass.R;
import org.tiqr.glass.authentication.AuthenticationConfirmationActivity;
import org.tiqr.glass.enrollment.EnrollmentConfirmationActivity;
import org.tiqr.scan.ScanHelper;
import org.tiqr.scan.ViewfinderView;
import org.tiqr.service.authentication.AuthenticationService;
import org.tiqr.service.authentication.AuthenticationService.OnParseAuthenticationChallengeListener;
import org.tiqr.service.authentication.ParseAuthenticationChallengeError;
import org.tiqr.service.enrollment.EnrollmentService;
import org.tiqr.service.enrollment.EnrollmentService.OnParseEnrollmentChallengeListener;
import org.tiqr.service.enrollment.ParseEnrollmentChallengeError;

import javax.inject.Inject;

/**
 * Scanner.
 */
public final class ScanActivity extends Activity implements ScanHelper.OnScanListener {
    private final static int CAMERA_PREVIEW_WIDTH = 1280;
    private final static int CAMERA_PREVIEW_HEIGHT = 720;
    private final static int CAMERA_ZOOM = 2;

    protected @Inject AuthenticationService _authenticationService;
    protected @Inject EnrollmentService _enrollmentService;

    private ViewfinderView _viewfinderView;
    private View _progressView;
    private ScanHelper _captureManager;

    /**
     * Create.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((Application)getApplication()).inject(this);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.scan);

        _viewfinderView = (ViewfinderView)findViewById(R.id.viewfinder);
        _progressView = findViewById(R.id.progress);

        TextureView previewView = (TextureView)findViewById(R.id.preview);
        _captureManager = new ScanHelper(previewView, this);
    }

    /**
     * Resume.
     */
    @Override
    public synchronized void onResume() {
        super.onResume();
        _captureManager.start();
    }

    /**
     * Pause.
     */
    @Override
    public synchronized void onPause() {
        super.onPause();
        _captureManager.stop();
    }

    /**
     * Initialize camera and screen.
     *
     * @param helper
     * @param camera
     */
    @Override
    public void onScanCameraOpen(ScanHelper helper, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        params.setPreviewSize(CAMERA_PREVIEW_WIDTH, CAMERA_PREVIEW_HEIGHT);
        CameraConfigurationUtils.setFocus(params, true, false, false);
        CameraConfigurationUtils.setBestPreviewFPS(params);
        CameraConfigurationUtils.setBarcodeSceneMode(params);
        CameraConfigurationUtils.setVideoStabilization(params);
        CameraConfigurationUtils.setMetering(params);
        CameraConfigurationUtils.setZoom(params, CAMERA_ZOOM);
        camera.setParameters(params);

        _viewfinderView.setPreviewSize(params.getPreviewSize().width, params.getPreviewSize().height);

        int finderAreaWidth = Math.max(5 * _viewfinderView.getWidth() / 8, 240);
        int finderAreaHeight = Math.max(5 * _viewfinderView.getHeight() / 8, 240);
        Rect finderArea = new Rect();
        finderArea.left = (_viewfinderView.getWidth() - finderAreaWidth) / 2;
        finderArea.top = (_viewfinderView.getHeight() - finderAreaHeight) / 2;
        finderArea.right = finderArea.left + finderAreaWidth;
        finderArea.bottom = finderArea.top + finderAreaHeight;
        _viewfinderView.setFinderArea(finderArea);

        Rect scanArea = new Rect();
        scanArea.left = (int)Math.floor(((float)params.getPreviewSize().width / _viewfinderView.getWidth()) * finderArea.left);
        scanArea.top = (int)Math.floor(((float)params.getPreviewSize().height / _viewfinderView.getHeight()) * finderArea.top);
        scanArea.right =  (int)Math.floor(((float)params.getPreviewSize().width / _viewfinderView.getWidth()) * finderArea.right);
        scanArea.bottom = (int)Math.floor(((float)params.getPreviewSize().height / _viewfinderView.getHeight()) * finderArea.bottom);
        helper.setScanArea(scanArea);
    }

    /**
     * Possible result point.
     *
     * @param helper
     * @param point
     */
    @Override
    public void onScanPossibleResultPoint(ScanHelper helper, ResultPoint point) {
        _viewfinderView.addPossibleResultPoint(point);
    }

    /**
     * Scan match.
     *
     * @param helper
     * @param result
     *
     * @return Success (or continue scanning)?
     */
    @Override
    public boolean onScanResult(ScanHelper helper, ParsedResult result) {
        _progressView.setVisibility(View.VISIBLE);

        boolean success = false;
        if (_authenticationService.isAuthenticationChallenge(result.getDisplayResult())) {
            success = true;
            _authenticate(result.getDisplayResult());
        } else if (_enrollmentService.isEnrollmentChallenge(result.getDisplayResult())) {
            success = true;
            _enroll(result.getDisplayResult());
        }

        // play sound
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.playSoundEffect(success ? Sounds.SUCCESS : Sounds.ERROR);

        return success;
    }

    /**
     * Parse authentication challenge and start authentication process.
     *
     * @param challenge
     */
    private void _authenticate(String challenge) {
        _authenticationService.parseAuthenticationChallenge(challenge, new OnParseAuthenticationChallengeListener() {
            @Override
            public void onParseAuthenticationChallengeSuccess(AuthenticationChallenge challenge) {
                _progressView.setVisibility(View.GONE);

                Intent intent;
                if (challenge.getIdentity() == null) {
                    intent = AuthenticationIdentitySelectActivity.createIntent(ScanActivity.this, challenge);
                } else {
                    intent = AuthenticationConfirmationActivity.createIntent(ScanActivity.this, challenge);
                }

                startActivity(intent);
            }

            @Override
            public void onParseAuthenticationChallengeError(ParseAuthenticationChallengeError error) {
                _progressView.setVisibility(View.GONE);
                Intent intent = ErrorActivity.createIntent(ScanActivity.this, error.getMessage());
                startActivity(intent);
            }
        });
    }

    /**
     * Parse enrollment challenge and start enrollment process.
     *
     * @param challenge
     */
    private void _enroll(String challenge) {
        _enrollmentService.parseEnrollmentChallenge(challenge, new OnParseEnrollmentChallengeListener() {
            @Override
            public void onParseEnrollmentChallengeSuccess(EnrollmentChallenge challenge) {
                _progressView.setVisibility(View.GONE);
                Intent intent = EnrollmentConfirmationActivity.createIntent(ScanActivity.this, challenge);
                startActivity(intent);
            }

            @Override
            public void onParseEnrollmentChallengeError(ParseEnrollmentChallengeError error) {
                _progressView.setVisibility(View.GONE);
                Intent intent = ErrorActivity.createIntent(ScanActivity.this, error.getMessage());
                startActivity(intent);
            }
        });
    }
}