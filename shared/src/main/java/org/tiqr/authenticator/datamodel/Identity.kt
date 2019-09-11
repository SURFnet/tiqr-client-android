package org.tiqr.authenticator.datamodel

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Identity wrapper class for identities.
 */
@Parcelize
class Identity(
        /**
         * Sets the identity row id.
         *
         * The id is -1 for an identity that hasn't bee inserted yet.
         *
         * @param id row id
         */
        var id: Long = -1,
        val identifier : String,
        val displayName : String? = null,
        val sortIndex : Int = 0,
        var isBlocked : Boolean = false,
        var _showFingerprintUpgrade : Boolean = true,
        var isUsingFingerprint : Boolean = false

) : Parcelable {

    val isNew: Boolean
        get() = id == -1L
}