package org.tiqr.authenticator.identity;

import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

import org.tiqr.authenticator.MainActivity;
import org.tiqr.authenticator.R;
import org.tiqr.authenticator.datamodel.Identity;
import org.tiqr.authenticator.datamodel.IdentityProvider;
import org.tiqr.authenticator.general.FooterView;
import org.tiqr.authenticator.general.HeaderView;
import org.tiqr.authenticator.qr.CaptureActivity;

public class IdentityAdminActivity extends AbstractIdentityListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        registerForContextMenu(getListView());

        HeaderView headerView = (HeaderView)findViewById(R.id.headerView);
        headerView.setOnLeftClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        headerView.setRightIcon(R.drawable.icon_add);
        headerView.setOnRightClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _enrollNewIdentity();
            }
        });

        FooterView footerView = (FooterView)findViewById(R.id.fallbackFooterView);
        footerView.hideInfoIcon();
    }

    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        SQLiteCursor cursor = (SQLiteCursor)getListAdapter().getItem(position);
        Identity i = _db.createIdentityObjectForCurrentCursorPosition(cursor);
        _showIdentityDetail(i);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;

        // Set the popup menu header to the current display name
        Cursor c = getIdentityCursor();
        c.moveToPosition(info.position);
        Identity identity = _db.createIdentityObjectForCurrentCursorPosition(c);
        menu.setHeaderTitle(identity.getDisplayName());

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.identity_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.delete:
                new AlertDialog.Builder(IdentityAdminActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.delete_confirm_title)
                        .setMessage(R.string.delete_confirm)
                        .setPositiveButton(R.string.delete_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                IdentityProvider identityProvider = _db.getIdentityProviderForIdentityId(info.id);

                                _db.deleteIdentityProvider(identityProvider.getId());
                                _db.deleteIdentity(info.id);

                                if (getListAdapter().getCount() > 1) {
                                    getIdentityCursor().requery();
                                } else {
                                    startActivity(new Intent(IdentityAdminActivity.this, MainActivity.class));
                                    finish();
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel_button, null)
                        .show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Show the detail activity for the identity
     *
     * @param id The identity
     */
    protected void _showIdentityDetail(Identity id) {
        Intent intent = new Intent().setClass(this, IdentityDetailActivity.class);
        intent.putExtra("org.tiqr.identity.id", id.getId());
        startActivity(intent);
    }

    /**
     * Start enrolling a new identity.
     */
    private void _enrollNewIdentity() {
        Intent intent = new Intent().setClass(this, CaptureActivity.class);
        startActivity(intent);
    }
}
