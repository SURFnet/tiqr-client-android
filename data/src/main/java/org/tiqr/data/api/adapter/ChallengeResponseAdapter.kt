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

package org.tiqr.data.api.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import org.tiqr.data.model.AuthenticationResponse
import org.tiqr.data.model.AuthenticationResponse.Code.AUTH_RESULT_ACCOUNT_BLOCKED
import org.tiqr.data.model.AuthenticationResponse.Code.AUTH_RESULT_INVALID_CHALLENGE
import org.tiqr.data.model.AuthenticationResponse.Code.AUTH_RESULT_INVALID_REQUEST
import org.tiqr.data.model.AuthenticationResponse.Code.AUTH_RESULT_INVALID_RESPONSE
import org.tiqr.data.model.AuthenticationResponse.Code.AUTH_RESULT_INVALID_USER_ID
import org.tiqr.data.model.AuthenticationResponse.Code.AUTH_RESULT_SUCCESS
import org.tiqr.data.model.EnrollmentResponse
import org.tiqr.data.model.EnrollmentResponse.Code.ENROLL_RESULT_INVALID_RESPONSE
import org.tiqr.data.model.EnrollmentResponse.Code.ENROLL_RESULT_SUCCESS

private const val CODE_OK = "OK"
private const val CODE_ACCOUNT_BLOCKED = "ACCOUNT_BLOCKED"
private const val CODE_INVALID_CHALLENGE = "INVALID_CHALLENGE"
private const val CODE_INVALID_REQUEST = "INVALID_REQUEST"
private const val CODE_INVALID_USERID = "INVALID_USERID"
private const val CODE_INVALID_RESPONSE = "INVALID_RESPONSE"

/**
 * Custom Moshi adapter to parse [EnrollmentResponse] in ascii & json format
 */
@Deprecated("Protocol version 1 (ascii) is not supported anymore")
class EnrollmentResponseAdapter private constructor() {
    companion object {
        fun create() = EnrollmentResponseAdapter()
    }

    @FromJson
    fun fromJson(
            reader: JsonReader,
            delegate: JsonAdapter<EnrollmentResponse>
    ): EnrollmentResponse? {
        return with(reader) {
            when (peek()) {
                JsonReader.Token.STRING -> {
                    // v1 response format (ascii)
                    EnrollmentResponse(code = when (nextString()) {
                        CODE_OK -> ENROLL_RESULT_SUCCESS
                        else -> ENROLL_RESULT_INVALID_RESPONSE
                    })
                }
                JsonReader.Token.BEGIN_OBJECT -> {
                    // v2 response format (json)
                    delegate.fromJson(this)
                }
                else -> {
                    // unknown response format
                    null
                }
            }
        }
    }
}

/**
 * Custom Moshi adapter to parse [AuthenticationResponse] in ascii & json format
 */
@Deprecated("Protocol version 1 (ascii) is not supported anymore")
class AuthenticationResponseAdapter private constructor() {
    companion object {
        fun create() = AuthenticationResponseAdapter()
    }

    @FromJson
    fun fromJson(
            reader: JsonReader,
            delegate: JsonAdapter<AuthenticationResponse>
    ): AuthenticationResponse? {
        return with(reader) {
            when (peek()) {
                JsonReader.Token.STRING -> {
                    // v1 response format (ascii)
                    with(nextString()) {
                        when {
                            this == CODE_OK -> AuthenticationResponse(code = AUTH_RESULT_SUCCESS)
                            this == CODE_ACCOUNT_BLOCKED -> AuthenticationResponse(code = AUTH_RESULT_ACCOUNT_BLOCKED)
                            this == CODE_INVALID_CHALLENGE -> AuthenticationResponse(code = AUTH_RESULT_INVALID_CHALLENGE)
                            this == CODE_INVALID_REQUEST -> AuthenticationResponse(code = AUTH_RESULT_INVALID_REQUEST)
                            this == CODE_INVALID_USERID -> AuthenticationResponse(code = AUTH_RESULT_INVALID_USER_ID)
                            startsWith(CODE_INVALID_RESPONSE) -> {
                                val remainingAttempts = substringAfter('|').toIntOrNull() ?: 0
                                return AuthenticationResponse(code = AUTH_RESULT_INVALID_RESPONSE, attemptsLeft = remainingAttempts)
                            }
                            else -> AuthenticationResponse(code = AUTH_RESULT_INVALID_RESPONSE)
                        }
                    }
                }
                JsonReader.Token.BEGIN_OBJECT -> {
                    // v2 response format (json)
                    delegate.fromJson(this)
                }
                else -> {
                    // unknown response format
                    null
                }
            }
        }
    }
}