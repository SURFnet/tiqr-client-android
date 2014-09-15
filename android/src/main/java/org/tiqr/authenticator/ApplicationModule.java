package org.tiqr.authenticator;

import android.content.Context;

import org.tiqr.authenticator.authentication.AuthenticationConfirmationActivity;
import org.tiqr.authenticator.authentication.AuthenticationPincodeActivity;
import org.tiqr.authenticator.enrollment.EnrollmentPincodeVerificationActivity;
import org.tiqr.authenticator.qr.CaptureActivity;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A module for Application-specific dependencies which require a {@link Context} or
 * {@link android.app.Application} to create.
 */
@Module(
        injects = {
                Application.class,
                MainActivity.class,
                AuthenticationConfirmationActivity.class,
                AuthenticationPincodeActivity.class,
                EnrollmentPincodeVerificationActivity.class
        }
)
public class ApplicationModule {
    private final Application _application;

    /**
     * Constructor.
     *
     * @param application
     */
    public ApplicationModule(Application application) {
        _application = application;
    }

    /**
     * Allow the application context to be injected.
     */
    @Provides @Singleton Context provideContext() {
        return _application.getApplicationContext();
    }
}