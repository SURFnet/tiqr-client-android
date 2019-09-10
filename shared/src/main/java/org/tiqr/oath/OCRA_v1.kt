package org.tiqr.oath

import java.lang.reflect.UndeclaredThrowableException
import java.security.GeneralSecurityException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import org.tiqr.authenticator.exceptions.InvalidChallengeException

import java.math.BigInteger
import kotlin.experimental.and


/**
 * This an example implementation of the OATH OCRA algorithm.
 * Visit www.openauthentication.org for more information.
 *
 * @author Johan Rydell, PortWise
 */
object OCRA_v1 {


    private val DIGITS_POWER = intArrayOf(1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000)// 0 1  2   3    4     5      6       7        8

    /**
     * This method uses the JCE to provide the crypto
     * algorithm.
     * HMAC computes a Hashed Message Authentication Code with the
     * crypto hash algorithm as a parameter.
     *
     * @param crypto     the crypto algorithm (HmacSHA1,
     * HmacSHA256,
     * HmacSHA512)
     * @param keyBytes   the bytes to use for the HMAC key
     * @param text       the message or text to be authenticated.
     */

    private fun hmac_sha1(crypto: String,
                          keyBytes: ByteArray,
                          text: ByteArray): ByteArray {
        try {
            val hmac: Mac
            hmac = Mac.getInstance(crypto)
            val macKey = SecretKeySpec(keyBytes, "RAW")
            hmac.init(macKey)
            return hmac.doFinal(text)
        } catch (gse: GeneralSecurityException) {
            throw UndeclaredThrowableException(gse)
        }

    }

    /**
     * This method converts HEX string to Byte[]
     *
     * @param hex   the HEX string
     *
     * @return      A byte array
     */

    private fun hexStr2Bytes(hex: String): ByteArray {
        // Adding one byte to get the right conversion
        // values starting with "0" can be converted
        val bArray = BigInteger("10$hex", 16).toByteArray()

        // Copy all the REAL bytes, not the "first"
        val ret = ByteArray(bArray.size - 1)
        for (i in ret.indices)
            ret[i] = bArray[i + 1]
        return ret
    }


