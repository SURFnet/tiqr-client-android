package org.tiqr.authenticator.general;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.tiqr.authenticator.R;

public class ErrorActivity extends Activity {

    private static final String INTENT_KEY_TITLE = "org.tiqr.error.title";
    private static final String INTENT_KEY_MESSAGE = "org.tiqr.error.message";
    private static final String INTENT_KEY_EXCEPTION = "org.tiqr.error.exception";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.error);

        String title = getIntent().getStringExtra(INTENT_KEY_TITLE);
        String message = getIntent().getStringExtra(INTENT_KEY_MESSAGE);

        if (getIntent().hasExtra(INTENT_KEY_EXCEPTION)) {
            Exception exception = (Exception) getIntent().getSerializableExtra(INTENT_KEY_EXCEPTION);
            final TextView technicalDetailsText = findViewById(R.id.technical_details_text);
            technicalDetailsText.setText(exception.toString());
            final Button showTechnicalDetailsButton = findViewById(R.id.show_technical_details_button);
            showTechnicalDetailsButton.setVisibility(View.VISIBLE);
            showTechnicalDetailsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTechnicalDetailsButton.setVisibility(View.GONE);
                    technicalDetailsText.setVisibility(View.VISIBLE);
                }
            });
        }

        HeaderView headerView = (HeaderView) findViewById(R.id.headerView);
        headerView.setOnLeftClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        headerView.hideRightButton();

        FooterView footer = (FooterView) findViewById(R.id.footerView);
        footer.hideInfoIcon();

        TextView titleField = (TextView) findViewById(R.id.error_title);
        titleField.setText(title);

        TextView messageField = (TextView) findViewById(R.id.error_message);
        messageField.setText(message);
    }

    public static class ErrorBuilder {
        private String _title;
        private String _message;
        private Exception _exception;

        public ErrorBuilder setTitle(String title) {
            _title = title;
            return this;
        }

        public ErrorBuilder setMessage(String message) {
            _message = message;
            return this;
        }

        public ErrorBuilder setException(Exception ex) {
            _exception = ex;
            return this;
        }

        public void show(Context context) {
            if (context == null) {
                return;
            }

            Intent intent = new Intent(context, ErrorActivity.class);

            intent.putExtra(INTENT_KEY_TITLE, _title);
            intent.putExtra(INTENT_KEY_MESSAGE, _message);

            if (_exception != null) {
                intent.putExtra(INTENT_KEY_EXCEPTION, _exception);
            }

            context.startActivity(intent);
        }
    }
}
