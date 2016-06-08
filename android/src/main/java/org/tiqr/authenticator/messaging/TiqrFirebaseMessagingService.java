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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.tiqr.authenticator.Application;
import org.tiqr.authenticator.R;

import java.util.Map;

import javax.inject.Inject;

/**
 * Our custom FCM messaging service.
 * Created by Daniel Zolnai on 2016-06-07.
 */
public class TiqrFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = TiqrFirebaseMessagingService.class.getName();

    @Inject
    protected Context _context;

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application)getApplication()).inject(this);
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

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                Notification notification = builder.setContentIntent(pendingIntent)
                        .setSmallIcon(icon).setTicker(text).setWhen(when)
                        .setAutoCancel(true).setContentTitle(title)
                        .setContentText(text).build();

                NotificationManager manager = (NotificationManager)_context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(0, notification);
            }
        }
    }
}