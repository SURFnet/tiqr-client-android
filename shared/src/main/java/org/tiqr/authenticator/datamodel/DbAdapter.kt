package org.tiqr.authenticator.datamodel

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.util.Log

import java.util.ArrayList

class DbAdapter(private val _ctx: Context) {

    private val TAG = DbAdapter::class.java.name

    private val _DBHelper: DatabaseHelper
    private val _db: SQLiteDatabase

    /**
     * Returns a cursor for all identities available in the system. The identities are ordered by their sort index.
     *
     * @return cursor object
     */
    val allIdentities: Cursor
        get() = _db.query(TABLE_IDENTITY, arrayOf(ROWID, DISPLAY_NAME, BLOCKED, IDENTIFIER, IDENTITYPROVIDER, SORT_INDEX, SHOW_FINGERPRINT_UPGRADE, USE_FINGERPRINT), null, null, null, null, SORT_INDEX)

    val allIdentitiesWithIdentityProviderData: Cursor
        get() {
            val builder = SQLiteQueryBuilder()
            builder.tables = JOIN_IDENTITY_IDENTITYPROVIDER

            return builder.query(_db, arrayOf("$TABLE_IDENTITY.$ROWID", "$TABLE_IDENTITY.$DISPLAY_NAME", "$TABLE_IDENTITY.$BLOCKED", "$TABLE_IDENTITY.$IDENTIFIER", "$TABLE_IDENTITY.$SORT_INDEX", "$TABLE_IDENTITY.$SHOW_FINGERPRINT_UPGRADE", "$TABLE_IDENTITY.$USE_FINGERPRINT", "$TABLE_IDENTITYPROVIDER.$LOGO"), null, null, null, null, SORT_INDEX)

        }

    init {
        _DBHelper = DatabaseHelper(_ctx)
        _db = _DBHelper.writableDatabase
    }

    private class DatabaseHelper internal constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE " + TABLE_IDENTITYPROVIDER + " (" + ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + DISPLAY_NAME + " TEXT NOT NULL, " + IDENTIFIER + " TEXT NOT NULL, " + AUTHENTICATION_URL + " TEXT NOT NULL, "
                    + OCRA_SUITE + " TEXT NOT NULL, " + INFO_URL + " TEXT, " + LOGO + " TEXT);")

            db.execSQL("CREATE TABLE " + TABLE_IDENTITY + " (" + ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + BLOCKED + " INTEGER NOT NULL DEFAULT 0, " + DISPLAY_NAME + " TEXT NOT NULL, " + IDENTIFIER + " TEXT NOT NULL, "
                    + IDENTITYPROVIDER + " INTEGER NOT NULL, " + SORT_INDEX + " INTEGER NOT NULL, " + SHOW_FINGERPRINT_UPGRADE + " INTEGER NOT NULL DEFAULT 1, " + USE_FINGERPRINT + " INTEGER NOT NULL DEFAULT 0);")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.w("DbAdapter", "Upgrading database from version $oldVersion to $newVersion, which will destroy identityprovider logo data")
            if (oldVersion == DB_VERSION_INITIAL && newVersion >= 6) {
                db.execSQL("ALTER TABLE $TABLE_IDENTITY ADD COLUMN $SHOW_FINGERPRINT_UPGRADE INTEGER NOT NULL DEFAULT 1; ")
                db.execSQL("ALTER TABLE $TABLE_IDENTITY ADD COLUMN $USE_FINGERPRINT INTEGER NOT NULL DEFAULT 0; ")
            }
            if (oldVersion <= 6 && newVersion >= 7) {
                // SQLLite doesn't allow ALTER COLUMN
                // Backup identity provider data.
                // Recreate table
                // Re-add backup data
                // ADD logo column (which is allowed)
                // Logo information is lost. But not all data is lost and users can still use their existing identities
                db.beginTransaction()
                try {
                    db.execSQL("CREATE TEMPORARY TABLE " + TABLE_IDENTITYPROVIDER + "_backup(" + ROWID + "," + DISPLAY_NAME + "," + IDENTIFIER + "," + AUTHENTICATION_URL + "," + OCRA_SUITE + "," + INFO_URL + ");")
                    db.execSQL("INSERT INTO " + TABLE_IDENTITYPROVIDER + "_backup SELECT " + ROWID + "," + DISPLAY_NAME + ", " + IDENTIFIER + "," + AUTHENTICATION_URL + "," + OCRA_SUITE + "," + INFO_URL + " FROM " + TABLE_IDENTITYPROVIDER + ";")
                    db.execSQL("DROP TABLE $TABLE_IDENTITYPROVIDER;")
                    db.execSQL("CREATE TABLE $TABLE_IDENTITYPROVIDER ($ROWID INTEGER PRIMARY KEY AUTOINCREMENT, $DISPLAY_NAME TEXT NOT NULL, $IDENTIFIER TEXT NOT NULL, $AUTHENTICATION_URL TEXT NOT NULL, $OCRA_SUITE TEXT NOT NULL, $INFO_URL TEXT);")
                    db.execSQL("INSERT INTO " + TABLE_IDENTITYPROVIDER + " SELECT " + ROWID + "," + DISPLAY_NAME + ", " + IDENTIFIER + "," + AUTHENTICATION_URL + "," + OCRA_SUITE + "," + INFO_URL + " FROM " + TABLE_IDENTITYPROVIDER + "_backup;")
                    db.execSQL("DROP TABLE " + TABLE_IDENTITYPROVIDER + "_backup")
                    db.execSQL("ALTER TABLE $TABLE_IDENTITYPROVIDER ADD COLUMN $LOGO TEXT;")
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        }
    }

