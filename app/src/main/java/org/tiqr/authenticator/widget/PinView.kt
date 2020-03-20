/*
 * Copyright (c) 2010-2020 SURFnet bv
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

package org.tiqr.authenticator.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.text.*
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.autofill.AutofillValue
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.autofill.HintConstants
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import androidx.core.os.postDelayed
import org.tiqr.authenticator.R
import org.tiqr.data.algorithm.Verhoeff.generateVerhoeff

/**
 * This composite view advertises itself as a text editor,
 * handles the pin code input and supports auto fill.
 */
class PinView : ConstraintLayout {
    companion object {
        private const val CHAR_X = "X"
        private const val CHAR_EMPTY = ""

        private const val PIN_LENGTH = 4
        private const val PIN_FADE_DURATION = 1_500L
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        context.withStyledAttributes(attrs, R.styleable.PinView) {
            isForNewPin = getInt(R.styleable.PinView_pinInputType, 0) == 1
        }
    }

    private var isForNewPin: Boolean = false
    private val pinInput: Editable
    private val ok: Button
    private val pins: List<TextView>

    private val fadeHandler = Handler()
    private val typefaceDefault = Typeface.defaultFromStyle(Typeface.NORMAL)
    private val typefaceAnimals = if (isInEditMode) typefaceDefault else ResourcesCompat.getFont(context, R.font.animals)

    private val inputMethodManager = context.getSystemService<InputMethodManager>()
    private val gestureDetector = GestureDetector(context, PinGestureDetector { focusAndShowKeyboard() })
    private val inputWatcher = PinInputWatcher { updatePinDisplay() }
    private var showKeyboardDelayed = false
    private var onConfirmListener: ((String) -> Unit)? = null

    init {
        View.inflate(context, R.layout.view_pin, this)

        isClickable = true
        isLongClickable = true
        isFocusable = true
        isFocusableInTouchMode = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
            // We don't want the default ripple background
            defaultFocusHighlightEnabled = false
        }

