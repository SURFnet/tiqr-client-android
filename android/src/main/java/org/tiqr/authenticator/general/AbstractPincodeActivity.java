package org.tiqr.authenticator.general;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.tiqr.authenticator.R;
import org.tiqr.authenticator.security.Verhoeff;

import java.util.ArrayList;
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

    protected Button btn_ok;

    protected Handler fadeAnimalHander = new Handler();

    Typeface tf_default;
    Typeface tf_animals;

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

        tf_default = Typeface.defaultFromStyle(Typeface.NORMAL);
        tf_animals = Typeface.createFromAsset(getAssets(), "fonts/animals.ttf");

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
        fadeAnimalHander.removeCallbacksAndMessages(null);
        String pincodeText = pincode.getText().toString();
        int animalIndex = pincodeText.length() - 1;
        List<TextView> pincodes = Arrays.asList(pin1, pin2, pin3, pin4);
        for (int i = 0; i < pincodes.size(); ++i) {
            final TextView fakePinView = pincodes.get(i);
            if (i == animalIndex) {
                fakePinView.setTypeface(tf_animals);
                fakePinView.setText(_verificationCharForPin(pincodeText));
                fakePinView.setTransformationMethod(SingleLineTransformationMethod.getInstance());
                fadeAnimalHander.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fakePinView.setTypeface(tf_default);
                        fakePinView.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        fakePinView.setText("x");
                    }
                }, 2000);
            } else if (i < pincodeText.length()){
                fakePinView.setText("x");
                fakePinView.setTypeface(tf_default);
                fakePinView.setTransformationMethod(PasswordTransformationMethod.getInstance());
            } else {
                fakePinView.setText("");
            }
        }
        btn_ok.setEnabled(pincodeText.length() == 4);

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
        int location = Verhoeff.verhoeffDigit(pin);
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