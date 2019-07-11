package org.tiqr.authenticator.security

import android.annotation.SuppressLint
import java.math.BigInteger

import org.tiqr.authenticator.exceptions.InvalidChallengeException
import org.tiqr.oath.OCRA_v1

/**
 * Old (flawed) implementation of an ocra wrapper using the old
 * OCRA implementation (draft 13). Used by protocol level 1 clients.
 * @author ivo
 */
@SuppressLint("DefaultLocale")
class OCRAWrapper_v1 : OCRAProtocol {
    protected fun _numStrToHex(question: String): String {

        return BigInteger(question, 10).toString(16).toUpperCase()
    }

    @Throws(InvalidChallengeException::class)
    override fun generateOCRA(ocraSuite: String, key: ByteArray, challengeQuestion: String, sessionKey: String): String {
        // The reference implementation takes session data into account even if -S isn't specified in the suite.
        // We therefor explicitly pass "" if -S is not in the suite.
        var sessionData = ""

        if (ocraSuite.toLowerCase().indexOf(":s") > 1 || ocraSuite.toLowerCase().indexOf("-s",
                        ocraSuite.indexOf(":",
                                ocraSuite.indexOf(":") + 1)) > 1) {
            sessionData = sessionKey
        }

        val challenge: String
        if (ocraSuite.toLowerCase().indexOf("qn") > 1) {
            // Using numeric challenge questions, need to convert to hex first
            challenge = _numStrToHex(challengeQuestion)
        } else {
            // if qh, we're already dealing with hex
            challenge = challengeQuestion
        }

        return OCRA_v1.generateOCRA(
                ocraSuite,
                Encryption.bytesToHexString(key),
                "",
                challenge,
                "",
                sessionData,
                "")
    }
}