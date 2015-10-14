package org.tiqr.authenticator.general;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.tiqr.authenticator.MainActivity;
import org.tiqr.authenticator.R;

public class ErrorActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.error);

        String title = getIntent().getStringExtra("org.tiqr.error.title");
        String message = getIntent().getStringExtra("org.tiqr.error.message");

        HeaderView headerView = (HeaderView)findViewById(R.id.headerView);
        headerView.setOnLeftClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        headerView.hideRightButton();

        FooterView footer = (FooterView)findViewById(R.id.footerView);
        footer.hideInfoIcon();

        TextView titleField = (TextView)findViewById(R.id.error_title);
        titleField.setText(title);

        TextView messageField = (TextView)findViewById(R.id.error_message);
        messageField.setText(message);
    }

    public static class ErrorBuilder {
        private String _title;
        private String _message;

        public ErrorBuilder setTitle(String title) {
            _title = title;
            return this;
        }

        public ErrorBuilder setMessage(String message) {
            _message = message;
            return this;
        }

        public void show(Context context) {
            if (context == null) {
                return;
            }

            Intent intent = new Intent(context, ErrorActivity.class);

            intent.putExtra("org.tiqr.error.title", _title);
            intent.putExtra("org.tiqr.error.message", _message);

            context.startActivity(intent);
        }
    }
}
