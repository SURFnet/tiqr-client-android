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

import retrofit2.Response
import okhttp3.ResponseBody
import retrofit2.Converter
import java.lang.reflect.Type

internal object ApiResponseHandler {
    /**
     * Converts the given [response] to a subclass of [ApiResponse] based on different conditions.
     *
     * If the server response is successful with:
     * => a non-empty body -> NetworkResponse.Success<S>
     * => an empty body (and [successType] is not Unit) -> NetworkResponse.ServerError<S>
     * => an empty body (and [successType] is Unit) -> NetworkResponse.Success<Unit>
     *
     * If the servers response is not successful:
     * => a non-empty body -> NetworkResponse.Failure<S>
     * => an empty body -> NetworkResponse.Failure<S>
     * => errors -> NetworkResponse.Error
     */
    fun <S : Any> handle(
            response: Response<S>,
            successType: Type,
            errorConverter: Converter<ResponseBody, S>
    ): ApiResponse<S> {
        val body = response.body()
        val headers = response.headers()
        val code = response.code()
        val errorBody = response.errorBody()

        return if (response.isSuccessful) {
            if (body != null) {
                ApiResponse.Success(body = body, code = code, headers = headers)
            } else {
                if (successType == Unit::class.java) {
                    @Suppress("UNCHECKED_CAST")
                    ApiResponse.Success(body = Unit, code = code, headers = headers) as ApiResponse<S>
                } else {
                    ApiResponse.Failure(body = null, code = code, headers = headers)
                }
            }
        } else {
            return try {
                val convertedBody = if (errorBody == null) {
                    null
                } else {
                    errorConverter.convert(errorBody)
                }
                if (successType == Unit::class.java) {
                    @Suppress("UNCHECKED_CAST")
                    ApiResponse.Success(body = Unit, code = code, headers = headers) as ApiResponse<S>
                } else {
                    ApiResponse.Failure(body = convertedBody, code = code, headers = headers)
                }
            } catch (ex: Exception) {
                ApiResponse.Error(error = ex, code = code)
            }
        }
    }
}
