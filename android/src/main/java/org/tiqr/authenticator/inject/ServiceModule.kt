package org.tiqr.authenticator.inject

import android.content.Context
import android.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import org.tiqr.authenticator.datamodel.DbAdapter
import org.tiqr.service.authentication.AuthenticationService
import org.tiqr.service.enrollment.EnrollmentService
import org.tiqr.service.notification.NotificationService
import javax.inject.Singleton

/**
 * Module which provides all the services.
 * Created by Daniel Zolnai on 2017-04-05.
 */
@Module
class ServiceModule {

    @Provides
    @Singleton
    internal fun provideNotificationService(context: Context) = NotificationService(context)

    @Provides
    @Singleton
    internal fun provideAuthenticationService(context: Context, notificationService: NotificationService, dbAdapter: DbAdapter) =
            AuthenticationService(context, notificationService, dbAdapter)

    @Provides
    @Singleton
    internal fun provideEnrollmentService(context: Context, notificationService: NotificationService, dbAdapter: DbAdapter) =
            EnrollmentService(context, notificationService, dbAdapter)

    @Provides
    @Singleton
    internal fun provideDbAdapter(context: Context) = DbAdapter(context)


    @Provides
    internal fun provideSharedPreferences(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

}