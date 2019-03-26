package org.tiqr.authenticator.identity;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.tiqr.authenticator.R;
import org.tiqr.authenticator.datamodel.DbAdapter;

import java.io.InputStream;

public class IdentityCursorAdapter extends SimpleCursorAdapter {

    public IdentityCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
        super(context, layout, c, from, to);
    }

    public final void bindView(final View v, final Context context, final Cursor cursor) {
        ImageView logoView = v.findViewById(R.id.identity_provider_logo);

        String logoUrl = cursor.getString(cursor.getColumnIndex(DbAdapter.LOGO));
        if (logoUrl != null && !logoUrl.isEmpty()) {
            Glide.with(logoView)
                    .load(logoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into(logoView);
        }

        int blocked = cursor.getInt(cursor.getColumnIndex(DbAdapter.BLOCKED));
        TextView blockedText = v.findViewById(R.id.blocked);

        if (blocked == 0) {
            blockedText.setVisibility(View.INVISIBLE);
            blockedText.setEnabled(false);
            blockedText.setHeight(0);
        }

        super.bindView(v, context, cursor);
    }
}


