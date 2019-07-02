package org.tiqr.authenticator.inject

import dagger.Component
import org.tiqr.authenticator.MainActivity
import org.tiqr.authenticator.TiqrApplication
import org.tiqr.authenticator.authentication.AuthenticationConfirmationActivity
import org.tiqr.authenticator.authentication.AuthenticationFingerprintActivity
import org.tiqr.authenticator.authentication.AuthenticationPincodeActivity
import org.tiqr.authenticator.authentication.AuthenticationSummaryActivity
import org.tiqr.authenticator.enrollment.EnrollmentPincodeVerificationActivity
import org.tiqr.authenticator.identity.IdentityDetailActivity
import org.tiqr.authenticator.messaging.TiqrFirebaseMessagingService
import org.tiqr.authenticator.qr.CaptureActivity
import javax.inject.Singleton

@Component(modules = [ApplicationModule::class])
@Singleton
interface TiqrComponent {
    fun inject(authenticationSummaryActivity: AuthenticationSummaryActivity)
    fun inject(authenticationFingerprintActivity: AuthenticationFingerprintActivity)
    fun inject(authenticationPincodeActivity: AuthenticationPincodeActivity)
    fun inject(authenticationConfirmationActivity: AuthenticationConfirmationActivity)
    fun inject(tiqrFirebaseMessagingService: TiqrFirebaseMessagingService)
    fun inject(enrollmentPincodeVerificationActivity: EnrollmentPincodeVerificationActivity)
    fun inject(mainActivity: MainActivity)
    fun inject(identityDetailActivity: IdentityDetailActivity)
    fun inject(captureActivity: CaptureActivity)
    fun inject(tiqrApplication: TiqrApplication)
}