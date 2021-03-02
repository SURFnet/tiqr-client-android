/*
 * Copyright (c) 2010-2021 SURFnet bv
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

package org.tiqr.data.api.response

import okhttp3.Headers
import java.io.IOException
import retrofit2.Retrofit

/**
 * Wrapper for [Retrofit] responses to parse an error body with the success body type.
 */
sealed class ApiResponse<out S : Any> {
    /**
     * Success response (2xx status code) with body
     */
    data class Success<S : Any>(
            val body: S,
            val code: Int,
            val headers: Headers
    ) : ApiResponse<S>()

    /**
     * Failure response (non-2xx status code) with a optional body
     */
    data class Failure<S : Any>(
            val body: S?,
            val code: Int,
            val headers: Headers
    ) : ApiResponse<S>()

    /**
     * Network error
     */
    data class NetworkError(
            val error: IOException
    ) : ApiResponse<Nothing>()

    /**
     * Any other non-network error
     */
    data class Error(
            val error: Throwable,
            val code: Int? = null
    ) : ApiResponse<Nothing>()
}
