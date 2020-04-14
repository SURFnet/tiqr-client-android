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

package org.tiqr.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.tiqr.data.api.adapter.ValueEnum

/**
 * Model for mapping response for challenges.
 */
sealed class ChallengeResponse<T> {
    abstract val code: T
}

/**
 * Model for mapping response for  enrollment challenges.
 */
@JsonClass(generateAdapter = true)
data class EnrollmentResponse(
        @Json(name = "responseCode")
        override val code: Code
) : ChallengeResponse<EnrollmentResponse.Code>() {
    enum class Code(override val value: Int) : ValueEnum {
        ENROLL_RESULT_SUCCESS(1),
        ENROLL_RESULT_INVALID_RESPONSE(101);

        override fun toString(): String = value.toString()
    }
}

/**
 * Model for mapping response for authentication challenges.
 */
@JsonClass(generateAdapter = true)
data class AuthenticationResponse(
        @Json(name = "responseCode")
        override val code: Code,
        val attemptsLeft: Int? = null,
        val duration: Int? = null
) : ChallengeResponse<AuthenticationResponse.Code>() {
    enum class Code(override val value: Int) : ValueEnum {
        AUTH_RESULT_SUCCESS(1),
        AUTH_RESULT_INVALID_RESPONSE(201),
        AUTH_RESULT_INVALID_REQUEST(202),
        AUTH_RESULT_INVALID_CHALLENGE(203),
        AUTH_RESULT_ACCOUNT_BLOCKED(204),
        AUTH_RESULT_INVALID_USER_ID(205);

        override fun toString(): String = value.toString()
    }
}
