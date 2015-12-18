package org.tiqr.authenticator;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.c2dm.C2DMBaseReceiver;

import org.tiqr.service.notification.NotificationService;
import org.tiqr.service.notification.Prefs;

import javax.inject.Inject;

/**
 * Android Cloud to Device Messaging receiver.
 */
public class C2DMReceiver extends C2DMBaseReceiver {
    public static final String SENDER_ID = "70645846943";

    protected
    @Inject
    NotificationService _notificationService;

    /**
     * Constructor.
     */
    public C2DMReceiver() {
        super(SENDER_ID);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((Application)getApplication()).inject(this);
    }

    @Override
    public void onRegistered(Context context, String deviceToken) {
        Log.d(getClass().getSimpleName(), "Registered device for C2DM, token: " + deviceToken);

        SharedPreferences settings = Prefs.get(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("deviceToken", deviceToken);
        editor.commit();

        _notificationService.sendRequestWithDeviceToken(deviceToken);
    }

    @Override
    public void onUnregistered(Context context) {
        SharedPreferences settings = Prefs.get(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove("deviceToken");
        editor.commit();
    }

    @Override
    public void onError(Context context, String error) {
        Log.d(getClass().getSimpleName(), "Error registering device for C2DM, error: " + error);
    }

    @Override
    public void onMessage(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }

        String challenge = (String)extras.get("challenge");
        String title = context.getString(R.string.app_name);
        String text = (String)extras.get("text");

        if (!TextUtils.isEmpty(challenge)) {
            Intent authIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(challenge));
            authIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            if (context.getPackageManager().queryIntentActivities(authIntent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0) {

                int icon = R.drawable.icon_notification;
                long when = System.currentTimeMillis();

                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, authIntent, 0);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                Notification notification = builder.setContentIntent(pendingIntent)
                        .setSmallIcon(icon).setTicker(text).setWhen(when)
                        .setAutoCancel(true).setContentTitle(title)
                        .setContentText(text).build();

                NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(0, notification);
            }
        }
    }
}