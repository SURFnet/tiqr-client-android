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

package org.tiqr.authenticator.identity

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import org.tiqr.authenticator.R
import org.tiqr.authenticator.base.BaseFragment
import org.tiqr.authenticator.databinding.FragmentIdentityListBinding
import org.tiqr.authenticator.util.extensions.doOnCameraPermission
import org.tiqr.data.model.IdentityWithProvider
import org.tiqr.data.viewmodel.IdentityViewModel

/**
 * Fragment to displays the list of identities.
 */
class IdentityListFragment : BaseFragment<FragmentIdentityListBinding>() {
    private val viewModel by navGraphViewModels<IdentityViewModel>(R.id.identity_nav) { factory }
    private val listAdapter = IdentityListAdapter(::onItemClick, ::onItemDelete)

    private lateinit var itemTouchHelper: ItemTouchHelper

    @LayoutRes
    override val layout = R.layout.fragment_identity_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.list.apply {
            adapter = listAdapter

            IdentityListAdapter.SwipeCallback(requireContext(), listAdapter) { viewHolder ->
                itemTouchHelper.startSwipe(viewHolder)
            }.run {
                itemTouchHelper = ItemTouchHelper(this)
                itemTouchHelper.attachToRecyclerView(this@apply)
            }
        }

        binding.add.setOnClickListener {
            requireActivity().doOnCameraPermission {
                findNavController().navigate(IdentityListFragmentDirections.actionIdentityAdd())
            }
        }

        viewModel.identities.observe(viewLifecycleOwner) { identities ->
            if (identities.isNullOrEmpty()) {
                findNavController().popBackStack()
            }
            listAdapter.submitList(identities)
        }
    }

    /**
     * Handle the [item] selection
     */
    private fun onItemClick(item: IdentityWithProvider) {
        findNavController().navigate(IdentityListFragmentDirections.actionIdentityDetail(item))
    }

    /**
     * Handle the [item] deletion
     */
    private fun onItemDelete(item: IdentityWithProvider) {
        viewModel.deleteIdentity(item.identity)
    }
}