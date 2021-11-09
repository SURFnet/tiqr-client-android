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
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
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
class EnrollTest {
    private lateinit var url: String

    @Rule(order = 0)
    @JvmField
    val animationsRule = AnimationsRule()

    @Rule(order = 1)
    @JvmField
    val hiltRule = HiltAndroidRule(this)

    @Rule(order = 2)
    @JvmField
    val activityRule = ActivityTestRule(MainActivity::class.java, true, false)

    @Before
    fun setup() {
        url = "tiqrenroll://http://localhost:8080/tiqr?key=bb11a12fa4f7eeba1ce1d2d3569b21ad5e76e3dc5de08fe88eece8a440cce808"
        // TODO: should be retrieved from hosted tiqr-cli
    }

    @Test
    fun enroll() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activityRule.launchActivity(intent)

        // Check if enroll url had errors
        onView(withId(androidx.appcompat.R.id.alertTitle))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))

        onView(withId(android.R.id.button1))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click())

        // Otherwise continue

        // Check if screen is now on EnrollConfirm
        // Click OK

        // Check if screen is now on EnrollPin
        // Enter new pin
        // Click OK

        // Check if screen is now on EnrollPinConfirm
        // Enter confirm pin
        // Click OK

        // Check if screen is now on EnrollSummary
        // Click OK

        // Done
    }
}