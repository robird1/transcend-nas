package com.transcend.nas.management.browser;

import android.app.SearchManager;
import android.content.Context;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.browser_framework.Browser;

import java.lang.reflect.Field;

/**
 * Created by steve_su on 2017/7/20.
 */

public abstract class MediaGeneral {
    private static final String TAG = MediaGeneral.class.getSimpleName();
    protected Context mContext;
    protected BrowserActivity mActivity;
    protected StoreJetCloudData mModel;
    protected RequestAction mRequestControl;
    private MenuItem mMenuSearchItem;
    protected BrowserSearchView mSearchView;

        MediaGeneral(Context context) {
        mContext = context;
        mActivity = (BrowserActivity) context;
    }

    public boolean createOptionsMenu(Menu menu) {
        Log.d(TAG, "[Enter] createOptionsMenu mTabPosition: "+ mActivity.mTabPosition);
        mActivity.getMenuInflater().inflate(R.menu.option_menu_all_file, menu);
        return true;
    }

    public boolean optionsItemSelected(MenuItem item) {
        return false;
    }

    public void onRecyclerItemClick(FileInfo fileInfo) {
        Log.d(TAG, "[Enter] onRecyclerItemClick");
        mRequestControl.onRecyclerItemClick(fileInfo);
    }

    public void onBackPressed() {
        mRequestControl.onBackPressed();
    }

    public void refresh(boolean showProgress) {
        mRequestControl.refresh(showProgress);
    }

    public void onPageChanged() {

    }

//    public void onDropdownItemSelected(int position) {
//
//    }

    public void onCreateActionMode(Menu menu) {
        mActivity.getMenuInflater().inflate(R.menu.twonky_editor, menu);
    }

    public void lazyLoad() {

    }


    protected void doSelect() {
        mActivity.startEditorMode();
    }

    protected void doSelectAll() {
        mActivity.startEditorMode();
        mActivity.toggleSelectAll();
    }

    protected Browser.LayoutType onViewAllLayout() {
        return Browser.LayoutType.GRID;
    }

    public void onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "[Enter] onPrepareOptionsMenu mTabPosition: "+ mActivity.mTabPosition);
//        menu.clear();
//        MenuInflater inflater = mActivity.getMenuInflater();
//        inflater.inflate(R.menu.file_manage_viewer, menu);

        mActivity.mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);


        mMenuSearchItem = menu.findItem(R.id.my_search);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) mActivity.getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (BrowserSearchView) mMenuSearchItem.getActionView();
        mSearchView.setActivity(mActivity);

        // Assumes current activity is the searchable activity
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(mActivity.getComponentName()));

        // 這邊讓icon可以還原到搜尋的icon
        mSearchView.setIconifiedByDefault(true);

        mSearchView.setOnQueryTextListener(mSearchView);
        MenuItemCompat.setOnActionExpandListener(mMenuSearchItem, mSearchView);

//        mSearchView.setBackgroundTintMode(PorterDuff.Mode.LIGHTEN);
        AutoCompleteTextView searchTextView = (AutoCompleteTextView) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        try {
            Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            mCursorDrawableRes.setAccessible(true);

            //This sets the cursor resource ID to 0 or @null which will make it visible on white background
            mCursorDrawableRes.set(searchTextView, R.drawable.color_cursor);
        } catch (Exception e) {

        }

    }


}
