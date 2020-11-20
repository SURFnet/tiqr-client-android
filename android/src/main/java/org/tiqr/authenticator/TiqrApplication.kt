package org.tiqr.authenticator

import android.content.Context
import android.util.Log
import androidx.multidex.MultiDexApplication
import com.google.firebase.iid.FirebaseInstanceId
import org.tiqr.authenticator.inject.ApplicationModule
import org.tiqr.authenticator.inject.DaggerTiqrComponent
import org.tiqr.authenticator.inject.TiqrComponent
import org.tiqr.service.notification.NotificationService
import javax.inject.Inject


/**
 * Tiqr Application base.
 */
class TiqrApplication : MultiDexApplication() {

    @Inject
    internal lateinit var notificationService: NotificationService

    private var component: TiqrComponent? = null

    override fun onCreate() {
        super.onCreate()
        initComponent(this)

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

    private fun initComponent(application: TiqrApplication) : TiqrComponent {
        component = DaggerTiqrComponent.builder()
                .applicationModule(ApplicationModule(this))
                .build()
                .also {
                    it.inject(this)
                }
        return component!!
    }

    companion object {

        private val TAG = TiqrApplication::class.java.simpleName

        @JvmStatic
        fun component(context: Context): TiqrComponent {
            return (context.applicationContext as TiqrApplication).let {app ->
                app.component?.let {
                    return it
                }
                // Although this should never happen, because Activity.onCreate() is guaranteed to
                // be called after Application.onCreate().
                // Still, we got crashes where the Activity tried to access the component, and it was
                // not initialized yet, so just to be safe, we add this step again.
                return app.initComponent(app)
            }
        }
    }

}
