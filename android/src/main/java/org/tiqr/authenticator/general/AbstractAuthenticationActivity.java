package org.tiqr.authenticator.general;

import android.app.Activity;
import android.app.ProgressDialog;

import org.tiqr.authenticator.auth.Challenge;

/**
 * Abstract class to handle authentication.
 *
 * @author Wiebe
 */
public abstract class AbstractAuthenticationActivity extends Activity {

    private ProgressDialog _progressDialog;


    /**
     * Returns the challenge.
     *
     * @return challenge
     */
    protected Challenge _getChallenge() {
        AbstractActivityGroup parent = (AbstractActivityGroup)getParent();
        return parent.getChallenge();
    }

    /**
     * Show the user a progress dialog
     *
     * @param title The title that will be shown in the progress dialog
     */
    protected void _showProgressDialog(String title) {
        _progressDialog = new ProgressDialog(this);
        _progressDialog.setTitle(title);
        _progressDialog.show();
    }

    protected void _cancelProgressDialog() {
        if (_progressDialog != null && _progressDialog.isShowing()) {
            _progressDialog.dismiss();
            _progressDialog = null;
        }
    }
}
