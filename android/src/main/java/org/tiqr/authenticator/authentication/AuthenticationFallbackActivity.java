package org.tiqr.authenticator.authentication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.tiqr.authenticator.IncompatibilityDialog;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.auth.AuthenticationChallenge;
import org.tiqr.authenticator.exceptions.InvalidChallengeException;
import org.tiqr.authenticator.exceptions.SecurityFeaturesException;
import org.tiqr.authenticator.general.AbstractActivityGroup;
import org.tiqr.authenticator.general.ErrorActivity;
import org.tiqr.authenticator.general.HeaderView;
import org.tiqr.authenticator.security.Encryption;
import org.tiqr.authenticator.security.OCRAProtocol;
import org.tiqr.authenticator.security.OCRAWrapper;
import org.tiqr.authenticator.security.OCRAWrapper_v1;
import org.tiqr.authenticator.security.Secret;

import java.security.InvalidKeyException;

import javax.crypto.SecretKey;

/**
 * Confirmation dialog for authentication challenge.
 */
public class AuthenticationFallbackActivity extends Activity {

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fallback);

        HeaderView headerView = (HeaderView)findViewById(R.id.headerView);
        headerView.setOnLeftClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        headerView.hideRightButton();

        TextView identifier = (TextView)findViewById(R.id.identifier);
        identifier.setText(((AuthenticationActivityGroup)getParent()).getChallenge().getIdentity().getIdentifier());

        Button ok = (Button)findViewById(R.id.confirm_button);
        ok.setText(R.string.authentication_fallback_button);

        if (ok != null) {
            ok.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        String pincode = getIntent().getStringExtra("org.tiqr.authentication.pincode");
        _fetchOTP(pincode);
    }

    /**
     * Given the pincode, try to fetch a One Time Password from the server and set it in the view
     *
     * @param pincode
     */
    protected void _fetchOTP(String pincode) {
        try {
            SecretKey sessionKey = Encryption.keyFromPassword(getParent(), pincode);
            AbstractActivityGroup parent = (AbstractActivityGroup)getParent();
            AuthenticationChallenge challenge = (AuthenticationChallenge)parent.getChallenge();
            Secret secret = Secret.secretForIdentity(challenge.getIdentity(), this);
            SecretKey secretKey = secret.getSecret(sessionKey);

            OCRAProtocol ocra;
            if (challenge.getProtocolVersion().equals("1")) {
                ocra = new OCRAWrapper_v1();
            } else {
                ocra = new OCRAWrapper();
            }

            String otp = ocra.generateOCRA(
                    challenge.getIdentityProvider().getOCRASuite(),
                    secretKey.getEncoded(),
                    challenge.getChallenge(),
                    challenge.getSessionKey());

            TextView otpView = (TextView)findViewById(R.id.otp);
            otpView.setText(otp);
        } catch (InvalidChallengeException e) {
            _showErrorActivityWithMessage(getString(R.string.authentication_failure_title), getString(R.string.error_auth_invalid_challenge));
        } catch (ArrayIndexOutOfBoundsException e) {
            _showErrorActivityWithMessage(getString(R.string.authentication_failure_title), getString(R.string.error_auth_server_incompatible));
        } catch (InvalidKeyException e) {
            _showErrorActivityWithMessage(getString(R.string.authentication_failure_title), getString(R.string.error_auth_invalid_key));
        } catch (SecurityFeaturesException e) {
            new IncompatibilityDialog().show(this);
        } catch (NumberFormatException e) {
            _showErrorActivityWithMessage(getString(R.string.authentication_failure_title), getString(R.string.error_auth_invalid_challenge));
        }
    }

    /**
     * Shows the error activity if needed, with a title and message
     *
     * @param title
     * @param message
     */
    protected void _showErrorActivityWithMessage(String title, String message) {
        new ErrorActivity.ErrorBuilder()
                .setTitle(title)
                .setMessage(message)
                .show(this);
    }

    @Override
    public void onBackPressed() {
        AbstractActivityGroup parent = (AbstractActivityGroup)getParent();
        parent.finish();
    }
}