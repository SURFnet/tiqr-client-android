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

package org.tiqr.core.scan

import android.os.Bundle
import android.view.View
import androidx.core.view.doOnLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import org.tiqr.core.R
import org.tiqr.core.base.BaseFragment
import org.tiqr.core.databinding.FragmentScanBinding
import org.tiqr.core.util.extensions.hasCameraPermission
import org.tiqr.data.model.AuthenticationChallenge
import org.tiqr.data.model.ChallengeParseResult
import org.tiqr.data.model.EnrollmentChallenge
import org.tiqr.data.viewmodel.ScanViewModel

@AndroidEntryPoint
class ScanFragment : BaseFragment<FragmentScanBinding>() {
    override val layout = R.layout.fragment_scan

    private val viewModel by viewModels<ScanViewModel>()

    private lateinit var scanComponent: ScanComponent

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewFinder.doOnLayout {
            scanComponent = ScanComponent(
                    context = requireContext(),
                    lifecycleOwner = viewLifecycleOwner,
                    viewFinder = binding.viewFinder,
                    viewFinderRatio = it.height.toFloat() / it.width.toFloat()
            ) { result ->
                binding.progress.show()
                viewModel.parseChallenge(result)
            }
        }

        viewModel.challenge.observe(viewLifecycleOwner, this::handleParse)
    }

    /**
     * Parse the result after scanning the QR code.
     */
    private fun handleParse(result: ChallengeParseResult<*, *>) {
        binding.progress.hide()
        when (result) {
            is ChallengeParseResult.Success -> {
                viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                    delay(200L) // delay a bit, otherwise beep sound is cutoff
                    when (result.value) {
                        is EnrollmentChallenge -> findNavController().navigate(ScanFragmentDirections.actionEnroll(result.value as EnrollmentChallenge))
                        is AuthenticationChallenge -> findNavController().navigate(ScanFragmentDirections.actionAuthenticate(result.value as AuthenticationChallenge))
                    }
                }
            }
            is ChallengeParseResult.Failure -> {
                MaterialAlertDialogBuilder(requireContext())
                        .setTitle(result.failure.title)
                        .setMessage(result.failure.message)
                        .setCancelable(false)
                        .setNegativeButton(R.string.button_cancel) { _, _ -> findNavController().popBackStack() }
                        .setPositiveButton(R.string.button_retry) { dialog, _ ->
                            dialog.dismiss()
                            scanComponent.resumeScanning()
                        }
                        .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check permission again, since user could have revoked it while in paused state.
        if (requireActivity().hasCameraPermission().not()) {
            findNavController().popBackStack()
        }
    }
}