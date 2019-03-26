package org.tiqr;

import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.tiqr.authenticator.exceptions.SecurityFeaturesException;
import org.tiqr.authenticator.security.Encryption;

import androidx.test.filters.SmallTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static org.junit.Assert.*;

@SmallTest
@RunWith(AndroidJUnit4ClassRunner.class)
public class UtilsTest {

    @Test
    public void byteArrayToCharArray() throws SecurityFeaturesException {
        // This test is only for old versions of Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return;
        }
        for (int i =0; i < 100_000; ++i) {
            byte[] randomBytes = Encryption.getRandomBytes(20);
            String utfString = new String(randomBytes);
            assertArrayEquals(Utils.byteArrayToCharArray(randomBytes), utfString.toCharArray());
        }
    }
}