        pinInput = Editable.Factory.getInstance().newEditable("")
        pinInput.filters += InputFilter.LengthFilter(PIN_LENGTH)
        pinInput.setSpan(inputWatcher, 0, pinInput.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        pins = listOf(
                findViewById(R.id.pin1),
                findViewById(R.id.pin2),
                findViewById(R.id.pin3),
                findViewById(R.id.pin4)
        )

        pins.forEach {
            it.isFocusable = false
            it.isFocusableInTouchMode = false
            it.isClickable = false
            it.isLongClickable = false
        }

        ok = findViewById(R.id.pin_ok)
        ok.setOnClickListener {
            if (it.isEnabled) {
                inputMethodManager?.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
                onConfirmListener?.invoke(pinInput.toString())
            }
        }

        setOnTouchListener(PinTouchListener(gestureDetector))

        if (isInEditMode) {
            pins.last().isSelected = true
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        post {
            updatePinDisplay()
            focusAndShowKeyboard()
        }
    }

    /**
     * Clear pin.
     */
    fun clear(showKeyboard: Boolean = true) {
        fadeHandler.removeCallbacksAndMessages(null)
        pinInput.clear()
        pins.forEach {
            it.text = CHAR_EMPTY
        }
        ok.isEnabled = false

        if (showKeyboard) {
            focusAndShowKeyboard()
        }
    }

    /**
     * Set the listener for the OK button click
     */
    fun setConfirmListener(listener: (pin: String) -> Unit) {
        onConfirmListener = listener
    }

    /**
     * Update the pins after text changes.
     */
    private fun updatePinDisplay() {
        fun verificationCharForPin(pin: String): String {
            val table = "$',^onljDP"
            val location = pin.generateVerhoeff()
            return table.substring(location, location + 1)
        }

        fadeHandler.removeCallbacksAndMessages(null)

        val pin = pinInput.toString()
        val animalIndex = pin.lastIndex

        pins.forEachIndexed { index, textView ->
            when {
                index == animalIndex -> {
                    textView.isSelected = false
                    textView.text = verificationCharForPin(pin)
                    textView.typeface = typefaceAnimals
                    textView.transformationMethod = SingleLineTransformationMethod.getInstance()
                    fadeHandler.postDelayed(PIN_FADE_DURATION) {
                        textView.text = CHAR_X
                        textView.typeface = typefaceDefault
                        textView.transformationMethod = PasswordTransformationMethod.getInstance()
                    }
                }
                index < pin.length -> {
                    textView.isSelected = false
                    textView.text = CHAR_X
                    textView.typeface = typefaceDefault
                    textView.transformationMethod = PasswordTransformationMethod.getInstance()

                }
                else -> {
                    textView.text = CHAR_EMPTY
                    textView.isSelected = index == animalIndex + 1
                }
            }
        }

        ok.isEnabled = pin.length == PIN_LENGTH
    }

    /**
     * Focus this view and open the keyboard.
     */
    private fun focusAndShowKeyboard() {
        requestFocus()
        showKeyboardDelayed = true
        maybeShowKeyboard()
    }

    /**
     * Handle the reliably showing the keyboard.
     */
    private fun maybeShowKeyboard() {
        if (hasWindowFocus() && showKeyboardDelayed) {
            if (isFocused) {
                post {
                    inputMethodManager?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                }
            }
            showKeyboardDelayed = false
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        maybeShowKeyboard()
    }

    override fun onCheckIsTextEditor() = true

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection {
        outAttrs?.apply {
            // Setup the keyboard
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            imeOptions = EditorInfo.IME_ACTION_DONE
        }

        return PinInputConnection(this, true, pinInput) { ok.performClick() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getAutofillType(): Int = View.AUTOFILL_TYPE_TEXT

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getAutofillHints(): Array<String> = arrayOf(
            if (isForNewPin) {
                HintConstants.AUTOFILL_HINT_NEW_PASSWORD
            } else {
                HintConstants.AUTOFILL_HINT_PASSWORD
            }
    )

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getAutofillValue(): AutofillValue = AutofillValue.forText(pinInput.toString())

    @RequiresApi(Build.VERSION_CODES.O)
    override fun autofill(value: AutofillValue?) {
        if (value?.isText != true) {
            return
        }

        val text = value.textValue
        if (text.isNotEmpty()) {
            pinInput.clear()
            pinInput.insert(0, text)
        }
    }

    /**
     * [InputConnection] to handle key presses from numeric keyboard
     */
    class PinInputConnection(
            target: View,
            full: Boolean,
            private val pin: Editable,
            private val ok: () -> Unit
    ) : BaseInputConnection(target, full) {
        override fun getEditable() = pin

        override fun sendKeyEvent(event: KeyEvent?): Boolean {
            event?.apply {
                if (action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_ENTER -> {
                            ok()
                        }
                        KeyEvent.KEYCODE_DEL -> {
                            if (pin.isNotEmpty()) {
                                pin.delete(pin.lastIndex, pin.length)
                            }
                        }
                        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                            val number = event.keyCharacterMap.getNumber(event.keyCode)
                            pin.append(number.toString())
                        }
                    }
                }
            }
            return super.sendKeyEvent(event)
        }
    }

    /**
     * [TextWatcher] to replace typed characters with animal characters
     */
    class PinInputWatcher(private val onChanged: () -> Unit) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) = onChanged.invoke()
    }

    /**
     * [GestureDetector] to handle clicks
     */
    class PinGestureDetector(private val onTap: () -> Unit) : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            onTap.invoke()
            return true
        }
    }

    /**
     * [View.OnTouchListener] to delegate events to [PinGestureDetector]
     */
    class PinTouchListener(private val gestureDetector: GestureDetector) : OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            return gestureDetector.onTouchEvent(event)
        }
    }
}