package org.tiqr.authenticator.security

import org.tiqr.oath.OCRA
import org.tiqr.authenticator.exceptions.InvalidChallengeException

class OCRAWrapper : OCRAProtocol {
    @Throws(InvalidChallengeException::class)
    override fun generateOCRA(ocraSuite: String, key: ByteArray, challengeQuestion: String, sessionKey: String): String {
        val otp: String

        try {
            otp = OCRA.generateOCRA(
                    ocraSuite,
                    Encryption.bytesToHexString(key),
                    "",
                    challengeQuestion,
                    "",
                    sessionKey,
                    "")
        } catch (e: Exception) {
            throw InvalidChallengeException()
        }

        return otp
    }
}
