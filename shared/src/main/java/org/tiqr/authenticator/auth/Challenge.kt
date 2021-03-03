package org.tiqr.authenticator.auth

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

import org.tiqr.authenticator.datamodel.Identity
import org.tiqr.authenticator.datamodel.IdentityProvider

/**
 * Challenge base class.
 */
@Parcelize
open class Challenge(
        open val protocolVersion: String?,
        open val identityProvider: IdentityProvider,
        open val identity: Identity?,
        open val returnURL: String?
): Parcelable
