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

package org.tiqr.authenticator

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.CoilUtils
import okhttp3.OkHttpClient
import org.tiqr.authenticator.di.DaggerTiqrComponent
import org.tiqr.authenticator.di.TiqrComponent
import timber.log.Timber
import javax.inject.Inject

class TiqrApplication : Application(), ImageLoaderFactory {
    private lateinit var component: TiqrComponent
    @Inject
    internal lateinit var imageOkHttpClient: OkHttpClient.Builder

    override fun onCreate() {
        super.onCreate()

        // Setup Timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        component = DaggerTiqrComponent.factory().create(this)
        component.inject(this)

        // Set the Coil's singleton instance
        Coil.setImageLoader(this)
    }

    override fun getSystemService(name: String): Any? {
        return if (TiqrComponent::class.java.name == name) {
            component
        } else {
            super.getSystemService(name)
        }
    }

    /**
     * Use our own okHttp to share pools
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(context = this)
                .crossfade(enable = true)
                .okHttpClient {
                    imageOkHttpClient
                            .cache(CoilUtils.createDefaultCache(context = this))
                            .build()
                }
                .build()
    }
}