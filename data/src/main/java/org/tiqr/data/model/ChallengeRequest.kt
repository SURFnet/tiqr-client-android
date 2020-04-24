package org.tiqr.data.model

import org.tiqr.data.service.SecretService

/**
 * Sealed class for passing parameters to complete the [Challenge]
 */
sealed class ChallengeCompleteRequest<T : Challenge> {
    abstract val challenge: T
    abstract val password: String
}

class EnrollmentCompleteRequest<T : Challenge>(
        override val challenge: T,
        override val password: String
) : ChallengeCompleteRequest<T>()

class AuthenticationCompleteRequest<T : Challenge>(
        override val challenge: T,
        override val password: String,
        val type: SecretService.Type
) : ChallengeCompleteRequest<T>()
