/*
 * Copyright (c) 2010-2020 SURFnet bv
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of SURFnet bv nor the names of its contributors
 *    may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.tiqr.data.algorithm

import org.tiqr.data.util.extension.hexAsByteArray
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * This is an implementation of the OCRA spec in Kotlin.
 * It is based on the reference implementation.
 * @see <a href="http://tools.ietf.org/html/rfc6287">rfc6287</a>
 */
object Ocra {
    class OcraException(message: String? = null, cause: Exception? = null) : Exception(message, cause)

    private val DIGITS_POWER = intArrayOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000)

    /**
     * Generate an OCRA HOTP value for the given set of parameters.
     *
     * @param suite         the suite of operations to compute an OCRA response
     * @param key           the shared secret _(HEX encoded)_
     * @param counter       optional, synchronized between the client and the server _(HEX encoded)_
     * @param question      the challenge question _(HEX encoded)_
     * @param password      optional PIN/password _(HEX encoded)_
     * @param session       optional information about the current session _(HEX encoded)_
     * @param timestamp     optional timestamp since Unix epoch
     *
     * @throws OcraException if the provided [suite] is unsupported, or computation fails
     */
    fun generate(
            suite: String,
            key: String,
            counter: String? = null,
            question: String,
            password: String? = null,
            session: String? = null,
            timestamp: String? = null
    ): String {
        // The suite components
        val (algo: String, crypt: String, input: String) = with(suite.toUpperCase(Locale.ROOT).split(":")) {
            if (size != 3) throw OcraException("Invalid OCRA suite provided")
            this
        }

        if (algo != "OCRA-1") throw OcraException("Unsupported OCRA version")

        // CryptoFunction
        val (crypto: String, digits: Int) = with(crypt.split("-")) {
            when {
                size != 3 -> throw OcraException("Invalid OCRA crypto specified")
                first() != "HOTP" -> throw OcraException("Invalid OCRA crypto mode specified (only HOTP is accepted)")
            }

            val algorithm: String = when (this[1]) {
                "SHA1" -> "HmacSHA1"
                "SHA256" -> "HmacSHA256"
                "SHA512" -> "HmacSHA512"
                else -> throw OcraException("Invalid OCRA crypto specified")
            }

            val truncation: Int = this[2].toIntOrNull() ?: 0

            algorithm to truncation
        }

        // DataInput
        @Suppress("NAME_SHADOWING")
        val message: ByteArray = with(input) {
            // Counter
            val (counter: ByteArray?, counterSize: Int) = when {
                startsWith("C") -> counter?.padStart(16, '0')?.hexAsByteArray() to 8
                else -> counter?.hexAsByteArray() to 0
            }
            // Question - always 128 bytes
            val (question: ByteArray, questionSize: Int) = when {
                startsWith("Q") || contains("-Q") -> {
                    question.padEnd(256, '0').hexAsByteArray() to 128
                }
                else -> question.hexAsByteArray() to 0
            }
            // Password
            val (password: ByteArray?, passwordSize: Int) = when {
                contains("PHSA1") -> password?.padStart(40, '0')?.hexAsByteArray() to 20
                contains("PSHA256") -> password?.padStart(64, '0')?.hexAsByteArray() to 32
                contains("PSHA512") -> password?.padStart(128, '0')?.hexAsByteArray() to 64
                else -> password?.hexAsByteArray() to 0
            }
            // Session
            val (session: ByteArray?, sessionSize: Int) = when {
                contains("S064") -> session?.padStart(128, '0')?.hexAsByteArray() to 64
                contains("S128") -> session?.padStart(256, '0')?.hexAsByteArray() to 128
                contains("S256") -> session?.padStart(512, '0')?.hexAsByteArray() to 256
                contains("S512") -> session?.padStart(1024, '0')?.hexAsByteArray() to 512
                // Deviation from RFC. Tiqr supports 's' without length indicator.
                contains("S") -> session?.padStart(128, '0')?.hexAsByteArray() to 64
                else -> session?.hexAsByteArray() to 0
            }
            // Timestamp
            val (timestamp: ByteArray?, timestampSize: Int) = when {
                startsWith("T") || contains("-T") -> {
                    timestamp?.padStart(16, '0')?.hexAsByteArray() to 8
                }
                else -> timestamp?.hexAsByteArray() to 0
            }

            val delimiterSize = 1
            val suiteSize: Int = suite.toByteArray().size
            val msg = ByteArray(suiteSize + delimiterSize + counterSize + questionSize + passwordSize + sessionSize + timestampSize)

            var pos = 0
            // Add bytes for suite into the message
            suite.toByteArray().copyInto(destination = msg, destinationOffset = pos)
            pos += suiteSize
            // Add bytes for delimiter
            msg[pos] = 0x00
            pos += delimiterSize
            // Add bytes for counter into the message
            if (counterSize > 0) counter?.copyInto(destination = msg, destinationOffset = pos)
            pos += counterSize
            // Add bytes for question into the message
            if (questionSize > 0) question.copyInto(destination = msg, destinationOffset = pos)
            pos += questionSize
            // Add bytes for password into the message
            if (passwordSize > 0) password?.copyInto(destination = msg, destinationOffset = pos)
            pos += passwordSize
            // Add bytes for session info into the message
            if (sessionSize > 0) session?.copyInto(destination = msg, destinationOffset = pos)
            pos += sessionSize
            // Add bytes for timestamp into the message
            if (timestampSize > 0) timestamp?.copyInto(destination = msg, destinationOffset = pos)
            pos += timestampSize

            msg
        }

        return with(
                try {
                    computeHMAC(crypto, key.hexAsByteArray(), message)
                } catch (e: Exception) {
                    throw OcraException("Failed to calculate HMAC hash")
                }) {
            val offset: Int = this[size - 1].toInt() and 0xf
            val binary: Int = this[offset].toInt() and 0x7f shl 24 or
                    (this[offset + 1].toInt() and 0xff shl 16) or
                    (this[offset + 2].toInt() and 0xff shl 8) or
                    (this[offset + 3].toInt() and 0xff)

            binary % DIGITS_POWER[digits]
        }.run {
            toString().padStart(digits, '0')
        }
    }

    /**
     * This method uses the JCE to provide the crypto algorithm.
     * HMAC computes a Hashed Message Authentication Code with the crypto hash algorithm as a parameter.
     *
     * @throws OcraException when computing HMAC fails
     */
    private fun computeHMAC(crypto: String, keyBytes: ByteArray, text: ByteArray): ByteArray {
        return try {
            val macKey = SecretKeySpec(keyBytes, "RAW")

            Mac.getInstance(crypto).run {
                init(macKey)
                doFinal(text)
            }
        } catch (e: Exception) {
            throw OcraException("Failed to compute HMAC", e)
        }
    }
}