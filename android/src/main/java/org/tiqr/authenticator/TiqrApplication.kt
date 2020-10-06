package org.tiqr.authenticator

import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId
import org.tiqr.authenticator.inject.ApplicationModule
import org.tiqr.authenticator.inject.DaggerTiqrComponent
import org.tiqr.authenticator.inject.TiqrComponent
import org.tiqr.service.notification.NotificationService
import javax.inject.Inject


/**
 * Tiqr Application base.
 */
class TiqrApplication : android.app.Application() {

    @Inject
    internal lateinit var notificationService: NotificationService

    override fun onCreate() {
        super.onCreate()
        component = DaggerTiqrComponent.builder()
                .applicationModule(ApplicationModule(this))
                .build()
                .also {
                    it.inject(this)
                }

        Thread {
            try {
                val token = FirebaseInstanceId.getInstance().getToken(getString(R.string.gcm_defaultSenderId), "FCM")
                if (!token.isNullOrEmpty()) {
                    notificationService.requestNewToken(token)
                }
            } catch (ex: Exception) {
                Log.w(TAG, "Unable to determine Firebase token of the user", ex)
            }
        }.start()
    }

    companion object {
        private lateinit var component: TiqrComponent

        private val TAG = TiqrApplication::class.simpleName

        @JvmStatic
        fun component(): TiqrComponent {
            return component
        }
    }

}
