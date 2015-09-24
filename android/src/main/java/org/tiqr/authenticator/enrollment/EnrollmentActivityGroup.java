package org.tiqr.authenticator.enrollment;

import android.content.Intent;
import android.os.Bundle;

import org.tiqr.authenticator.auth.EnrollmentChallenge;
import org.tiqr.authenticator.general.AbstractActivityGroup;

public class EnrollmentActivityGroup extends AbstractActivityGroup {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EnrollmentChallenge challenge = getIntent().getParcelableExtra("org.tiqr.challenge");
        setChallenge(challenge);

        Intent intent = new Intent().setClass(this, EnrollmentConfirmationActivity.class);
        startChildActivity("EnrollmentConfirmationActivity", intent);
    }
}
