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
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
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

import com.squareup.moshi.Moshi
import dagger.Lazy
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.tiqr.data.BuildConfig
import org.tiqr.data.api.HeaderInjector
import org.tiqr.data.api.TiqrApi
import org.tiqr.data.di.ApiScope
import org.tiqr.data.di.BaseScope
import org.tiqr.data.di.TokenScope
import org.tiqr.data.util.extension.callFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
object NetworkModule {
    //region OkHttp
    @Provides
    @Singleton
    @JvmStatic
    @BaseScope
    internal fun provideOkHttpClientBuilder(): OkHttpClient {
        return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .build()
    }

    @Provides
    @Singleton
    @JvmStatic
    @ApiScope
    internal fun provideApiOkHttpClient(
            @BaseScope client: OkHttpClient,
            headerInjector: HeaderInjector,
            loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        val builder = client
                .newBuilder()
                .addInterceptor(headerInjector)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    @JvmStatic
    @TokenScope
    internal fun provideTokenOkHttpClient(
            @BaseScope client: OkHttpClient,
            loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        val builder = client.newBuilder()
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    @JvmStatic
    internal fun provideHeaderInjector(): HeaderInjector = HeaderInjector()

    @Provides
    @Singleton
    @JvmStatic
    internal fun provideLoggingInterceptor(): HttpLoggingInterceptor =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
    //endregion

    //region Retrofit
    @Provides
    @Singleton
    @JvmStatic
    internal fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    @JvmStatic
    @BaseScope
    internal fun provideBaseRetrofit(): Retrofit {
        return Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL) //Dummy base URL, since each api call uses its own url
                .build()
    }

    @Provides
    @Singleton
    @JvmStatic
    @ApiScope
    internal fun provideApiRetrofit(
            @BaseScope retrofit: Retrofit,
            @ApiScope client: Lazy<OkHttpClient>,
            moshi: Moshi
    ): Retrofit {
        return retrofit.newBuilder()
                .callFactory { client.get().newCall(it) }
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .baseUrl(BuildConfig.BASE_URL) //Dummy base URL, since each api call uses its own url
                .build()
    }

    @Provides
    @Singleton
    @JvmStatic
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
    @JvmStatic
    @Singleton
    internal fun provideTiqrApi(@ApiScope retrofit: Retrofit): TiqrApi = retrofit.create(TiqrApi::class.java)
    //endregion
}