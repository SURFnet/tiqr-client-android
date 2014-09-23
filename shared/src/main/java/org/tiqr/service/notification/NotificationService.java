package org.tiqr.service.notification;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import javax.inject.Inject;

public class NotificationService
{
    protected @Inject Context _context;

    public static final String TOKENEXCHANGE_URL = "https://mobi.surfnet.nl/tokenexchange/?appId=tiqr";

    private void _sendRequestWithDeviceToken(final String deviceToken) throws Exception
    {
        String notificationToken = getNotificationToken();

        HttpPost httpPost = new HttpPost(TOKENEXCHANGE_URL);
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("deviceToken", deviceToken));
        if (notificationToken != null) {
            nameValuePairs.add(new BasicNameValuePair("notificationToken", notificationToken));
        }

        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpResponse httpResponse;
        httpResponse = httpClient.execute(httpPost);

        notificationToken = EntityUtils.toString(httpResponse.getEntity());
        Log.d(NotificationService.class.getSimpleName(), "Notification token: " + notificationToken);

        SharedPreferences settings = Prefs.get(_context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("sa_notificationToken", notificationToken);
        editor.commit();
    }
    
    public String getNotificationToken()
    {
        SharedPreferences settings = Prefs.get(_context);
        return settings.getString("sa_notificationToken", null);
    }

    public void sendRequestWithDeviceToken(final String deviceToken)
    {
        new Thread(new Runnable() {
            public void run()
            {
                try {
                    _sendRequestWithDeviceToken(deviceToken);
                } catch (Exception ex) {
                    Log.e(NotificationService.class.getSimpleName(), "Error retrieving device notification token", ex);
                }
            }
        }).start();
    }
}