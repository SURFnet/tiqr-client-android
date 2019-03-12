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

    private static final char REPLACEMENT_CHAR = (char) 0xfffd;


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

    /**
     * This code was copied from the original Java implementation for parsing byte arrays into char
     * arrays, required to parse an UTF-8 stream of bytes into a meaningful String. This implementation changed
     * with Android 9, which made our app break, which depended on it. For backwards compatibility reasons,
     * I have taken the old parsing code, and inserted here.
     * @param data
     * @return
     */
    public static char[] byteArrayToCharArray(byte[] data) {
        char[] value;
        final int offset = 0;
        final int byteCount = data.length;

        char[] v = new char[byteCount];
        int idx = offset;
        int last = offset + byteCount;
        int s = 0;
        outer:
        while (idx < last) {
            byte b0 = data[idx++];
            if ((b0 & 0x80) == 0) {
                // 0xxxxxxx
                // Range:  U-00000000 - U-0000007F
                int val = b0 & 0xff;
                v[s++] = (char) val;
            } else if (((b0 & 0xe0) == 0xc0) || ((b0 & 0xf0) == 0xe0) ||
                    ((b0 & 0xf8) == 0xf0) || ((b0 & 0xfc) == 0xf8) || ((b0 & 0xfe) == 0xfc)) {
                int utfCount = 1;
                if ((b0 & 0xf0) == 0xe0) utfCount = 2;
                else if ((b0 & 0xf8) == 0xf0) utfCount = 3;
                else if ((b0 & 0xfc) == 0xf8) utfCount = 4;
                else if ((b0 & 0xfe) == 0xfc) utfCount = 5;
                // 110xxxxx (10xxxxxx)+
                // Range:  U-00000080 - U-000007FF (count == 1)
                // Range:  U-00000800 - U-0000FFFF (count == 2)
                // Range:  U-00010000 - U-001FFFFF (count == 3)
                // Range:  U-00200000 - U-03FFFFFF (count == 4)
                // Range:  U-04000000 - U-7FFFFFFF (count == 5)
                if (idx + utfCount > last) {
                    v[s++] = REPLACEMENT_CHAR;
                    continue;
                }
                // Extract usable bits from b0
                int val = b0 & (0x1f >> (utfCount - 1));
                for (int i = 0; i < utfCount; ++i) {
                    byte b = data[idx++];
                    if ((b & 0xc0) != 0x80) {
                        v[s++] = REPLACEMENT_CHAR;
                        idx--; // Put the input char back
                        continue outer;
                    }
                    // Push new bits in from the right side
                    val <<= 6;
                    val |= b & 0x3f;
                }
                // Note: Java allows overlong char
                // specifications To disallow, check that val
                // is greater than or equal to the minimum
                // value for each count:
                //
                // count    min value
                // -----   ----------
                //   1           0x80
                //   2          0x800
                //   3        0x10000
                //   4       0x200000
                //   5      0x4000000
                // Allow surrogate values (0xD800 - 0xDFFF) to
                // be specified using 3-byte UTF values only
                if ((utfCount != 2) && (val >= 0xD800) && (val <= 0xDFFF)) {
                    v[s++] = REPLACEMENT_CHAR;
                    continue;
                }
                // Reject chars greater than the Unicode maximum of U+10FFFF.
                if (val > 0x10FFFF) {
                    v[s++] = REPLACEMENT_CHAR;
                    continue;
                }
                // Encode chars from U+10000 up as surrogate pairs
                if (val < 0x10000) {
                    v[s++] = (char) val;
                } else {
                    int x = val & 0xffff;
                    int u = (val >> 16) & 0x1f;
                    int w = (u - 1) & 0xffff;
                    int hi = 0xd800 | (w << 6) | (x >> 10);
                    int lo = 0xdc00 | (x & 0x3ff);
                    v[s++] = (char) hi;
                    v[s++] = (char) lo;
                }
            } else {
                // Illegal values 0x8*, 0x9*, 0xa*, 0xb*, 0xfd-0xff
                v[s++] = REPLACEMENT_CHAR;
            }
        }
        if (s == byteCount) {
            // We guessed right, so we can use our temporary array as-is.
            value = v;
        } else {
            // Our temporary array was too big, so reallocate and copy.
            value = new char[s];
            System.arraycopy(v, 0, value, 0, s);
        }
        return value;
    }
}
