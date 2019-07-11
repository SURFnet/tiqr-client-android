package org.tiqr.authenticator.security

import org.tiqr.authenticator.exceptions.InvalidChallengeException

interface OCRAProtocol {
    @Throws(InvalidChallengeException::class)
    fun generateOCRA(ocraSuite: String, key: ByteArray, challengeQuestion: String, sessionKey: String): String
}
