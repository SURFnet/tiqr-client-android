/*
 * Copyright (c) 2010-2021 SURFnet bv
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of SURFnet bv nor the names of its contributors
 *    may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.tiqr.authenticator

import android.content.Intent
import android.net.Uri
import androidx.appcompat.R
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.tiqr.authenticator.util.AnimationsRule
import org.tiqr.core.MainActivity

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AuthenticateTest {
    private lateinit var url: String

    @Rule(order = 0)
    @JvmField
    val animationsRule = AnimationsRule()

    @Rule(order = 1)
    @JvmField
    var hiltRule = HiltAndroidRule(this)

    @Rule(order = 2)
    @JvmField
    val activityRule = ActivityTestRule(MainActivity::class.java, true, false)


    @Before
    fun setup() {
        url = "tiqrauth://debug.tiqr.org/f1bdfc0808520bff758f301cb15a26e13a526f1eafdd6da5a5b738bdb8746cb2/d4f84c9b74/debug.tiqr.org/2"
        // TODO: should be retrieved from hosted tiqr-cli
    }

    @Test
    fun enroll() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activityRule.launchActivity(intent)

        // Check if authenticate url had errors
        Espresso.onView(ViewMatchers.withId(R.id.alertTitle))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        Espresso.onView(ViewMatchers.withId(android.R.id.button1))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())

        // Otherwise continue

        // Check if screen is now on AuthenticationConfirm
        // Click OK

        // Check if screen is now on AuthenticationPin
        // Enter correct pin
        // Click OK

        // Check if screen is now on AuthenticationSummary
        // Click OK

        // Done
    }
}