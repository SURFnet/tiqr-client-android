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

package org.tiqr.authenticator.start

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import org.tiqr.authenticator.R
import org.tiqr.authenticator.base.BindingFragment
import org.tiqr.authenticator.databinding.FragmentStartBinding
import org.tiqr.authenticator.util.extensions.doOnCameraPermission
import org.tiqr.data.viewmodel.MainViewModel
import timber.log.Timber

/**
 * Fragment to handle main screen and button to qr-scanner.
 */
class StartFragment : BindingFragment<FragmentStartBinding>() {
    private val mainViewModel by activityViewModels<MainViewModel> { component.viewModeFactory }

    @LayoutRes
    override val layout: Int = R.layout.fragment_start

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = mainViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        mainViewModel.identityCount.observe(viewLifecycleOwner) {
            // rebuild options menu when count changes
            requireActivity().invalidateOptionsMenu()
        }

        binding.scanButton.setOnClickListener {
            requireActivity().doOnCameraPermission {
                findNavController().navigate(R.id.action_mainFragment_to_scanFragment)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_identity, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.identity)?.apply {
            isVisible = (mainViewModel.identityCount.value ?: 0) > 0
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.identity -> {
                Timber.i("Identities clicked")
                // TODO: open IdentityList
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
