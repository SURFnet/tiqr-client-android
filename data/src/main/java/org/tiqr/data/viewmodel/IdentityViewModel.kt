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

package org.tiqr.data.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.tiqr.data.model.Identity
import org.tiqr.data.model.IdentityWithProvider
import org.tiqr.data.repository.IdentityRepository
import javax.inject.Inject

/**
 * ViewModel for the identity screens (list & detail).
 */
@HiltViewModel
class IdentityViewModel @Inject constructor(private val repository: IdentityRepository) : ViewModel() {
    private val identifier = MutableLiveData<String>()
    val identities = repository.allIdentities().asLiveData(viewModelScope.coroutineContext)
    val identity: LiveData<IdentityWithProvider?> = identifier.switchMap {
        repository.identity(it).asLiveData(viewModelScope.coroutineContext)
    }

    /**
     * Get the [Identity] with given [identifier]
     */
    fun getIdentity(identifier: String) {
        this.identifier.value = identifier
    }

    /**
     * Toggle biometric [use] for [identity]
     */
    fun useBiometric(identity: Identity, use: Boolean) {
        viewModelScope.launch {
            repository.useBiometric(identity, use)
        }
    }

    /**
     * Toggle biometric [upgrade] for [identity]
     */
    fun upgradeToBiometric(identity: Identity, upgrade: Boolean) {
        viewModelScope.launch {
            repository.upgradeToBiometric(identity, upgrade)
        }
    }

    /**
     * Delete [identity]
     */
    fun deleteIdentity(identity: Identity) {
        viewModelScope.launch {
            repository.delete(identity)
        }
    }

    /**
     * Check if a secret for biometric is available.
     */
    fun hasBiometricSecret(identity: Identity): Boolean = repository.hasBiometricSecret(identity)
}