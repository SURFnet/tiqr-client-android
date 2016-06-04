package org.tiqr;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Common class for sharing utility methods.
 * Created by Daniel Zolnai on 2016-06-04.
 */
public class Utils {

    private static final String TAG = Utils.class.getName();

    /**
     * Retrieves the response of an URLConnection and converts it to a string.
     *
     * @param connection The URL connection to get the response of.
     * @return The response as a string.
     */
    public static String urlConnectionResponseAsString(URLConnection connection) {
        String result = null;
        StringBuilder stringBuilder = new StringBuilder();
        InputStream inputStream = null;

        try {
            inputStream = new BufferedInputStream(connection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                stringBuilder.append(inputLine);
            }
            result = stringBuilder.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error reading InputStream", e);
            result = null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.i(TAG, "Error closing InputStream", e);
                }
            }
        }
        return result;
    }

    /**
     * Converts a key value map to a byte array.
     * Can be used for POST data which should be sent in URL form encoded way.
     * @param keyValueMap The input key-value map.
     * @return The output byte array.
     */
    public static byte[] keyValueMapToByteArray(Map<String, String> keyValueMap) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            for (Map.Entry<String, String> param : keyValueMap.entrySet()) {
                if (stringBuilder.length() != 0) stringBuilder.append('&');
                stringBuilder.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                stringBuilder.append('=');
                stringBuilder.append(URLEncoder.encode(param.getValue(), "UTF-8"));
            }
            return stringBuilder.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding is not supported!", e);
        }
    }
}
