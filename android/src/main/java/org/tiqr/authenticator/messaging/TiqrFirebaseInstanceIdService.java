/**
 * Based on: https://github.com/firebase/quickstart-android/blob/master/messaging/app/src/main/java/com/google/firebase/quickstart/fcm/MyFirebaseInstanceIDService.java
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

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.tiqr.authenticator.Application;
import org.tiqr.service.notification.NotificationService;

import javax.inject.Inject;

/**
 * The Firebase instance ID service
 * Created by Daniel Zolnai on 2016-06-07.
 */
public class TiqrFirebaseInstanceIdService extends FirebaseInstanceIdService {

    private static final String TAG = TiqrFirebaseInstanceIdService.class.getName();

    @Inject
    protected NotificationService _notificationService;

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application)getApplication()).inject(this);
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);
        _notificationService.sendRequestWithDeviceToken(refreshedToken);
    }
    // [END refresh_token]
}

