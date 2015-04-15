/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.c2dm;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Utilities for device registration.
 *
 * Will keep track of the registration token in a private preference.
 */
public class C2DMessaging {
    public static final String EXTRA_SENDER = "sender";
    public static final String EXTRA_APPLICATION_PENDING_INTENT = "app";
    public static final String REQUEST_UNREGISTRATION_INTENT = "com.google.android.c2dm.intent.UNREGISTER";
    public static final String REQUEST_REGISTRATION_INTENT = "com.google.android.c2dm.intent.REGISTER";
    public static final String LAST_REGISTRATION_CHANGE = "last_registration_change";
    public static final String BACKOFF = "backoff";
    public static final String GSF_PACKAGE = "com.google.android.gsf";

    private static final String PROPERTY_GCM_REGISTRATION_VERSION = "gcm_registration_version";
    private static final String PROPERTY_GCM_REGISTRATION_ID = "dm_registration";

    // Increase the following number if there's an incompatible messaging API increase
    // This will lead to the client requesting a new token at the next app start.
    private static final int GCM_REGISTRATION_VERSION = 0;

    // package
    static final String PREFERENCE = "com.google.android.c2dm";
    
    private static final long DEFAULT_BACKOFF = 30000;

    /**
     * Initiate c2d messaging registration for the current application
     */
    public static void register(Context context,
            String senderId) {
        Intent registrationIntent = new Intent(REQUEST_REGISTRATION_INTENT);
        registrationIntent.setPackage(GSF_PACKAGE);
        registrationIntent.putExtra(EXTRA_APPLICATION_PENDING_INTENT,
                PendingIntent.getBroadcast(context, 0, new Intent(), 0));
        registrationIntent.putExtra(EXTRA_SENDER, senderId);
        context.startService(registrationIntent);
        // TODO: if intent not found, notification on need to have GSF
    }

    /**
     * Unregister the application. New messages will be blocked by server.
     */
    public static void unregister(Context context) {
        Intent regIntent = new Intent(REQUEST_UNREGISTRATION_INTENT);
        regIntent.setPackage(GSF_PACKAGE);
        regIntent.putExtra(EXTRA_APPLICATION_PENDING_INTENT, PendingIntent.getBroadcast(context,
                0, new Intent(), 0));
        context.startService(regIntent);
    }

    /**
     * Return the current registration id.
     *
     * If result is empty, the registration has failed.
     *
     * @return registration id, or empty string if the registration is not complete.
     */
    public static String getRegistrationId(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                PREFERENCE,
                Context.MODE_PRIVATE);
        Integer gcmVersion = prefs.getInt(PROPERTY_GCM_REGISTRATION_VERSION, -1);
        if (GCM_REGISTRATION_VERSION != gcmVersion) {
            // If the versions do not match, we can't use the existing.
            // Transition at TokenExchange will be smooth this way, because the notificationToken property
            // is stored separately. If there's a previous notification token, the server will update the device
            // token for that one, and not create a new one.
            return "";
        }
        return prefs.getString(PROPERTY_GCM_REGISTRATION_ID, "");
    }

    public static long getLastRegistrationChange(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                PREFERENCE,
                Context.MODE_PRIVATE);
        return prefs.getLong(LAST_REGISTRATION_CHANGE, 0);
    }
    
    static long getBackoff(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                PREFERENCE,
                Context.MODE_PRIVATE);
        return prefs.getLong(BACKOFF, DEFAULT_BACKOFF);
    }
    
    static void setBackoff(Context context, long backoff) {
        final SharedPreferences prefs = context.getSharedPreferences(
                PREFERENCE,
                Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putLong(BACKOFF, backoff);
        editor.commit();

    }

    // package
    static void clearRegistrationId(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(
                PREFERENCE,
                Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(PROPERTY_GCM_REGISTRATION_ID, "");
        editor.putLong(LAST_REGISTRATION_CHANGE, System.currentTimeMillis());
        editor.commit();

    }

    // package
    static void setRegistrationId(Context context, String registrationId) {
        final SharedPreferences prefs = context.getSharedPreferences(
                PREFERENCE,
                Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(PROPERTY_GCM_REGISTRATION_ID, registrationId);
        editor.putInt(PROPERTY_GCM_REGISTRATION_VERSION, GCM_REGISTRATION_VERSION);
        editor.commit();

    }
}