    /**
     * Inserts an identity into the database.
     *
     * The identity object's id is automatically set based on the new database row id.
     *
     * @param identity Identity
     * @param ip IdentityProvider
     *
     * @return insertion successful?
     */
    fun insertIdentityForIdentityProvider(identity: Identity, ip: IdentityProvider): Boolean {
        val values = ContentValues()
        values.put(IDENTIFIER, identity.identifier)
        values.put(DISPLAY_NAME, identity.displayName)
        values.put(BLOCKED, if (identity.isBlocked) 1 else 0)
        values.put(IDENTITYPROVIDER, ip.id)
        values.put(SORT_INDEX, identity.sortIndex)
        values.put(SHOW_FINGERPRINT_UPGRADE, if (identity.showFingerprintUpgrade()) 1 else 0)
        values.put(USE_FINGERPRINT, if (identity.getUseFingerprint()) 1 else 0)

        val id = _db.insert(TABLE_IDENTITY, null, values)
        if (id != -1L) {
            identity.id = id
            return true
        } else {
            return false
        }
    }

    /**
     * Updates an existing identity.
     *
     * @param identity identity
     *
     * @return update successful?
     */
    fun updateIdentity(identity: Identity): Boolean {
        val values = ContentValues()
        values.put(IDENTIFIER, identity.identifier)
        values.put(DISPLAY_NAME, identity.displayName)
        values.put(BLOCKED, if (identity.isBlocked) 1 else 0)
        values.put(SORT_INDEX, identity.sortIndex)
        values.put(SHOW_FINGERPRINT_UPGRADE, if (identity.showFingerprintUpgrade()) 1 else 0)
        values.put(USE_FINGERPRINT, if (identity.getUseFingerprint()) 1 else 0)
        return _db.update(TABLE_IDENTITY, values, "$ROWID = ?", arrayOf(identity.id.toString())) > 0
    }

    /**
     * Deletes a particular identity.
     *
     * @param identityId identity row-id
     *
     * @return delete successful?
     */
    fun deleteIdentity(identityId: Long): Boolean {
        return _db.delete(TABLE_IDENTITY, "$ROWID = ?", arrayOf(identityId.toString())) > 0
    }

    /**
     * Convenience method to block all available identities
     *
     * @return The number of affected rows
     */
    fun blockAllIdentities(): Int {
        val values = ContentValues()
        values.put(BLOCKED, true)
        return _db.update(TABLE_IDENTITY, values, null, null)
    }

    fun createIdentityObjectForCurrentCursorPosition(cursor: Cursor): Identity {

        val rowIdColumn = cursor.getColumnIndex(DbAdapter.ROWID)
        val identifierColumn = cursor.getColumnIndex(DbAdapter.IDENTIFIER)
        val displayNameColumn = cursor.getColumnIndex(DbAdapter.DISPLAY_NAME)
        val blockedColumn = cursor.getColumnIndex(DbAdapter.BLOCKED)
        val sortIndexColumn = cursor.getColumnIndex(DbAdapter.SORT_INDEX)
        val showFingerprintUpgrade = cursor.getColumnIndex(DbAdapter.SHOW_FINGERPRINT_UPGRADE)
        val useFingerprint = cursor.getColumnIndex(DbAdapter.USE_FINGERPRINT)

        return Identity(
            id = cursor.getLong(rowIdColumn),
            identifier = cursor.getString(identifierColumn),
            displayName = cursor.getString(displayNameColumn),
            sortIndex = cursor.getInt(sortIndexColumn),
            isBlocked = if (cursor.getInt(blockedColumn) == 1) true else false,
            _showFingerprintUpgrade = if (cursor.getInt(showFingerprintUpgrade) == 1) true else false,
            isUsingFingerprint = if (cursor.getInt(useFingerprint) == 1) true else false
        )
    }

