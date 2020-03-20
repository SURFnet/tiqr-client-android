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

import android.os.Bundle

/**
 * Base Failure for [Challenge]
 */
abstract class ChallengeFailure {
    abstract val title: String
    abstract val message: String
}

/**
 * Failure for parsing [Challenge]
 */
sealed class ChallengeParseFailure : ChallengeFailure()

/**
 * Failure for parsing [EnrollmentChallenge]
 */
data class EnrollmentParseFailure(
        val reason: Reason = Reason.UNKNOWN,
        override val title: String,
        override val message: String
) : ChallengeParseFailure() {
    enum class Reason {
        UNKNOWN, CONNECTION, INVALID_CHALLENGE, INVALID_RESPONSE
    }
}

/**
 * Failure for parsing [AuthenticationChallenge]
 */
data class AuthenticationParseFailure(
        val reason: Reason = Reason.UNKNOWN,
        override val title: String,
        override val message: String
) : ChallengeParseFailure() {
    enum class Reason {
        UNKNOWN, INVALID_CHALLENGE, INVALID_IDENTITY_PROVIDER, INVALID_IDENTITY, NO_IDENTITIES
    }
}

/**
 * Generic parsing failure
 */
data class ParseFailure(
        override val title: String,
        override val message: String
): ChallengeParseFailure()

/**
 * Failure for completing [Challenge]
 */
sealed class ChallengeCompleteFailure : ChallengeFailure()

/**
 * Failure for completing [EnrollmentChallenge]
 */
data class EnrollmentCompleteFailure(
        val reason: Reason = Reason.UNKNOWN,
        override val title: String,
        override val message: String
) : ChallengeCompleteFailure() {
    enum class Reason {
        UNKNOWN, CONNECTION, INVALID_RESPONSE
    }
}

/**
 * Failure for completing [AuthenticationChallenge]
 */
data class AuthenticationCompleteFailure(
        val reason: Reason = Reason.UNKNOWN,
        override val title: String,
        override val message: String,
        val extras: Bundle? = null
) : ChallengeCompleteFailure() {
    enum class Reason {
        UNKNOWN, CONNECTION, INVALID_CHALLENGE, INVALID_REQUEST, INVALID_RESPONSE,
        INVALID_USER, ACCOUNT_BLOCKED, ACCOUNT_TEMPORARY_BLOCKED
    }
}