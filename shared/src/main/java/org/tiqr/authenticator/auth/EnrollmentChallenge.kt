package org.tiqr.authenticator.auth

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.tiqr.authenticator.datamodel.Identity
import org.tiqr.authenticator.datamodel.IdentityProvider

/**
 * Represents an enrollment challenge.
 */
@Parcelize
class EnrollmentChallenge(

        val enrollmentURL: String,

        override val protocolVersion: String?,
        override val identityProvider: IdentityProvider,
        override var identity: Identity,
        override val returnURL: String?

) : Challenge(protocolVersion, identityProvider, identity, returnURL), Parcelable