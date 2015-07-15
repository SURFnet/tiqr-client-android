package org.tiqr.authenticator.enrollment;

import org.tiqr.authenticator.R;
import org.tiqr.authenticator.general.AbstractActivityGroup;
import org.tiqr.authenticator.general.AbstractPincodeActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class EnrollmentPincodeActivity extends AbstractPincodeActivity {

	public void process(View v) {
		_hideSoftKeyboard(pincode);
		
    	AbstractActivityGroup parent = (AbstractActivityGroup) getParent();
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