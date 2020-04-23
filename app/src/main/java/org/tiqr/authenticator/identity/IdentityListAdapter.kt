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

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.tiqr.authenticator.R
import org.tiqr.authenticator.databinding.ListItemIdentityBinding
import org.tiqr.authenticator.util.extensions.getThemeColor
import org.tiqr.data.model.IdentityWithProvider
import kotlin.math.roundToInt

/**
 * Adapter for the identity item layout
 */
class IdentityListAdapter(
        private val onClick: (IdentityWithProvider) -> Unit,
        private val onDelete: (IdentityWithProvider) -> Unit
) : ListAdapter<IdentityWithProvider, IdentityListAdapter.ViewHolder>(IdentityListDiffCallback()) {
    companion object {
        class IdentityListDiffCallback : DiffUtil.ItemCallback<IdentityWithProvider>() {
            override fun areItemsTheSame(
                    oldItem: IdentityWithProvider,
                    newItem: IdentityWithProvider
            ) = oldItem.identity.id == newItem.identity.id

            override fun areContentsTheSame(
                    oldItem: IdentityWithProvider,
                    newItem: IdentityWithProvider
            ) = oldItem.identity == newItem.identity
        }
    }

    override fun getItemId(position: Int) = getItem(position).identity.id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        DataBindingUtil.inflate<ListItemIdentityBinding>(
                LayoutInflater.from(parent.context),
                R.layout.list_item_identity,
                parent,
                false
        ).run {
            return ViewHolder(this)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position), onClick)

    class ViewHolder(private val binding: ListItemIdentityBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: IdentityWithProvider, onClick: (IdentityWithProvider) -> Unit) {
            binding.model = item
            binding.executePendingBindings()

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    /**
     * [ItemTouchHelper] to handle swipe to delete
     */
    class SwipeCallback(
            context: Context,
            private val adapter: IdentityListAdapter,
            private val onCancel: (RecyclerView.ViewHolder) -> Unit
    ) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        private val frameSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80f, context.resources.displayMetrics)
        private val background = ColorDrawable(context.getThemeColor(R.attr.colorError))
        private val icon = ContextCompat.getDrawable(context, R.drawable.ic_delete)?.apply {
            DrawableCompat.setTint(this, context.getThemeColor(R.attr.colorOnError))
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition

            AlertDialog.Builder(viewHolder.itemView.context)
                    .setTitle(R.string.identity_delete_title)
                    .setMessage(R.string.identity_delete_message)
                    .setNegativeButton(R.string.button_cancel) { dialog, _ ->
                        onCancel(viewHolder)
                        dialog.dismiss()
                    }
                    .setPositiveButton(R.string.button_delete) { _, _ ->
                        adapter.onDelete(adapter.getItem(position))
                    }
                    .show()
        }

        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
            val itemView = viewHolder.itemView
            val dX2 = if (dX > 0) dX.coerceIn(0f, frameSize) else dX.coerceIn(frameSize.unaryMinus(), 0f)

            val iconHeight = icon?.intrinsicHeight ?: 0
            val iconWidth = icon?.intrinsicWidth ?: 0
            val iconMargin = (itemView.height - iconHeight) / 2
            val iconTop = itemView.top + (itemView.height - iconHeight) / 2
            val iconBottom = iconTop + iconHeight

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                when {
                    dX > 0 -> { // Swiping right
                        background.setBounds(
                                itemView.left,
                                itemView.top,
                                (itemView.left + dX2).roundToInt(),
                                itemView.bottom
                        )

                        icon?.setBounds(
                                itemView.left + iconMargin + iconWidth,
                                iconTop,
                                itemView.left + iconMargin,
                                iconBottom
                        )
                    }
                    dX < 0 -> { // Swiping left
                        background.setBounds(
                                (itemView.right + dX2).roundToInt(),
                                itemView.top,
                                itemView.right,
                                itemView.bottom
                        )

                        icon?.setBounds(
                                itemView.right - iconMargin - iconWidth,
                                iconTop,
                                itemView.right - iconMargin,
                                iconBottom
                        )
                    }
                    else -> { // No swipe
                        icon?.setBounds(0, 0, 0, 0)
                        background.setBounds(0, 0, 0, 0)
                    }
                }
                background.draw(c)
                icon?.draw(c)
            }

            super.onChildDraw(c, recyclerView, viewHolder, dX2, dY, actionState, isCurrentlyActive)
        }
    }
}