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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.tiqr.data.model.EnrollmentChallenge
import org.tiqr.data.model.EnrollmentCompleteRequest
import org.tiqr.data.repository.EnrollmentRepository
import timber.log.Timber

/**
 * ViewModel for Enrollment
 */
class EnrollmentViewModel @AssistedInject constructor(
        @Assisted override val _challenge: MutableLiveData<EnrollmentChallenge>,
        override val repository: EnrollmentRepository
) : ChallengeViewModel<EnrollmentChallenge, EnrollmentRepository>() {
    private val enrollmentComplete = MutableLiveData<EnrollmentCompleteRequest<EnrollmentChallenge>>()
    val enrollment = enrollmentComplete.switchMap {
        liveData {
            emit(repository.completeChallenge(it))
        }
    }

    /**
     * Perform enroll
     */
    fun enroll(password: String) {
        challenge.value?.let {
            enrollmentComplete.value = EnrollmentCompleteRequest(it, password)
        } ?: Timber.e("Cannot enroll, challenge is null")
    }

    /**
     * Factory to inject the [EnrollmentChallenge] at runtime
     */
    @AssistedFactory
    interface Factory : ChallengeViewModelFactory<EnrollmentChallenge> {
        override fun create(challenge: MutableLiveData<EnrollmentChallenge>): EnrollmentViewModel
    }
}