    /**
     * Create identity objects for the results of the given cursor.
     *
     * NOTE: this method closes the cursor when it's done!
     *
     * @param cursor database cursor
     *
     * @return Identity objects
     */
    private fun _createIdentityObjectsForCursor(cursor: Cursor): Array<Identity> {
        val identities = ArrayList<Identity>()

        if (cursor.moveToFirst()) {

            do {
                val identity = createIdentityObjectForCurrentCursorPosition(cursor)
                identities.add(identity)
            } while (cursor.moveToNext())
        }

        cursor.close()

        return identities.toTypedArray()
    }

    /**
     * Returns the identity with the given identifier and identity provider.
     *
     * @param identityIdentifier identity identifier
     * @param identityProviderIdentifier identityIProvider identifier
     *
     * @return cursor object
     */
    @Throws(SQLException::class)
    fun getIdentityByIdentifierAndIdentityProviderIdentifier(identityIdentifier: String, identityProviderIdentifier: String): Cursor? {
        val builder = SQLiteQueryBuilder()
        builder.tables = JOIN_IDENTITY_IDENTITYPROVIDER

        val cursor = builder.query(_db, arrayOf("$TABLE_IDENTITY.$ROWID", "$TABLE_IDENTITY.$DISPLAY_NAME", "$TABLE_IDENTITY.$BLOCKED", "$TABLE_IDENTITY.$IDENTIFIER", "$TABLE_IDENTITY.$IDENTITYPROVIDER", "$TABLE_IDENTITY.$SORT_INDEX", "$TABLE_IDENTITY.$SHOW_FINGERPRINT_UPGRADE", "$TABLE_IDENTITY.$USE_FINGERPRINT"),
                "$TABLE_IDENTITY.$IDENTIFIER = ? AND $TABLE_IDENTITYPROVIDER.$IDENTIFIER = ?", arrayOf(identityIdentifier, identityProviderIdentifier), null, null, SORT_INDEX)

        cursor?.moveToFirst()

        return cursor
    }

    fun getIdentityByIdentityId(identity_id: Long): Identity? {
        val builder = SQLiteQueryBuilder()
        builder.tables = JOIN_IDENTITY_IDENTITYPROVIDER

        val cursor = builder.query(_db, arrayOf("$TABLE_IDENTITY.$ROWID", "$TABLE_IDENTITY.$DISPLAY_NAME", "$TABLE_IDENTITY.$BLOCKED", "$TABLE_IDENTITY.$IDENTIFIER", "$TABLE_IDENTITY.$SORT_INDEX", "$TABLE_IDENTITY.$SHOW_FINGERPRINT_UPGRADE", "$TABLE_IDENTITY.$USE_FINGERPRINT", "$TABLE_IDENTITYPROVIDER.$LOGO"), "$TABLE_IDENTITY.$ROWID = ?", arrayOf(identity_id.toString()), null, null, SORT_INDEX)

        val identities = _createIdentityObjectsForCursor(cursor)
        return if (identities.size > 0) {
            identities[0]
        } else null

    }

    /**
     * Returns the identity with the given identifier and identity provider identifier as an identity provider object.
     *
     * @param identifier identity identifier
     * @param identityProviderIdentifier identity provider identifier
     *
     * @return identity provider object or null if identity provider is unknown
     */
    fun getIdentityByIdentifierAndIdentityProviderIdentifierAsObject(identifier: String, identityProviderIdentifier: String): Identity? {
        val identities = _createIdentityObjectsForCursor(getIdentityByIdentifierAndIdentityProviderIdentifier(identifier, identityProviderIdentifier)!!)
        return if (identities.size == 1) identities[0] else null
    }

    /**
     * Count how many identities there are in the database./
     *
     * @return cursor object
     */
    fun identityCount(): Int {

        val cursor = allIdentities
        val result = cursor.count
        cursor.close()
        return result
    }

