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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.startsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.tiqr.authenticator.util.AnimationsRule
import org.tiqr.authenticator.util.matchNavHost
import org.tiqr.core.MainActivity
import org.tiqr.core.R

@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AboutTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Rule(order = 0)
    @JvmField
    val animationsRule = AnimationsRule()

    @Rule(order = 1)
    @JvmField
    var hiltRule = HiltAndroidRule(this)

    @Rule(order = 2)
    @JvmField
    val activityScenarioRule = activityScenarioRule<MainActivity>()

    @Test
    fun aboutTest() {
        activityScenarioRule.scenario

        // Find info button and click to start AboutFragment
        val info = onView(allOf(withContentDescription(context.getString(R.string.button_info)), isDisplayed()))
        info.perform(click())

        // Check if NavHost is displayed
        onView(matchNavHost()).check(matches(isDisplayed()))

        // Find version CardView and check text
        withId(R.id.app).also { versionCard ->
            onView(versionCard).check(matches(isDisplayed()))
            withParent(versionCard).also { versionText ->
                onView(versionText).check(matches(isDisplayed()))
                onView(versionText).check(matches(withText(startsWith(context.getString(R.string.app_name)))))
            }
        }

        // Find provider CardView and check text
        withId(R.id.provider).also { provider ->
            onView(provider).check(matches(isDisplayed()))
            withParent(provider).also { providerText ->
                onView(providerText).check(matches(isDisplayed()))
                onView(providerText).check(matches(withText(R.string.about_label_provided_by)))
            }
        }

        // Find developer CardView and check text
        withId(R.id.developer).also { developer ->
            onView(developer).check(matches(isDisplayed()))
            withParent(developer).also { developerText ->
                onView(developerText).check(matches(isDisplayed()))
                onView(developerText).check(matches(withText(R.string.about_label_developed_by)))
            }
        }

        // Find designer CardView and check text
        withId(R.id.designer).also { designer ->
            onView(designer).check(matches(isDisplayed()))
            withParent(designer).also { designerText ->
                onView(designerText).check(matches(isDisplayed()))
                onView(designerText).check(matches(withText(R.string.about_label_interaction_by)))
            }
        }
    }
}
