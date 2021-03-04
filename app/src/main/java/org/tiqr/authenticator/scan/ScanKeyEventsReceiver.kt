/*
 * Copyright (c) 2010-2019 SURFnet bv
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

package org.tiqr.authenticator.scan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.KeyEvent

/**
 * Broadcast receiver to handle key events while scanning.
 */
class ScanKeyEventsReceiver(private val torch: (enable: Boolean) -> Unit) : BroadcastReceiver() {
    companion object {
        private const val KEY_EVENT_ACTION = "key_event_action"
        private const val KEY_EVENT_EXTRA = "key_event_extra"

        val filter = IntentFilter().apply { addAction(KEY_EVENT_ACTION) }

        /**
         * Create the key event [Intent]
         */
        fun createEvent(keyCode: Int): Intent = Intent(KEY_EVENT_ACTION).apply {
            putExtra(KEY_EVENT_EXTRA, keyCode)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.getIntExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)) {
            KeyEvent.KEYCODE_FOCUS,
            KeyEvent.KEYCODE_CAMERA -> { } // handled to prevent opening the camera app
            KeyEvent.KEYCODE_VOLUME_UP -> torch.invoke(true)
            KeyEvent.KEYCODE_VOLUME_DOWN -> torch.invoke(false)
        }
    }
}