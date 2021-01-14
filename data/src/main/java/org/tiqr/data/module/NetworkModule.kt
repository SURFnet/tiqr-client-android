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

package org.tiqr.data.module

import android.content.Context
import com.squareup.moshi.Moshi
import dagger.Lazy
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.tiqr.data.BuildConfig
import org.tiqr.data.api.interceptor.HeaderInjector
import org.tiqr.data.api.TiqrApi
import org.tiqr.data.api.adapter.AuthenticationResponseAdapter
import org.tiqr.data.api.adapter.EnrollmentResponseAdapter
import org.tiqr.data.api.adapter.addValueEnum
import org.tiqr.data.api.interceptor.UserAgentInjector
import org.tiqr.data.di.ApiScope
import org.tiqr.data.di.BaseScope
import org.tiqr.data.di.TokenScope
import org.tiqr.data.model.AuthenticationResponse
import org.tiqr.data.model.EnrollmentRequestJsonAdapter
import org.tiqr.data.model.EnrollmentResponse
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Module which serves the network related dependencies,
 * such as the HTTP client or API connector.
 */
@Module
internal object NetworkModule {
    //region OkHttp
    @Provides
    @Singleton
    @BaseScope
    internal fun provideOkHttpClientBuilder(): OkHttpClient {
        return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .build()
    }

    @Provides
    @Singleton
    @ApiScope
    internal fun provideApiOkHttpClient(
            @BaseScope client: OkHttpClient,
            userAgentInjector: UserAgentInjector,
            headerInjector: HeaderInjector,
            loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        val builder = client
                .newBuilder()
                .addInterceptor(headerInjector)
                .addInterceptor(userAgentInjector)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    internal fun provideImageOkHttpClientBuilder(
            @BaseScope client: OkHttpClient
    ): OkHttpClient.Builder = client.newBuilder()

    @Provides
    @Singleton
    @TokenScope
    internal fun provideTokenOkHttpClient(
            @BaseScope client: OkHttpClient,
            userAgentInjector: UserAgentInjector,
            loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        val builder = client
                .newBuilder()
                .addInterceptor(userAgentInjector)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    internal fun provideHeaderInjector(): HeaderInjector = HeaderInjector()

    @Provides
    @Singleton
    internal fun provideUserAgentInjector(context: Context): UserAgentInjector = UserAgentInjector(context)

    @Provides
    @Singleton
    internal fun provideLoggingInterceptor(): HttpLoggingInterceptor =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
    //endregion

    //region Retrofit
    @Provides
    @Singleton
    internal fun provideMoshi(): Moshi = Moshi.Builder()
            .add(EnrollmentResponseAdapter.create())
            .add(AuthenticationResponseAdapter.create())
            .addValueEnum(EnrollmentResponse.Code::class, EnrollmentResponse.Code.ENROLL_RESULT_INVALID_RESPONSE)
            .addValueEnum(AuthenticationResponse.Code::class, AuthenticationResponse.Code.AUTH_RESULT_INVALID_RESPONSE)
            .build()

    @Provides
    @Singleton
    @BaseScope
    internal fun provideBaseRetrofit(): Retrofit {
        return Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL) //Dummy base URL, since each api call uses its own url
                .build()
    }

    @Provides
    @Singleton
    @ApiScope
    internal fun provideApiRetrofit(
            @BaseScope retrofit: Retrofit,
            @ApiScope client: Lazy<OkHttpClient>,
            moshi: Moshi
    ): Retrofit {
        return retrofit.newBuilder()
                .callFactory { client.get().newCall(it) }
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(if (BuildConfig.PROTOCOL_COMPATIBILITY_MODE) MoshiConverterFactory.create(moshi).asLenient() else MoshiConverterFactory.create(moshi))
                .baseUrl(BuildConfig.BASE_URL) //Dummy base URL, since each api call uses its own url
                .build()
    }

    @Provides
    @Singleton
    @TokenScope
    internal fun provideTokenRetrofit(
            @BaseScope retrofit: Retrofit,
            @TokenScope client: Lazy<OkHttpClient>
    ): Retrofit {
        return retrofit.newBuilder()
                .callFactory { client.get().newCall(it) }
                .addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl(BuildConfig.TOKEN_EXCHANGE_BASE_URL)
                .build()
    }

    @Provides
    @Singleton
    internal fun provideTiqrApi(@ApiScope retrofit: Retrofit): TiqrApi = retrofit.create(TiqrApi::class.java)
    //endregion
}