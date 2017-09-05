package com.transcend.nas.management.browser;

import android.content.Context;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

/**
 * Created by steve_su on 2017/8/28.
 */

public class BrowserSearchView extends SearchView implements SearchView.OnQueryTextListener, MenuItemCompat.OnActionExpandListener {
    private static final String TAG = BrowserSearchView.class.getSimpleName();
    static boolean mIsSearchMode = false;
    protected BrowserActivity mActivity;

    public BrowserSearchView(Context context) {
        super(context);
//        Log.d(TAG, "[Enter] MySearchView(Context context)");
    }

    public void setActivity(BrowserActivity activity) {
        mActivity = activity;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.d(TAG, "[Enter] onQueryTextSubmit query: " + query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
//        Log.d(TAG, "[Enter] onQueryTextChange query: " + newText);
        ((BrowserRecyclerAdapter) mActivity.mRecyclerAdapter).getFilter().filter(newText);
//        mIsSearchMode = true;
        return false;
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            Log.d(TAG, "[Enter] dispatchKeyEventPreIme");
            Log.d(TAG, "isIconified(): "+ isIconified());
            if (!isIconified()) {
                Log.d(TAG, "[Enter] collapseActionView");
                onActionViewCollapsed();
            }
        }

        return super.dispatchKeyEventPreIme(event);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        Log.d(TAG, "[Enter] mIsSearchMode = true");
        mIsSearchMode = true;
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        Log.d(TAG, "[Enter] mIsSearchMode = false");
        mIsSearchMode = false;
        return true;
    }
}
