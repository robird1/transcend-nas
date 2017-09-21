package com.transcend.nas.management.browser;

import android.content.Context;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.KeyEvent;
import android.view.MenuItem;

/**
 * Created by steve_su on 2017/8/28.
 */

public class BrowserSearchView extends SearchView implements SearchView.OnQueryTextListener,
        MenuItemCompat.OnActionExpandListener {

    static boolean mIsSearchMode = false;
    private BrowserActivity mActivity;

    public BrowserSearchView(Context context) {
        super(context);
    }

    public void setActivity(BrowserActivity activity) {
        mActivity = activity;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mActivity.mMediaControl.search(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (!isIconified()) {
                onActionViewCollapsed();
            }
        }

        return super.dispatchKeyEventPreIme(event);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        mIsSearchMode = true;
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        mIsSearchMode = false;
        mActivity.closeSearchResult();
        return true;
    }
}