    /**
     * Returns the identities for the given identity provider ordered by their sort index.
     *
     * Filter out the identities which are blocked, because this is only used for authentication
     *
     * @param identityProviderId Identity provider row-id
     *
     * @return result cursor
     */
    @Throws(SQLException::class)
    fun findIdentitiesByIdentityProviderIdWithIdentityProviderData(identityProviderId: Long): Cursor? {
        val builder = SQLiteQueryBuilder()
        builder.tables = JOIN_IDENTITY_IDENTITYPROVIDER

        val cursor = builder.query(_db, arrayOf("$TABLE_IDENTITY.$ROWID", "$TABLE_IDENTITY.$DISPLAY_NAME", "$TABLE_IDENTITY.$BLOCKED", "$TABLE_IDENTITY.$IDENTIFIER", "$TABLE_IDENTITY.$SORT_INDEX", "$TABLE_IDENTITY.$SHOW_FINGERPRINT_UPGRADE", "$TABLE_IDENTITY.$USE_FINGERPRINT", "$TABLE_IDENTITYPROVIDER.$LOGO"), "$IDENTITYPROVIDER = ? AND $TABLE_IDENTITY.$BLOCKED <> 1", arrayOf(identityProviderId.toString()), null, null, SORT_INDEX)

        cursor?.moveToFirst()

        return cursor
    }

    /**
     * Returns the identities for the given identity provider ordered by their sort index.
     *
     * @param identityProviderIdentifier identity provider identifier
     *
     * @return result cursor
     */
    @Throws(SQLException::class)
    fun findIdentitiesByIdentityProviderIdentifier(identityProviderIdentifier: String): Cursor? {
        val builder = SQLiteQueryBuilder()
        builder.tables = JOIN_IDENTITY_IDENTITYPROVIDER

        val cursor = builder.query(_db, arrayOf("$TABLE_IDENTITY.$ROWID", "$TABLE_IDENTITY.$DISPLAY_NAME", "$TABLE_IDENTITY.$BLOCKED", "$TABLE_IDENTITY.$IDENTIFIER", "$TABLE_IDENTITY.$IDENTITYPROVIDER", "$TABLE_IDENTITY.$SORT_INDEX", "$TABLE_IDENTITY.$SHOW_FINGERPRINT_UPGRADE", "$TABLE_IDENTITY.$USE_FINGERPRINT", "$TABLE_IDENTITYPROVIDER.$LOGO"), "$TABLE_IDENTITYPROVIDER.$IDENTIFIER = ?", arrayOf(identityProviderIdentifier), null, null, SORT_INDEX)

        cursor?.moveToFirst()

        return cursor
    }

    /**
     * Returns the identities for the given identity provider ordered by their sort index and returns them as an array of Identity objects.
     *
     * @param identityProviderIdentifier identity provider identifier
     *
     * @return result array
     */
    fun findIdentitiesByIdentityProviderIdentifierAsObjects(identityProviderIdentifier: String): Array<Identity> {
        try {
            return _createIdentityObjectsForCursor(findIdentitiesByIdentityProviderIdentifier(identityProviderIdentifier)!!)
        } catch (ex: Exception) {
            ex.printStackTrace()
            return arrayOf<Identity>()
        }

    }

    /**
     * Inserts an identity provider into the database.
     *
     * TODO: logo
     *
     * The identity provider object's id is automatically set based on the new database row id.
     *
     * @param identityProvider The identity provider
     *
     * @return insertion successful?
     */
    fun insertIdentityProvider(identityProvider: IdentityProvider): Boolean {
        val values = ContentValues()
        values.put(IDENTIFIER, identityProvider.identifier)
        values.put(DISPLAY_NAME, identityProvider.displayName)
        values.put(AUTHENTICATION_URL, identityProvider.authenticationURL)
        values.put(OCRA_SUITE, identityProvider.ocraSuite)
        values.put(LOGO, identityProvider.logoURL)
        values.put(INFO_URL, identityProvider.infoURL)

        val id = _db.insert(TABLE_IDENTITYPROVIDER, null, values)
        if (id != -1L) {
            identityProvider.id = id
            return true
        } else {
            return false
        }
    }

    /**
     * Deletes a particular identity provider.
     *
     * @param identityProviderId The identity provider row-id
     *
     * @return delete successful?
     */
    fun deleteIdentityProvider(identityProviderId: Long): Boolean {
        return _db.delete(TABLE_IDENTITYPROVIDER, "$ROWID = ?", arrayOf(identityProviderId.toString())) > 0
    }

