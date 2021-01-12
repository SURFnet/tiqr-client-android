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