/*
 * Copyright (c) 2010-2019 SURFnet bv
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

package org.tiqr.data.api

import org.tiqr.data.BuildConfig
import org.tiqr.data.model.AuthenticationResponse
import org.tiqr.data.model.EnrollmentRequest
import org.tiqr.data.model.EnrollmentResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API endpoints.
 */
interface TiqrApi {
    companion object {
        private const val FIELD_APP_ID_KEY = "appId"
        private const val FIELD_DEVICE_TOKEN_KEY = "deviceToken"
        private const val FIELD_NOTIFICATION_TOKEN_KEY = "notificationToken"

        private const val FIELD_SECRET_KEY = "secret"
        private const val FIELD_LANGUAGE_KEY = "language"
        private const val FIELD_NOTIFICATION_TYPE_KEY = "notificationType"
        private const val FIELD_NOTIFICATION_TYPE_VALUE = "GCM"
        private const val FIELD_NOTIFICATION_ADDRESS_KEY = "notificationAddress"
        private const val FIELD_OPERATION_KEY = "operation"
        private const val FIELD_OPERATION_VALUE_REGISTER = "register"
        private const val FIELD_OPERATION_VALUE_LOGIN = "login"

        private const val FIELD_SESSION_KEY = "sessionKey"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_RESPONSE = "response"
    }

    @POST("tokenexchange/")
    @FormUrlEncoded
    suspend fun registerDeviceToken(
            @Query(FIELD_APP_ID_KEY) appId: String = BuildConfig.TOKEN_EXCHANGE_APP_ID,
            @Query(FIELD_DEVICE_TOKEN_KEY) deviceToken: String,
            @Query(FIELD_NOTIFICATION_TOKEN_KEY) notificationToken: String? = null
    ): String

    @GET
    suspend fun requestEnroll(@Url url: String): Response<EnrollmentRequest>

    @POST
    @FormUrlEncoded
    suspend fun enroll(
            @Url url: String,
            @Field(FIELD_SECRET_KEY) secret: String,
            @Field(FIELD_LANGUAGE_KEY) language: String,
            @Field(FIELD_NOTIFICATION_ADDRESS_KEY) notificationAddress: String? = null,
            @Field(FIELD_NOTIFICATION_TYPE_KEY) notificationType: String? = if (notificationAddress == null) null else FIELD_NOTIFICATION_TYPE_VALUE,
            @Field(FIELD_OPERATION_KEY) operation: String = FIELD_OPERATION_VALUE_REGISTER
    ): Response<EnrollmentResponse>

    @POST
    @FormUrlEncoded
    suspend fun authenticate(
            @Url url: String,
            @Field(FIELD_SESSION_KEY) sessionKey: String,
            @Field(FIELD_USER_ID) userId: String?,
            @Field(FIELD_RESPONSE) response: String,
            @Field(FIELD_LANGUAGE_KEY) language: String,
            @Field(FIELD_NOTIFICATION_ADDRESS_KEY) notificationAddress: String?,
            @Field(FIELD_NOTIFICATION_TYPE_KEY) notificationType: String? = if (notificationAddress == null) null else FIELD_NOTIFICATION_TYPE_VALUE,
            @Field(FIELD_OPERATION_KEY) operation: String = FIELD_OPERATION_VALUE_LOGIN
    ): Response<AuthenticationResponse>
}