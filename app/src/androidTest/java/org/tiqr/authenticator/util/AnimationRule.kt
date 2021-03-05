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

package org.tiqr.authenticator.util

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.IOException

/**
 * Animation [TestRule] to automatically disable all animations before the test starts,
 * and enable them after the test completes.
 */
class AnimationsRule : TestRule {
    companion object {
        private const val CMD_TRANSITION_ANIMATION_SCALE = "settings put global transition_animation_scale"
        private const val CMD_WINDOW_ANIMATION_SCALE = "settings put global window_animation_scale"
        private const val CMD_ANIMATOR_DURATION_SCALE = "settings put global animator_duration_scale"
    }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                changeAnimationState(enable = false)
                try {
                    base.evaluate()
                } finally {
                    changeAnimationState(enable = true)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun changeAnimationState(enable: Boolean = true) {
        with(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())) {
            executeShellCommand("$CMD_TRANSITION_ANIMATION_SCALE ${enable.asInt()}")
            executeShellCommand("$CMD_WINDOW_ANIMATION_SCALE ${enable.asInt()}")
            executeShellCommand("$CMD_ANIMATOR_DURATION_SCALE ${enable.asInt()}")
        }
    }

    private fun Boolean.asInt(): Int = if (this) 1 else 0
}
