package org.tiqr.authenticator.datamodel

import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

/**
 * Simple wrapper class for services.
 */
@Parcelize
class IdentityProvider (

    /**
     * The id is -1 for a service that hasn't been inserted yet.
     */
    var id: Long = -1L,

    val identifier: String,
    val displayName: String,
    val logoURL: String,
    val authenticationURL: String,
    val infoURL: String

) : Parcelable {

    companion object {
        // The default version is compatible with old moby dick servers that don't specify a
        // suite. Default is to use an SHA1 hash
        val DEFAULT_OCRA_SUITE = "OCRA-1:HOTP-SHA1-6:QN10"
    }

    var ocraSuite: String = DEFAULT_OCRA_SUITE
}
