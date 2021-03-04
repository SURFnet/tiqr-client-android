package org.tiqr.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.tiqr.data.model.Identity
import org.tiqr.data.model.IdentityProvider
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class TiqrDatabaseTest {
    private lateinit var tiqrDb: TiqrDatabase
    private lateinit var tiqrDao: TiqrDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        tiqrDb = Room.inMemoryDatabaseBuilder(context, TiqrDatabase::class.java).build()
        tiqrDao = tiqrDb.tiqrDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() {
        tiqrDb.close()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun test_insert() {
        runBlockingTest {
            //Create identityprovider
            val identityProvider = IdentityProvider(
                    displayName = "demoProvider",
                    identifier = "1",
                    authenticationUrl = "https://demoProvider"
            )
            // Save identityprovider
            val id = tiqrDao.insertIdentityProvider(identityProvider)

            // Create identity
            val identity = Identity(
                    identifier = "1",
                    displayName = "demoIdentity",
                    identityProvider = id
            )

            // Save identity
            tiqrDao.insertIdentity(identity)

            // Get all identities
            val identities = tiqrDao.getIdentities()

            // Check if identifier matches
            Assert.assertEquals(identities.first().identifier, identity.identifier)
        }
    }

    // TODO: add more tests
}