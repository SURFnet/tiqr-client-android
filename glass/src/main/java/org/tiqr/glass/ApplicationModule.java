package org.tiqr.glass;

import android.content.Context;

import org.tiqr.glass.authentication.AuthenticationConfirmationActivity;
import org.tiqr.glass.authentication.AuthenticationIdentitySelectActivity;
import org.tiqr.glass.enrollment.EnrollmentConfirmationActivity;
import org.tiqr.glass.scan.ScanActivity;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A module for Application-specific dependencies which require a {@link android.content.Context} or
 * {@link android.app.Application} to create.
 */
@Module(
        injects = {
                Application.class,
                ScanActivity.class,
                EnrollmentConfirmationActivity.class,
                AuthenticationIdentitySelectActivity.class,
                AuthenticationConfirmationActivity.class
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