package org.tiqr.authenticator.security

import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException

import org.tiqr.authenticator.exceptions.SecurityFeaturesException

import org.openauthentication.otp.OneTimePasswordAlgorithm

// This is a light ocra version (hotp with a challenge). See separate story for full ocra support.

class Ocra(private val _secret: ByteArray) {

    @Throws(InvalidKeyException::class, SecurityFeaturesException::class)
    fun computeResponse(challenge: Long): String {
        try {
            return OneTimePasswordAlgorithm.generateOTP(_secret, challenge, Ocra.RESPONSE_DIGITS, false, -1)
        } catch (e: NoSuchAlgorithmException) {
            throw SecurityFeaturesException()
        }
    }

    companion object {
        private val RESPONSE_DIGITS = 6
    }
}
