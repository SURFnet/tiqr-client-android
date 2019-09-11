package org.tiqr.authenticator.auth

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.tiqr.authenticator.datamodel.Identity
import org.tiqr.authenticator.datamodel.IdentityProvider

/**
 * Represents an authentication challenge.
 */
@Parcelize
class AuthenticationChallenge (

        val sessionKey: String,
        /**
         * Sets the authentication challenge, used to verify the request (not to be confused with the
         * raw challenge!).
         *
         * @param challenge authentication challenge
         */
        val challenge: String,

        val serviceProviderDisplayName: String,
        val serviceProviderIdentifier: String,
        val isStepUpChallenge: Boolean = false,

        override val protocolVersion: String?,
        override val identityProvider: IdentityProvider,
        override var identity: Identity,
        override val returnURL: String?

) : Challenge(protocolVersion, identityProvider, identity, returnURL), Parcelable
