package org.tiqr.authenticator.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import org.tiqr.authenticator.R;

public class IncompatibilityDialog {
    public void show(final Activity activity) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
        dialog.setMessage(activity.getString(R.string.error_device_incompatible_with_security_standards));
        dialog.setCancelable(false);
        dialog.setPositiveButton(activity.getString(R.string.ok_button), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                activity.finish();
            }
        });

        AlertDialog alert = dialog.create();
        alert.show();
    }
}
