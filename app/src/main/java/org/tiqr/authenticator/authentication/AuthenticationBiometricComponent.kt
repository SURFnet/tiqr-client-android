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

package org.tiqr.authenticator.authentication

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.tiqr.authenticator.R
import timber.log.Timber
import java.util.concurrent.Executor

/**
 * Component to use biometrics to authenticate.
 */
class AuthenticationBiometricComponent(
        fragment: Fragment,
        context: Context,
        private val onResult: (BiometricResult) -> Unit
) : BiometricPrompt.AuthenticationCallback() {
    private val executor: Executor = ContextCompat.getMainExecutor(context)
    private val promptInfo: BiometricPrompt.PromptInfo
    private val prompt: BiometricPrompt

    init {
        prompt = BiometricPrompt(fragment, executor, this)
        promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.auth_biometric_dialog_title))
                .setNegativeButtonText(context.getString(R.string.auth_biometric_dialog_cancel))
                .build()
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        when (errorCode) {
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onResult(BiometricResult.Cancel)
            else -> Timber.d("Error using biometrics #$errorCode: $errString")
        }
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        onResult(BiometricResult.Success)
    }

    override fun onAuthenticationFailed() {
        onResult(BiometricResult.Fail)
    }

    /**
     * Authenticate using biometrics.
     */
    fun authenticate() {
        prompt.authenticate(promptInfo)
    }

    sealed class BiometricResult {
        object Success : BiometricResult()
        object Cancel : BiometricResult()
        object Fail: BiometricResult()
    }
}