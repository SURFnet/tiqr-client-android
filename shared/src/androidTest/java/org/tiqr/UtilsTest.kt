package org.tiqr

import android.os.Build

import org.junit.Test
import org.junit.runner.RunWith
import org.tiqr.authenticator.exceptions.SecurityFeaturesException
import org.tiqr.authenticator.security.Encryption
import androidx.test.filters.SmallTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner

import org.junit.Assert.*


@SmallTest
@RunWith(AndroidJUnit4ClassRunner::class)
class UtilsTest {

    @Test
    @Throws(SecurityFeaturesException::class)
    fun byteArrayToCharArray() {
        // This test is only for old versions of Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return
        }
        for (i in 0..99999) {
            val randomBytes = Encryption.getRandomBytes(20)
            val utfString = String(randomBytes)
            assertArrayEquals(Utils.byteArrayToCharArray(randomBytes), utfString.toCharArray())
        }
    }
}