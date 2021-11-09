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

package org.tiqr.core.authentication

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.tiqr.core.R
import org.tiqr.core.base.BaseFragment
import org.tiqr.core.databinding.FragmentAuthenticationSummaryBinding
import org.tiqr.core.util.extensions.biometricUsable
import org.tiqr.data.viewmodel.AuthenticationViewModel

/**
 * Fragment to summarize the authentication
 */
@AndroidEntryPoint
class AuthenticationSummaryFragment : BaseFragment<FragmentAuthenticationSummaryBinding>() {
    private val viewModel by hiltNavGraphViewModels<AuthenticationViewModel>(R.id.authentication_nav)
    private val args by navArgs<AuthenticationSummaryFragmentArgs>()

    @LayoutRes
    override val layout = R.layout.fragment_authentication_summary

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel

        binding.buttonOk.setOnClickListener {
            findNavController().popBackStack()
        }

        showBiometricUpgrade(args.pin)
    }

    /**
     * Show biometric upgrade if supported & required
     */
    private fun showBiometricUpgrade(pin: String?) {
        if (requireContext().biometricUsable() && viewModel.challenge.value?.identity?.biometricOfferUpgrade == true && pin != null) {
            MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.account_upgrade_biometric_title)
                    .setMessage(R.string.account_upgrade_biometric_message)
                    .setCancelable(false)
                    .setNegativeButton(R.string.button_cancel) { _, _ ->
                        viewModel.stopOfferBiometric()
                    }
                    .setPositiveButton(R.string.button_ok) { _, _ ->
                        viewModel.upgradeBiometric(pin)
                    }
                    .show()
        }
    }
}