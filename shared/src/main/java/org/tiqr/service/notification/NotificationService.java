package org.tiqr.service.notification;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.tiqr.Utils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class NotificationService {
    protected
    @Inject
    Context _context;

    public static final String TOKENEXCHANGE_URL = "https://mobi.surfnet.nl/tokenexchange/?appId=tiqr";

    private void _sendRequestWithDeviceToken(final String deviceToken) throws Exception {
        String notificationToken = getNotificationToken();

        Map<String, String> nameValuePairs = new HashMap<>();
        nameValuePairs.put("deviceToken", deviceToken);
        if (notificationToken != null) {
            nameValuePairs.put("notificationToken", notificationToken);
        }

        URL tokenExchangeURL = new URL(TOKENEXCHANGE_URL);
        HttpURLConnection httpURLConnection = (HttpURLConnection) tokenExchangeURL.openConnection();
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setDoOutput(true);
        byte[] postData = Utils.keyValueMapToByteArray(nameValuePairs);
        httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        httpURLConnection.setRequestProperty("Content-Length", String.valueOf(postData.length));
        httpURLConnection.getOutputStream().write(postData);

        notificationToken = Utils.urlConnectionResponseAsString(httpURLConnection);

        Log.d(NotificationService.class.getSimpleName(), "Notification token: " + notificationToken);

        SharedPreferences settings = Prefs.get(_context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("sa_notificationToken", notificationToken);
        editor.commit();
    }

    public String getNotificationToken() {
        SharedPreferences settings = Prefs.get(_context);
        return settings.getString("sa_notificationToken", null);
    }

    public void sendRequestWithDeviceToken(final String deviceToken) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    _sendRequestWithDeviceToken(deviceToken);
                } catch (Exception ex) {
                    Log.e(NotificationService.class.getSimpleName(), "Error retrieving device notification token", ex);
                }
            }
        }).start();
    }
}