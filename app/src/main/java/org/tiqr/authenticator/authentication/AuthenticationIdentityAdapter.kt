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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.tiqr.authenticator.R
import org.tiqr.authenticator.databinding.ListItemIdentitySelectBinding
import org.tiqr.data.model.Identity

/**
 * Adapter for the identity item layout
 */
class AuthenticationIdentityAdapter(
        private val onClick: (Identity) -> Unit
) : ListAdapter<Identity, AuthenticationIdentityAdapter.ViewHolder>(IdentityListDiffCallback()) {
    companion object {
        class IdentityListDiffCallback : DiffUtil.ItemCallback<Identity>() {
            override fun areItemsTheSame(oldItem: Identity, newItem: Identity) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Identity, newItem: Identity) = oldItem == newItem
        }
    }

    override fun getItemId(position: Int) = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        DataBindingUtil.inflate<ListItemIdentitySelectBinding>(
                LayoutInflater.from(parent.context),
                R.layout.list_item_identity_select,
                parent,
                false
        ).run {
            return ViewHolder(this)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class ViewHolder(private val binding: ListItemIdentitySelectBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Identity, onClick: (Identity) -> Unit) {
            binding.model = item
            binding.executePendingBindings()

            binding.root.setOnClickListener { onClick(item) }
        }
    }
}