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

import org.tiqr.authenticator.R;
import org.tiqr.authenticator.datamodel.DbAdapter;

import java.io.InputStream;

public class IdentityCursorAdapter extends SimpleCursorAdapter {

    public IdentityCursorAdapter(Context context, int layout, Cursor c,
                                 String[] from, int[] to) {
        super(context, layout, c, from, to);
    }

    public final void bindView(final View v, final Context context, final Cursor cursor) {
        ImageView i = (ImageView)v.findViewById(R.id.identity_provider_logo);

        String logoUrl = cursor.getString(cursor.getColumnIndex(DbAdapter.LOGO));
        new DownloadImageTask(i).execute(logoUrl);

        int blocked = cursor.getInt(cursor.getColumnIndex(DbAdapter.BLOCKED));
        TextView blockedText = (TextView)v.findViewById(R.id.blocked);

        if (blocked == 0) {
            blockedText.setVisibility(View.INVISIBLE);
            blockedText.setEnabled(false);
            blockedText.setHeight(0);
        }

        super.bindView(v, context, cursor);
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap bmp = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                bmp = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return bmp;
        }
        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}


