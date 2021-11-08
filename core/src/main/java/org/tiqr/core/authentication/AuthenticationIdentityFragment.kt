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

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import org.tiqr.core.R
import org.tiqr.core.base.BaseDialogFragment
import org.tiqr.core.databinding.FragmentAuthenticationIdentityBinding
import org.tiqr.data.model.Identity
import org.tiqr.data.model.IdentityProvider
import org.tiqr.data.viewmodel.AuthenticationViewModel

/**
 * Fragment to pick an [Identity] if there are multiple identities for the same [IdentityProvider].
 */
@AndroidEntryPoint
class AuthenticationIdentityFragment : BaseDialogFragment<FragmentAuthenticationIdentityBinding>() {
    private val args by navArgs<AuthenticationIdentityFragmentArgs>()
    private val viewModel by hiltNavGraphViewModels<AuthenticationViewModel>(R.id.authentication_nav)
    private val listAdapter = AuthenticationIdentityAdapter(::onItemClick)

    override val layout = R.layout.fragment_authentication_identity

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.list.adapter = listAdapter

        viewModel.challenge.observe(viewLifecycleOwner) {
            listAdapter.submitList(it.identities)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setCanceledOnTouchOutside(args.cancellable)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        if (viewModel.challenge.value?.identity == null) {
            // If user did not pick an identity, we pop
            findNavController().popBackStack(R.id.authentication_confirm, true)
        } else {
            super.onCancel(dialog)
        }
    }

    private fun onItemClick(item: Identity) {
        viewModel.updateIdentity(item)
        dismiss()
    }
}