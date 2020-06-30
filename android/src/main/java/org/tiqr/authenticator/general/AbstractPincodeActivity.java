package org.tiqr.authenticator.general;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.tiqr.authenticator.R;
import org.tiqr.authenticator.security.Verhoeff;

import java.util.Arrays;
import java.util.List;

/**
 * Pin code screen.
 */
abstract public class AbstractPincodeActivity extends AbstractAuthenticationActivity {

    protected TextView title;
    protected TextView intro;
    protected TextView pintHint;

    protected EditText pincode;
    protected TextView pin1;
    protected TextView pin2;
    protected TextView pin3;
    protected TextView pin4;
    protected List<TextView> pincodes;
    protected Button btn_ok;

    /**
     * Create activity.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pincode);

        _setUIElements();

        // Create a TextChangedListener for the hidden  pincode field
        pincode.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                _updateFakePins();
            }
        });
    }

    private void _setUIElements() {
        title = findViewById(R.id.title);
        intro = findViewById(R.id.intro_label);

        pincode = findViewById(R.id.pincode);
        pin1 = findViewById(R.id.pin1Field);
        pin2 = findViewById(R.id.pin2Field);
        pin3 = findViewById(R.id.pin3Field);
        pin4 = findViewById(R.id.pin4Field);
        btn_ok = findViewById(R.id.ok_button);
        pintHint = findViewById(R.id.pinHint);
        pincodes  = Arrays.asList(pin1, pin2, pin3, pin4);

        HeaderView headerView = findViewById(R.id.headerView);
        headerView.setOnLeftClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                process();
            }
        });
    }

    private void _updateFakePins() {
        String pincodeText = pincode.getText().toString();
        int index = pincodeText.length() - 1;

        for (int i = 0; i < pincodes.size(); ++i) {
            final TextView fakePinView = pincodes.get(i);
            final String code = getPinChar(pincodeText, i);
            if (i == index) {
                fakePinView.setText(code);
            } else if (i > index) {
                fakePinView.setText(null);
            }
        }
        btn_ok.setEnabled(pincodeText.length() == 4);
    }

    @Nullable
    private String getPinChar(String code, int index) {
        try {
            return String.valueOf(code.charAt(index));
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        _initHiddenPincodeField();
    }

    protected void _initHiddenPincodeField() {
        pincode.post(new Runnable() {
            public void run() {
                pincode.requestFocusFromTouch();
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(pincode, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    protected void _hideSoftKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    /**
     * What should happen when the user clicks the ok button
     */
    abstract public void process();

    /**
     * Return an interpreted Verhoeff checksum for a given string
     *
     * @param pin
     * @return
     */
    private String _verificationCharForPin(String pin) {
        String table = "$',^onljDP";
        int location = Verhoeff.INSTANCE.verhoeffDigit(pin);
        return table.substring(location, location + 1);
    }

    /**
     * Loops through all EditText views inside an activity and clears them
     */
    protected void _clear() {
        pincode.setText("");
        pin1.setText("");
        pin2.setText("");
        pin3.setText("");
        pin4.setText("");
        btn_ok.setEnabled(false);
    }

    /**
     * Set intro text, html format supported
     *
     * @param resourceId string id
     */
    protected void setIntoText(int resourceId) {
        intro.setText(Html.fromHtml(getString(resourceId)));
    }
}