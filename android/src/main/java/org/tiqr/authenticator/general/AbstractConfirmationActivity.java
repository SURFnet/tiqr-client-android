package org.tiqr.authenticator.general;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.tiqr.authenticator.MainActivity;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.auth.Challenge;

/**
 * Confirmation activity base class for authentication and enrollment confirmation.
 */
public abstract class AbstractConfirmationActivity extends Activity {
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
     * Called when the user chooses OK in the dialog.
     */
    protected abstract void _onDialogConfirm();

    /**
     * Called when the user chooses Cancel in the dialog.
     */
    protected abstract void _onDialogCancel();

    /**
     * Called when the user has closed the Alert dialog.
     *
     * @param successful successful operation?
     * @param doReturn   return to previous activity?
     * @param doRetry    try again?
     */
    protected abstract void _onDialogDone(boolean successful, boolean doReturn, boolean doRetry);

    /**
     * Called to determine the layout to use. Defaults to confirmation. Subclasses have to override it.
     */
    abstract protected int _getLayoutResource();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(_getLayoutResource());

        HeaderView headerView = (HeaderView)findViewById(R.id.headerView);
        headerView.setOnLeftClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        headerView.hideRightButton();

        TextView dn = (TextView)findViewById(R.id.display_name);
        dn.setText(_getChallenge().getIdentity().getDisplayName());

        TextView ipdn = (TextView)findViewById(R.id.identity_provider_name);
        ipdn.setText(_getChallenge().getIdentity().getIdentifier());

        final Button ok = (Button)findViewById(R.id.confirm_button);

        if (ok != null) {
            ok.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    ok.setEnabled(false);
                    _onDialogConfirm();
                }
            });
        }

        final Button cancel = (Button)findViewById(R.id.cancel_button);

        if (cancel != null) {
            cancel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(AbstractConfirmationActivity.this, MainActivity.class));
                    finish();
                }
            });
        }

        FooterView footer = (FooterView)findViewById(R.id.fallbackFooterView);

        if (footer != null) {
            footer.hideInfoIcon();
        }
    }

    /**
     * Change the title.
     *
     * @param resourceId resource
     */
    public void setTitleText(int resourceId) {
        TextView view = (TextView)findViewById(R.id.title);
        view.setText(resourceId);
    }

    /**
     * Change the description.
     *
     * @param resourceId resource
     */
    public void setDescriptionText(int resourceId) {
        TextView view = (TextView)findViewById(R.id.description);
        view.setText(resourceId);
    }

    /**
     * Change the title of the OK button.
     *
     * @param resourceId resource
     */
    public void setConfirmButtonText(int resourceId) {
        Button ok = (Button)findViewById(R.id.confirm_button);
        ok.setText(resourceId);
    }

    /**
     * Change the title of the CANCEL button.
     *
     * @param resourceId resource
     */
    public void setCancelButtonText(int resourceId) {
        Button ok = (Button)findViewById(R.id.cancel_button);
        ok.setText(resourceId);
    }

    /**
     * Returns to the challenge return URL.
     *
     * @param successful successful?
     */
    protected void _returnToChallengeUrl(boolean successful) {
        String url = _getChallenge().getReturnURL();

        if (url.indexOf("?") >= 0) {
            url = url + "&succesful=" + successful;
        } else {
            url = url + "?succesful=" + successful;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception ex) {
            _onDialogCancel();
        }
    }
}