    /**
     * This method generates an OCRA HOTP value for the given
     * set of parameters.
     *
     * @param ocraSuite    the OCRA Suite
     * @param key          the shared secret, HEX encoded
     * @param counter      the counter that changes
     * on a per use basis,
     * HEX encoded
     * @param question     the challenge question, HEX encoded
     * @param password     a password that can be used,
     * HEX encoded
     * @param sessionInformation
     * Static information that identifies the
     * current session, Hex encoded
     * @param timeStamp    a value that reflects a time
     *
     * @return A numeric String in base 10 that includes
     * [truncationDigits] digits
     * @throws InvalidChallengeException
     */
    @Throws(InvalidChallengeException::class)
    fun generateOCRA(ocraSuite: String,
                     key: String,
                     counter_: String,
                     question_: String,
                     password_: String,
                     sessionInformation_: String,
                     timeStamp_: String): String {

        var counter = counter_
        var question = question_
        var password = password_
        var sessionInformation = sessionInformation_
        var timeStamp = timeStamp_
        var codeDigits: Int
        var crypto: String
        var result: String?
        val ocraSuiteLength = ocraSuite.toByteArray().size
        var counterLength = 0
        var questionLength = 0
        var passwordLength = 0

        var sessionInformationLength = 0
        var timeStampLength = 0

        if (ocraSuite.toLowerCase().indexOf("sha1") > 1)
            crypto = "HmacSHA1"
        if (ocraSuite.toLowerCase().indexOf("sha256") > 1)
            crypto = "HmacSHA256"
        if (ocraSuite.toLowerCase().indexOf("sha512") > 1)
            crypto = "HmacSHA512"
        else {
            crypto = "HmacSHA1"
        }

        // How many digits should we return
        val oS = ocraSuite.substring(ocraSuite.indexOf(":"),
                ocraSuite.indexOf(":", ocraSuite.indexOf(":") + 1))
        codeDigits = Integer.decode(oS.substring(oS.lastIndexOf("-") + 1,
                oS.length))

        // The size of the byte array message to be encrypted
        // Counter
        if (ocraSuite.toLowerCase().indexOf(":c") > 1) {
            // Fix the length of the HEX string
            while (counter.length < 16)
                counter = "0$counter"
            counterLength = 8
        }

        // Question length can't exceed 254 chars
        if (question.length > 254) {
            throw InvalidChallengeException()
        }

        // Question
        if (ocraSuite.toLowerCase().indexOf(":q") > 1 || ocraSuite.toLowerCase().indexOf("-q") > 1) {
            while (question.length < 256)
                question = question + "0"
            questionLength = 128
        }

        // Password
        if (ocraSuite.toLowerCase().indexOf(":p") > 1 || ocraSuite.toLowerCase().indexOf("-p") > 1) {
            while (password.length < 40)
                password = "0$password"
            passwordLength = 20
        }

        // sessionInformation
        if (ocraSuite.toLowerCase().indexOf(":s") > 1 || ocraSuite.toLowerCase().indexOf("-s",
                        ocraSuite.indexOf(":",
                                ocraSuite.indexOf(":") + 1)) > 1) {
            while (sessionInformation.length < 128)
                sessionInformation = "0$sessionInformation"

            sessionInformationLength = 64
        }
        // TimeStamp
        if (ocraSuite.toLowerCase().indexOf(":t") > 1 || ocraSuite.toLowerCase().indexOf("-t") > 1) {
            while (timeStamp.length < 16)
                timeStamp = "0$timeStamp"
            timeStampLength = 8
        }

        // Remember to add "1" for the "00" byte delimiter
        val msg = ByteArray(ocraSuiteLength +
                counterLength +
                questionLength +
                passwordLength +
                sessionInformationLength +
                timeStampLength +
                1)


        // Put the bytes of "ocraSuite" parameters into the message
        var bArray = ocraSuite.toByteArray()
        for (i in bArray.indices) {
            msg[i] = bArray[i]
        }

        // Delimiter
        msg[bArray.size] = 0x00

        // Put the bytes of "Counter" to the message
        // Input is HEX encoded
        if (counterLength > 0) {
            bArray = hexStr2Bytes(counter)
            for (i in bArray.indices) {
                msg[i + ocraSuiteLength + 1] = bArray[i]
            }
        }


        // Put the bytes of "question" to the message
        // Input is text encoded
        if (question.length > 0) {
            bArray = hexStr2Bytes(question)
            for (i in bArray.indices) {
                msg[i + ocraSuiteLength + 1 + counterLength] = bArray[i]
            }
        }

        // Put the bytes of "password" to the message
        // Input is HEX encoded
        if (password.length > 0) {
            bArray = hexStr2Bytes(password)
            for (i in bArray.indices) {
                msg[i + ocraSuiteLength + 1 + counterLength
                        + questionLength] = bArray[i]
            }
        }

        // Put the bytes of "sessionInformation" to the message
        // Input is text encoded
        if (sessionInformation.length > 0) {
            bArray = hexStr2Bytes(sessionInformation)
            var i = 0
            while (i < 128 && i < bArray.size) {
                msg[i + ocraSuiteLength
                        + 1 + counterLength
                        + questionLength
                        + passwordLength] = bArray[i]
                i++
            }
        }

        // Put the bytes of "time" to the message
        // Input is text value of minutes
        if (timeStamp.length > 0) {
            bArray = hexStr2Bytes(timeStamp)
            var i = 0
            while (i < 8 && i < bArray.size) {
                msg[i + ocraSuiteLength + 1 + counterLength +
                        questionLength + passwordLength +
                        sessionInformationLength] = bArray[i]
                i++
            }
        }

        val hash: ByteArray
        bArray = hexStr2Bytes(key)

        hash = hmac_sha1(crypto, bArray, msg)

        // put selected bytes into result int
        val offset = hash[hash.size - 1].toInt() and 0xf

        val binary =
                hash[offset].and(127).toInt().shl(24)
                        .or(hash[offset + 1].toInt().and(255).toInt().shl(16))
                        .or(hash[offset + 2].toInt().and(255).toInt().shl(8))
                        .or(hash[offset + 3].toInt().and(255).toInt())

        val otp = binary % DIGITS_POWER[codeDigits]
        result = Integer.toString(otp)
        while (result!!.length < codeDigits) {
            result = "0$result"
        }
        return result
    }

    @Throws(Exception::class)
    fun getHexString(b: ByteArray): String {
        var result = ""
        for (i in b.indices) {
            result += Integer.toString((b[i].toInt() and 0xff) + 0x100, 16).substring(1)
        }
        return result
    }
}
