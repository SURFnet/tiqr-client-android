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

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.tiqr.authenticator.R
import org.tiqr.authenticator.base.BaseFragment
import org.tiqr.authenticator.databinding.FragmentAuthenticationPinBinding
import org.tiqr.data.model.AuthenticationCompleteFailure
import org.tiqr.data.model.ChallengeCompleteResult
import org.tiqr.data.model.SecretCredential
import org.tiqr.data.viewmodel.AuthenticationViewModel

/**
 * Fragment to enter the PIN code for the authentication
 */
class AuthenticationPinFragment : BaseFragment<FragmentAuthenticationPinBinding>() {
    private val viewModel by navGraphViewModels<AuthenticationViewModel>(R.id.authentication_nav) { factory }

    @LayoutRes
    override val layout = R.layout.fragment_authentication_pin

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.pin.setConfirmListener { pin ->
            viewModel.authenticate(SecretCredential.pin(pin))
        }

        viewModel.authenticate.observe(viewLifecycleOwner) {
            when (it) {
                is ChallengeCompleteResult.Success -> {
                    findNavController().navigate(AuthenticationPinFragmentDirections.actionSummary(binding.pin.currentPin))
                }
                is ChallengeCompleteResult.Failure -> {
                    val failure = it.failure
                    if (failure is AuthenticationCompleteFailure) {
                        when (failure.reason) {
                            AuthenticationCompleteFailure.Reason.UNKNOWN,
                            AuthenticationCompleteFailure.Reason.CONNECTION -> {
                                findNavController().navigate(
                                        AuthenticationPinFragmentDirections.actionFallback(binding.pin.currentPin)
                                )
                            }
                            AuthenticationCompleteFailure.Reason.INVALID_RESPONSE -> {
                                val remaining = failure.remainingAttempts
                                if (remaining == null || remaining > 0) {
                                    binding.pin.clear(showKeyboard = true)
                                }

                                MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(failure.title)
                                        .setMessage(failure.message)
                                        .show()
                            }
                            else -> {
                                MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(failure.title)
                                        .setMessage(failure.message)
                                        .show()
                            }
                        }
                    }
                }
            }
        }
    }
}