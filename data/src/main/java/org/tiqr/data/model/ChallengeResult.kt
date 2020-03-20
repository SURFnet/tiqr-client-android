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

/**
 * Result for parsing [Challenge]
 */
sealed class ChallengeParseResult<out S : Challenge, out F : ChallengeParseFailure> {
    companion object {
        /**
         * Create a _success_ result
         */
        fun <S : Challenge> success(v: S) = Success(v)

        /**
         * Create a _failure_ result
         */
        fun <F : ChallengeParseFailure> failure(f: F) = Failure(f)
    }

    /**
     * Is this result a _success_?
     */
    val isSuccess
        get() = this is Success

    /**
     * Is this result a _failure_?
     */
    val isFailure
        get() = this is Failure

    /**
     * Parsing a [Challenge] was successful
     */
    data class Success<out S : Challenge>(val value: S) : ChallengeParseResult<S, Nothing>()

    /**
     * Parsing a [Challenge] failed
     */
    data class Failure<out F : ChallengeParseFailure>(val failure: F) : ChallengeParseResult<Nothing, F>()
}

/**
 * Result for completing [Challenge]
 */
sealed class ChallengeCompleteResult<out F : ChallengeCompleteFailure> {
    companion object {
        /**
         * Create a _success_ result
         */
        fun success() = Success

        /**
         * Create a _failure_ result
         */
        fun <F : ChallengeCompleteFailure> failure(f: F) = Failure(f)
    }

    /**
     * Is this result a _success_?
     */
    val isSuccess
        get() = this is Success

    /**
     * Is this result a _failure_?
     */
    val isFailure
        get() = this is Failure

    /**
     * Completing a [Challenge] was successful
     */
    object Success : ChallengeCompleteResult<Nothing>()

    /**
     * Completing a [Challenge] failed
     */
    data class Failure<out F : ChallengeCompleteFailure>(val failure: F) : ChallengeCompleteResult<F>()
}