    /**
     * Create identity provider objects for the results of the given cursor.
     *
     * NOTE: this method closes the cursor when it's done!
     *
     * @param cursor database cursor
     *
     * @return IdentityProvider objects
     */
    private fun _createIdentityProviderObjectsForCursor(cursor: Cursor): Array<IdentityProvider> {
        val identityproviders = ArrayList<IdentityProvider>()

        if (cursor.moveToFirst()) {
            val rowIdColumn = cursor.getColumnIndex(DbAdapter.ROWID)
            val identifierColumn = cursor.getColumnIndex(DbAdapter.IDENTIFIER)
            val displayNameColumn = cursor.getColumnIndex(DbAdapter.DISPLAY_NAME)
            val authURLColumn = cursor.getColumnIndex(DbAdapter.AUTHENTICATION_URL)
            val ocraSuiteColumn = cursor.getColumnIndex(DbAdapter.OCRA_SUITE)
            val logoColumn = cursor.getColumnIndex(DbAdapter.LOGO)
            val infoURLColumn = cursor.getColumnIndex(DbAdapter.INFO_URL)

            do {
                val ip = IdentityProvider(
                    id = cursor.getInt(rowIdColumn).toLong(),
                    identifier = cursor.getString(identifierColumn),
                    displayName = cursor.getString(displayNameColumn),
                    logoURL = cursor.getString(logoColumn),
                    authenticationURL = cursor.getString(authURLColumn),
                    infoURL = cursor.getString(infoURLColumn)
                )
                ip.ocraSuite = cursor.getString(ocraSuiteColumn)
                identityproviders.add(ip)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return identityproviders.toTypedArray()
    }

    /**
     * Returns the identity provider with the given identifier.
     *
     * @param identifier identity provider identifier
     *
     * @return cursor object for identity provider
     */
    @Throws(SQLException::class)
    fun getIdentityProviderByIdentifier(identifier: String): Cursor? {
        val cursor = _db.query(TABLE_IDENTITYPROVIDER, arrayOf(ROWID, DISPLAY_NAME, IDENTIFIER, AUTHENTICATION_URL, OCRA_SUITE, LOGO, INFO_URL), "$IDENTIFIER = ?", arrayOf(identifier), null, null, null)

        cursor?.moveToFirst()

        return cursor
    }

    /**
     * Return the identity provider for a given identity (identified by its id)
     *
     * @param identity_id The identity id
     * @return
     */
    fun getIdentityProviderForIdentityId(identity_id: Long): IdentityProvider? {
        val builder = SQLiteQueryBuilder()
        builder.tables = JOIN_IDENTITY_IDENTITYPROVIDER

        val cursor = builder.query(_db, arrayOf("$TABLE_IDENTITYPROVIDER.$ROWID", "$TABLE_IDENTITYPROVIDER.$DISPLAY_NAME", "$TABLE_IDENTITYPROVIDER.$IDENTIFIER", "$TABLE_IDENTITYPROVIDER.$AUTHENTICATION_URL", "$TABLE_IDENTITYPROVIDER.$OCRA_SUITE", "$TABLE_IDENTITYPROVIDER.$INFO_URL", "$TABLE_IDENTITYPROVIDER.$LOGO"), "$TABLE_IDENTITY.$ROWID = ?",
                arrayOf(identity_id.toString()), null, null, SORT_INDEX)

        val identityProviders = _createIdentityProviderObjectsForCursor(cursor)
        return if (identityProviders.size > 0) {
            identityProviders[0]
        } else null

    }

    /**
     * Returns the identity provider with the given identifier as an identityprovider object.
     *
     * @param identifier identity provider identifier
     *
     * @return identity provider object or null if identity provider is unknown
     */
    fun getIdentityProviderByIdentifierAsObject(identifier: String): IdentityProvider? {
        try {
            val identityproviders = _createIdentityProviderObjectsForCursor(getIdentityProviderByIdentifier(identifier)!!)
            return if (identityproviders.size > 0) identityproviders[0] else null
        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        }

    }

    companion object {
        val ROWID = "_id"
        val DISPLAY_NAME = "displayName"
        val BLOCKED = "blocked"
        val IDENTIFIER = "identifier"
        val SORT_INDEX = "sortIndex"
        val SHOW_FINGERPRINT_UPGRADE = "showFingerPrintUpgrade"
        val USE_FINGERPRINT = "useFingerPrint"
        val IDENTITYPROVIDER = "identityProvider"
        val LOGO = "logo"
        val INFO_URL = "infoUrl"
        val AUTHENTICATION_URL = "authenticationUrl"
        val OCRA_SUITE = "ocraSuite"

        private val DATABASE_NAME = "identities.db"
        private val TABLE_IDENTITY = "identity"
        private val TABLE_IDENTITYPROVIDER = "identityprovider"

        private val JOIN_IDENTITY_IDENTITYPROVIDER = "$TABLE_IDENTITY LEFT JOIN $TABLE_IDENTITYPROVIDER ON $TABLE_IDENTITY.$IDENTITYPROVIDER = $TABLE_IDENTITYPROVIDER.$ROWID"

        private val DATABASE_VERSION = 7
        private val DB_VERSION_INITIAL = 4
    }
}