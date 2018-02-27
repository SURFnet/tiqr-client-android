package org.tiqr.authenticator.general;

import android.app.Activity;
import android.app.ActivityGroup;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.tiqr.authenticator.auth.Challenge;

import java.util.ArrayList;

/**
 * Activity group base class.
 */
public abstract class AbstractActivityGroup extends ActivityGroup {
    private ArrayList<String> _ids = new ArrayList<String>();

    private boolean _inOnPrepareOptionsMenu = false;
    private boolean _inOnCreateOptionsMenu = false;
    private boolean _inOnOptionsItemSelected = false;

    private Challenge _challenge = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void finishFromChild(Activity child) {
        int size = _ids.size();
        if (size == 0) {
            finish();
            return;
        }

        String id = _ids.get(size - 1);
        getLocalActivityManager().destroyActivity(id, true);
        _ids.remove(id);

        Intent intent = getLocalActivityManager().getActivity(id).getIntent();
        View view = getLocalActivityManager().startActivity(id, intent).getDecorView();
        setContentView(view);
    }

    @Override
    public void onBackPressed() {
        int size = _ids.size();
        if (size == 0) {
            return;
        }

        getLocalActivityManager().getActivity(_ids.get(size - 1)).finish();
        if (size == 1) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (_inOnPrepareOptionsMenu) {
            return true;
        }

        _inOnPrepareOptionsMenu = true;
        menu.clear();
        boolean result = onCreateOptionsMenu(menu);

        Activity activity = getLocalActivityManager().getCurrentActivity();
        if (activity != null) {
            result = result && getLocalActivityManager().getCurrentActivity().onPrepareOptionsMenu(menu);
        }
        _inOnPrepareOptionsMenu = false;
        return result;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (_inOnCreateOptionsMenu) {
            return true;
        }

        _inOnCreateOptionsMenu = true;
        menu.clear();

        boolean result = false;
        Activity activity = getLocalActivityManager().getCurrentActivity();
        if (activity != null) {
            result = getLocalActivityManager().getCurrentActivity().onCreateOptionsMenu(menu);
        }
        _inOnCreateOptionsMenu = false;
        return result;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (_inOnOptionsItemSelected) {
            return true;
        }

        _inOnOptionsItemSelected = true;
        boolean result = false;
        Activity activity = getLocalActivityManager().getCurrentActivity();
        if (activity != null) {
            result = getLocalActivityManager().getCurrentActivity().onOptionsItemSelected(item);
        }
        _inOnOptionsItemSelected = false;
        return result;
    }

    /**
     * Finish the current activity.
     */
    public void goBack() {
        goBack(1);
    }

    /**
     * Go back x activities in the stack. All activities before that
     * will be finished.
     *
     * @param count activity count
     */
    public void goBack(int count) {
        count = Math.min(count, _ids.size());
        for (int i = 0; i < count; i++) {
            getCurrentActivity().finish();
        }
    }

    /**
     * Go back to the root activity.
     */
    public void goToRoot() {
        goBack(_ids.size() - 1);
    }

    public void startChildActivity(String id, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        View view = getLocalActivityManager().startActivity(id, intent).getDecorView();
        _ids.add(id);
        setContentView(view);
    }

    public void setChallenge(Challenge challenge) {
        _challenge = challenge;
    }

    public Challenge getChallenge() {
        return _challenge;
    }

}