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
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
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

package org.tiqr.authenticator.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import org.tiqr.authenticator.di.TiqrComponent
import org.tiqr.data.viewmodel.ViewModelFactory

/**
 * Base Fragment.
 */
abstract class BaseFragment<B : ViewDataBinding> : Fragment(), BindingProvider<B> {
    final override var _binding: B? = null
        private set

    @get:LayoutRes
    protected abstract val layout: Int

    /**
     * Get the Dagger App Component.
     */
    protected val component: TiqrComponent
        @CheckResult
        get() = requireContext()
                .applicationContext
                .getSystemService(TiqrComponent::class.java.name) as TiqrComponent

    /**
     * Get the ViewModelFactory
     */
    protected val factory: ViewModelFactory
        get() = component.viewModeFactory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return DataBindingUtil.inflate<B>(inflater, layout, container, false)
                .apply {
                    lifecycleOwner = viewLifecycleOwner
                    _binding = this
                }
                .root
    }

    override fun onDestroyView() {
        _binding?.unbind()
        _binding = null

        super.onDestroyView()
    }
}