package org.tiqr.authenticator.auth

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

import org.tiqr.authenticator.datamodel.Identity
import org.tiqr.authenticator.datamodel.IdentityProvider

/**
 * Challenge base class.
 */
@Parcelize
open class Challenge(
        open val protocolVersion: String?,
        open val identityProvider: IdentityProvider,
        open var identity: Identity,
        open val returnURL: String?
): Parcelable
