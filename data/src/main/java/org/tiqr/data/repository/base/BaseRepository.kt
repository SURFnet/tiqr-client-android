package org.tiqr.data.repository.base

import org.tiqr.data.model.Challenge

abstract class BaseRepository<T: Challenge> {
    /**
     * The scheme to distinguish between challenge types.
     */
    protected abstract val challengeScheme: String

    /**
     * Contains a valid challenge?
     */
    protected fun isValidChallenge(rawChallenge: String) = rawChallenge.startsWith(challengeScheme)

    /**
     * Parse the raw challenge.
     */
    abstract fun parseChallenge(rawChallenge: String): T

    /**
     * Validate the parsed challenge
     */
    abstract fun validateChallenge(challenge: T, password: String)
}