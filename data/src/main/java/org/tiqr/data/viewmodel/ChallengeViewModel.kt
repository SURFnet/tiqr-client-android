package org.tiqr.data.viewmodel

import androidx.lifecycle.ViewModel
import org.tiqr.data.model.Challenge
import org.tiqr.data.repository.base.ChallengeRepository

/**
 * Base ViewModel for [Challenge]
 */
abstract class ChallengeViewModel<C : Challenge, R: ChallengeRepository<*>> : ViewModel() {
    abstract val challenge: C
    protected abstract val repository: R
}
