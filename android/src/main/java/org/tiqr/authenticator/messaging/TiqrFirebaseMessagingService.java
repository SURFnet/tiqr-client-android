/**
 * Based on: https://github.com/firebase/quickstart-android/blob/master/messaging/app/src/main/java/com/google/firebase/quickstart/fcm/MyFirebaseMessagingService.java
 * Original license:
 * <p/>
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tiqr.authenticator.messaging;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.tiqr.authenticator.TiqrApplication;
import org.tiqr.authenticator.R;
import org.tiqr.service.notification.NotificationService;

import java.util.Map;

import javax.inject.Inject;

/**
 * Our custom FCM messaging service.
 * Created by Daniel Zolnai on 2016-06-07.
 */
public class TiqrFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = TiqrFirebaseMessagingService.class.getName();
    private static final String CHANNEL_ID = "default";

    @Inject
    protected Context _context;

    @Inject
    protected NotificationService _notificationService;


    @Override
    public void onCreate() {
        super.onCreate();
        TiqrApplication.Companion.component().inject(this);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        _notificationService.sendRequestWithDeviceToken(token);
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See _sendNotification method below.
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        for (Map.Entry<String, String> entry : remoteMessage.getData().entrySet()) {
            Log.e("MAP", entry.getKey() + ": " + entry.getValue());
        }
        _sendNotification(remoteMessage);
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param remoteMessage The message received via FCM.
     */
    private void _sendNotification(RemoteMessage remoteMessage) {
        if (remoteMessage == null || remoteMessage.getData() == null) {
            Log.w(TAG, "Unexpected Firebase message format found, discarding.");
            return;
        }
        String text = remoteMessage.getData().get("text");
        String challenge = remoteMessage.getData().get("challenge");

        String title = _context.getString(R.string.app_name);

        if (!TextUtils.isEmpty(challenge)) {
            Intent authIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(challenge));
            authIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            if (_context.getPackageManager().queryIntentActivities(authIntent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0) {

                int icon = R.drawable.icon_notification;
                long when = System.currentTimeMillis();

                PendingIntent pendingIntent = PendingIntent.getActivity(_context, 0, authIntent, 0);
                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                NotificationManager notificationManager = (NotificationManager)_context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(_context, CHANNEL_ID);
                    Notification notification = builder.setContentIntent(pendingIntent)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon_notification_big))
                            .setColor(ContextCompat.getColor(this, R.color.notification_icon_color))
                            .setTimeoutAfter(getResources().getInteger(R.integer.notification_timeout_ms))
                            .setSound(soundUri)
                            .setSmallIcon(icon)
                            .setAutoCancel(true)
                            .setWhen(when)
                            .setContentTitle(title)
                            .setTicker(text)
                            .setContentText(text)
                            .build();

                    _initNotificationChannel(notificationManager);

                    notificationManager.notify(0, notification);
                }
            }
        }
    }

    private void _initNotificationChannel(@NonNull NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, _context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setDescription(_context.getString(R.string.notification_channel_description));
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(true);
        notificationManager.createNotificationChannel(notificationChannel);
    }
}
