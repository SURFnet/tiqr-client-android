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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.LayoutRes
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.tiqr.authenticator.R
import org.tiqr.authenticator.base.BaseFragment
import org.tiqr.authenticator.databinding.FragmentAuthenticationConfirmBinding
import org.tiqr.data.viewmodel.AuthenticationViewModel
import org.tiqr.data.viewmodel.challengeViewModel

/**
 * Fragment to review and confirm the authentication
 */
@AndroidEntryPoint
class AuthenticationConfirmFragment: BaseFragment<FragmentAuthenticationConfirmBinding>() {
    private val args by navArgs<AuthenticationConfirmFragmentArgs>()
    private val viewModel by navGraphViewModels<AuthenticationViewModel>(R.id.authentication_nav) {
        factory.challengeViewModel(args.challenge)
    }

    @LayoutRes
    override val layout = R.layout.fragment_authentication_confirm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.challenge.value?.let {
            if (it.hasMultipleIdentities) {
                setHasOptionsMenu(true)
                findNavController().navigate(
                        AuthenticationConfirmFragmentDirections.actionIdentity(cancellable = false)
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_identity_select, menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel

        binding.buttonCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.buttonOk.setOnClickListener {
            if (viewModel.challenge.value?.identity?.biometricInUse == true) {
                findNavController().navigate(AuthenticationConfirmFragmentDirections.actionBiometric())
            } else {
                findNavController().navigate(AuthenticationConfirmFragmentDirections.actionPin())
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.identity_pick -> {
                findNavController().navigate(AuthenticationConfirmFragmentDirections.actionIdentity(cancellable = true))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}