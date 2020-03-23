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

package org.tiqr.data.viewmodel

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.tiqr.data.R
import org.tiqr.data.model.Challenge
import org.tiqr.data.model.ChallengeParseFailure
import org.tiqr.data.model.ChallengeParseResult
import org.tiqr.data.model.ParseFailure
import org.tiqr.data.repository.AuthenticationRepository
import org.tiqr.data.repository.EnrollmentRepository
import javax.inject.Inject

/**
 * ViewModel for Scanning QR codes
 */
class ScanViewModel @Inject constructor(
        private val resources: Resources,
        private val enroll: EnrollmentRepository,
        private val auth: AuthenticationRepository
): ViewModel() {
    private val _challenge = MutableLiveData<ChallengeParseResult<Challenge, ChallengeParseFailure>>()
    val challenge : LiveData<ChallengeParseResult<Challenge, ChallengeParseFailure>> = _challenge

    /**
     * Parse the [rawChallenge] and send the result to [_challenge]
     */
    fun parseChallenge(rawChallenge: String) {
        viewModelScope.launch {
            _challenge.value = when {
                enroll.isValidChallenge(rawChallenge) -> enroll.parseChallenge(rawChallenge)
                auth.isValidChallenge(rawChallenge) -> auth.parseChallenge(rawChallenge)
                else ->
                    ChallengeParseResult.failure(
                            ParseFailure(
                                    title = resources.getString(R.string.error_qr_unknown_title),
                                    message = resources.getString(R.string.error_qr_unknown)
                            )
                    )
            }
        }
    }
}