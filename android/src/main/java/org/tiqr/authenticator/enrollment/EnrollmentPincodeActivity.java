package org.tiqr.authenticator.enrollment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.tiqr.authenticator.R;
import org.tiqr.authenticator.general.AbstractActivityGroup;
import org.tiqr.authenticator.general.AbstractPincodeActivity;

public class EnrollmentPincodeActivity extends AbstractPincodeActivity {

    @Override
    public void process() {
        _hideSoftKeyboard(pincode);

        AbstractActivityGroup parent = (AbstractActivityGroup)getParent();
        Intent intent = new Intent().setClass(this, EnrollmentPincodeVerificationActivity.class);
        intent.putExtra("org.tiqr.firstPin", pincode.getText().toString());
        parent.startChildActivity("EnrollmentPincodeVerificationActivity", intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Update the text.
        title.setText(R.string.entroll_pin_title);
        setIntoText(R.string.login_intro);
    }
}