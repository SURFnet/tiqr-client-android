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

package org.tiqr.data.util.extension

import okhttp3.Headers
import okhttp3.HttpUrl
import org.tiqr.data.api.interceptor.HeaderInjector
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder

/**
 * Check if url is valid
 */
internal fun HttpUrl.isHttpOrHttps() = scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)

/**
 * Extract the Tiqr Protocol from the [Headers]
 */
internal fun Headers.tiqrProtocol() = this[HeaderInjector.HEADER_PROTOCOL]?.toIntOrNull() ?: 0

/**
 * Convert a url from a [String] representation into a [URL]
 */
internal fun String.toUrlOrNull(): URL? =
        try {
            URL(this)
        } catch (e: MalformedURLException) {
            null
        }

/**
 * Decode the url
 */
internal fun String.toDecodedUrlStringOrNull(): String? {
    return try {
        if (this.isNotEmpty()) {
            URLDecoder.decode(this, Charsets.UTF_8.name())
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}