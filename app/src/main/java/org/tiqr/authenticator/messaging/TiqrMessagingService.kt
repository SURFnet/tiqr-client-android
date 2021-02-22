/*
 * Copyright (c) 2010-2021 SURFnet bv
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of SURFnet bv nor the names of its contributors
 *    may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.tiqr.authenticator.messaging

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.tiqr.authenticator.R
import org.tiqr.data.repository.TokenRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Service to receive token (updates) and push notifications
 */
@AndroidEntryPoint
class TiqrMessagingService : FirebaseMessagingService() {
    companion object {
        private const val MESSAGE_TEXT = "text"
        private const val MESSAGE_CHALLENGE = "challenge"
        private const val CHANNEL_ID = "default"
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    @Inject
    internal lateinit var repository: TokenRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Timber.i("Received token: $token")

        scope.launch {
            repository.registerDeviceToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        sendNotification(message)
    }

    /**
     * Send the notification to the app to enroll or authenticate
     */
    private fun sendNotification(message: RemoteMessage) {
        val title = getString(R.string.app_name)
        val text = message.data[MESSAGE_TEXT]
        val challenge = message.data[MESSAGE_CHALLENGE]

        if (!challenge.isNullOrEmpty()) {
            val notificationManager = NotificationManagerCompat.from(this)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(
                        NotificationChannel(CHANNEL_ID,
                                resources.getString(R.string.notification_channel_name),
                                NotificationManager.IMPORTANCE_DEFAULT).also {
                            it.description = resources.getString(R.string.notification_channel_description)
                        }
                )
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(challenge)).also { intent ->
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentIntent(PendingIntent.getActivity(this, 0, intent, 0))
                    .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_notification_large))
                    .setSmallIcon(R.drawable.ic_notification)
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    .setTimeoutAfter(180_000) // 3 minutes
                    .setContentTitle(title)
                    .setTicker(text)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setDefaults(Notification.DEFAULT_ALL)
                    .build()
                    .apply {
                        notificationManager.notify(0, this)
                    }
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }
}