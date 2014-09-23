package org.tiqr.glass.general;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import com.google.android.glass.widget.CardBuilder;

/**
 * Displays an error.
 */
public class ErrorActivity extends Activity {
    private final static String MESSAGE = "MESSAGE";

    /**
     * Create intent.
     */
    public static Intent createIntent(Context context, String message) {
        Intent intent = new Intent(context, ErrorActivity.class);
        intent.putExtra(MESSAGE, message);
        return intent;
    }

    /**
     * Create.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String message = getIntent().getStringExtra(MESSAGE);

        View cardView =
            new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText(message)
                .setFootnote("Swipe to go back")
                .getView();

        setContentView(cardView);
    }

    /**
     * Handle gestures.
\     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
