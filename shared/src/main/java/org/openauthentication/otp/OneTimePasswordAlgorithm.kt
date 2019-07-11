/*
    * OneTimePasswordAlgorithm.java
    * OATH Initiative,
    * HOTP one-time password algorithm
    *
    */

/* Copyright (C) 2004, OATH.  All rights reserved.
    *
    * License to copy and use this software is granted provided that it
    * is identified as the "OATH HOTP Algorithm" in all material
    * mentioning or referencing this software or this function.
    *
    * License is also granted to make and use derivative works provided
    * that such works are identified as
    *  "derived from OATH HOTP algorithm"
    * in all material mentioning or referencing the derived work.
    *
    * OATH (Open AuTHentication) and its members make no
    * representations concerning either the merchantability of this
    * software or the suitability of this software for any particular
    * purpose.
    *
    * It is provided "as is" without express or implied warranty
    * of any kind and OATH AND ITS MEMBERS EXPRESSaLY DISCLAIMS
    * ANY WARRANTY OR LIABILITY OF ANY KIND relating to this software.
    *
    * These notices must be retained in any copies of any part of this
    * documentation and/or software.
    */

package org.openauthentication.otp

import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * This class contains static methods that are used to calculate the
 * One-Time Password (OTP) using
 * JCE to provide the HMAC-SHA-1.
 *
 * @author Loren Hart
 * @version 1.0
 */
object OneTimePasswordAlgorithm {

    // These are used to calculate the check-sum digits.
    //                                0  1  2  3  4  5  6  7  8  9
    private val doubleDigits = intArrayOf(0, 2, 4, 6, 8, 1, 3, 5, 7, 9)

    private val DIGITS_POWER = intArrayOf(1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000)

    /**
     * Calculates the checksum using the credit card algorithm.
     * This algorithm has the advantage that it detects any single
     * mistyped digit and any single transposition of
     * adjacent digits.
     *
     * @param num the number to calculate the checksum for
     * @param digits number of significant places in the number
     *
     * @return the checksum of num
     */
    fun calcChecksum(num_: Long, digits_: Int): Int {
        var num = num_
        var digits = digits_
        var doubleDigit = true
        var total = 0
        while (0 < digits--) {
            var digit = (num % 10).toInt()
            num /= 10
            if (doubleDigit) {
                digit = doubleDigits[digit]
            }
            total += digit
            doubleDigit = !doubleDigit
        }
        var result = total % 10
        if (result > 0) {
            result = 10 - result
        }
        return result
    }

    /**
     * This method uses the JCE to provide the HMAC-SHA-1
     * algorithm.
     * HMAC computes a Hashed Message Authentication Code and
     * in this case SHA1 is the hash algorithm used.
     *
     * @param keyBytes   the bytes to use for the HMAC-SHA-1 key
     * @param text       the message or text to be authenticated.
     *
     * @throws NoSuchAlgorithmException if no provider makes
     * either HmacSHA1 or HMAC-SHA-1
     * digest algorithms available.
     * @throws InvalidKeyException
     * The secret provided was not a valid HMAC-SHA-1 key.
     */

    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
    fun hmac_sha1(keyBytes: ByteArray, text: ByteArray): ByteArray {
        //        try {
        var hmacSha1: Mac
        try {
            hmacSha1 = Mac.getInstance("HmacSHA1")
        } catch (nsae: NoSuchAlgorithmException) {
            hmacSha1 = Mac.getInstance("HMAC-SHA-1")
        }

        val macKey = SecretKeySpec(keyBytes, "RAW")
        hmacSha1.init(macKey)
        return hmacSha1.doFinal(text)
        //        } catch (GeneralSecurityException gse) {
        //            throw new UndeclaredThrowableException(gse);
        //        }
    }
    // 0 1  2   3    4     5      6       7        8

    /**
     * This method generates an OTP value for the given
     * set of parameters.
     *
     * @param secret       the shared secret
     * @param movingFactor the counter, time, or other value that
     * changes on a per use basis.
     * @param codeDigits   the number of digits in the OTP, not
     * including the checksum, if any.
     * @param addChecksum  a flag that indicates if a checksum digit
     * *                     should be appended to the OTP.
     * @param truncationOffset the offset into the MAC result to
     * begin truncation.  If this value is out of
     * the range of 0 ... 15, then dynamic
     * truncation  will be used.
     * Dynamic truncation is when the last 4
     * bits of the last byte of the MAC are
     * used to determine the start offset.
     * @throws NoSuchAlgorithmException if no provider makes
     * either HmacSHA1 or HMAC-SHA-1
     * digest algorithms available.
     * @throws InvalidKeyException
     * The secret provided was not
     * a valid HMAC-SHA-1 key.
     *
     * @return A numeric String in base 10 that includes
     * [codeDigits] digits plus the optional checksum
     * digit if requested.
     */
    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
    fun generateOTP(secret: ByteArray, movingFactor_: Long, codeDigits: Int, addChecksum: Boolean, truncationOffset: Int): String {
        var movingFactor = movingFactor_
        var result: String?
        val digits = if (addChecksum) codeDigits + 1 else codeDigits

        val text = ByteArray(8)
        for (i in text.indices.reversed()) {
            text[i] = (movingFactor and 0xff).toByte()
            movingFactor = movingFactor shr 8
        }

        // compute hmac hash
        val hash = hmac_sha1(secret, text)

        // put selected bytes into result int
        var offset = hash[hash.size - 1].toInt() and 0xf

        if (0 <= truncationOffset && truncationOffset < hash.size - 4) {
            offset = truncationOffset
        }

        val binary = (hash[offset].toInt() and 0x7f shl 24
                or (hash[offset + 1].toInt() and 0xff shl 16)
                or (hash[offset + 2].toInt() and 0xff shl 8)
                or (hash[offset + 3].toInt() and 0xff))

        var otp = binary % DIGITS_POWER[codeDigits]
        if (addChecksum) {
            otp = otp * 10 + calcChecksum(otp.toLong(), codeDigits)
        }
        result = Integer.toString(otp)
        while (result!!.length < digits) {
            result = "0$result"
        }
        return result
    }
}
