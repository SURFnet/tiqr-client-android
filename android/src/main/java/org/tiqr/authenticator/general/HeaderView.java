package org.tiqr.authenticator.general;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.tiqr.authenticator.R;
import org.tiqr.authenticator.identity.IdentityAdminActivity;

/**
 * Created by andrei on 14/07/15.
 */
public class HeaderView extends FrameLayout {
    protected ImageView leftButton;
    protected ImageView rightButton;

    public HeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater layoutInflater = (LayoutInflater)context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.tiqr_titlebar, this);

        leftButton = (ImageView)findViewById(R.id.left_button);
        rightButton = (ImageView)findViewById(R.id.right_button);

        setOnRightClickListener(new OnClickListener() {
            public void onClick(View v) {
                doIdentityAdmin();
            }
        });
    }

    public void setOnLeftClickListener(OnClickListener listener) {
        leftButton.setOnClickListener(listener);
    }

    public void setRightIcon(int resourceId) {
        rightButton.setImageResource(resourceId);
    }

    public void setOnRightClickListener(OnClickListener listener) {
        rightButton.setOnClickListener(listener);
    }

    public void hideRightButton() {
        rightButton.setVisibility(View.GONE);
    }

    public void hideLeftButton() {
        leftButton.setVisibility(View.GONE);
    }

    public void doIdentityAdmin() {
        Intent identityIntent = new Intent(getContext(), IdentityAdminActivity.class);
        getContext().startActivity(identityIntent);
    }
}
