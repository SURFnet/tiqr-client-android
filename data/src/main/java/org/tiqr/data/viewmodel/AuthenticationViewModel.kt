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

package org.tiqr.data.viewmodel

import androidx.lifecycle.*
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import org.tiqr.data.model.AuthenticationChallenge
import org.tiqr.data.model.AuthenticationCompleteRequest
import org.tiqr.data.model.Identity
import org.tiqr.data.repository.AuthenticationRepository
import org.tiqr.data.service.SecretService
import org.tiqr.data.util.extension.mutate
import timber.log.Timber

/**
 * ViewModel for Authentication
 */
class AuthenticationViewModel @AssistedInject constructor(
        @Assisted override val _challenge: MutableLiveData<AuthenticationChallenge>,
        override val repository: AuthenticationRepository,
) : ChallengeViewModel<AuthenticationChallenge, AuthenticationRepository>() {
    private val authenticationComplete = MutableLiveData<AuthenticationCompleteRequest<AuthenticationChallenge>>()
    val authenticate = authenticationComplete.switchMap {
        liveData {
            emit(repository.completeChallenge(it))
        }
    }

    private val otpGenerate = MutableLiveData<String>()
    val otp = otpGenerate.switchMap { password ->
        liveData {
            challenge.value?.let { challenge ->
                challenge.identity?.let {
                    emit(repository.completeOtp(password, it, challenge))
                }
            }
        }
    }

    /**
     * Update the [Identity] for this [AuthenticationChallenge]
     */
    fun updateIdentity(identity: Identity) {
        _challenge.mutate {
            value?.identity = identity
        }
    }

    /**
     * Perform authenticate
     */
    fun authenticate(password: String, type: SecretService.Type = SecretService.Type.PIN_CODE) {
        challenge.value?.let {
            authenticationComplete.value = AuthenticationCompleteRequest(it, password, type)
        } ?: Timber.e("Cannot authenticate, challenge is null")
    }

    /**
     * Perform OTP generation
     */
    fun generateOTP(password: String) {
        otpGenerate.mutate {
            value = password
        }
    }

    /**
     * Factory to inject the [AuthenticationChallenge] at runtime
     */
    @AssistedInject.Factory
    interface Factory {
        fun create(_challenge: MutableLiveData<AuthenticationChallenge>): AuthenticationViewModel
    }
}